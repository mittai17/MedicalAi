package com.swasthai.app.feature.citizen.voice

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swasthai.app.ai.engine.ScanType
import com.swasthai.app.core.components.SwasthAIPrimaryButton
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.voice.TtsSpeaker
import com.swasthai.app.core.voice.VOICE_LANGUAGES
import com.swasthai.app.core.voice.speechLocaleFor
import com.swasthai.app.core.voice.voiceLanguageLabel
import com.swasthai.app.feature.citizen.screening.ScreeningStep
import com.swasthai.app.feature.citizen.screening.ScreeningViewModel
import com.swasthai.app.feature.citizen.screening.VoiceRecognizer
import com.swasthai.app.domain.model.ScreeningType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

/**
 * "Talk to SwasthAI" — hands-free voice command hub.
 *
 * Tap the big mic (or type a command) and the AI both understands and then
 * OPERATES the app: runs a symptom check, opens the camera and analyses a
 * photo, sets (recurring) reminders, books consultations, and navigates to
 * health services / records. Every reply is spoken back and shown as text,
 * in the selected language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandHubScreen(
    onBack: () -> Unit,
    onNavigateToScreeningResult: () -> Unit,
    onNavigateToConnect: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    viewModel: VoiceCommandHubViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "Talk to SwasthAI",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        VoiceCommandHubContent(
            onNavigateToScreeningResult = onNavigateToScreeningResult,
            onNavigateToConnect = onNavigateToConnect,
            onNavigateToRecords = onNavigateToRecords,
            viewModel = viewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Scaffold-less "Talk to SwasthAI" control centre, shared by the pushed
 * [VoiceCommandHubScreen] and the Voice mode of the unified Health Check hub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandHubContent(
    onNavigateToScreeningResult: () -> Unit,
    onNavigateToConnect: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
    viewModel: VoiceCommandHubViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screeningViewModel: ScreeningViewModel = hiltViewModel(
        LocalContext.current as ViewModelStoreOwner
    )
    val screeningState by screeningViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val speaker = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TtsSpeakerEntryPoint::class.java
        ).ttsSpeaker()
    }

    // ── UI-local state ──
    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingPermissionStart by remember { mutableStateOf(false) }
    val recognizerAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var commandText by rememberSaveable { mutableStateOf("") }

    // Camera state
    var pendingScanType by remember { mutableStateOf<ScanType?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var expectingResult by remember { mutableStateOf(false) }

    // ── Reactive side effects ──

    // Speak each new reply produced by the hub VM.
    LaunchedEffect(uiState.speakId) {
        if (uiState.speakId > 0 && uiState.reply.isNotBlank()) {
            speaker.speak(uiState.reply, uiState.language)
        }
    }

    // Navigate when the hub asks.
    LaunchedEffect(uiState.navTarget) {
        val target = uiState.navTarget ?: return@LaunchedEffect
        when (target) {
            VoiceNavTarget.CONNECT_PROVIDERS -> onNavigateToConnect()
            VoiceNavTarget.HEALTH_RECORDS -> onNavigateToRecords()
        }
        viewModel.clearNavTarget()
    }

    // When a hub-initiated check finishes, speak the result + jump to it.
    LaunchedEffect(screeningState.currentStep) {
        if (expectingResult && screeningState.currentStep == ScreeningStep.RESULT) {
            expectingResult = false
            val r = screeningState.diagnosisResult
            if (r != null) {
                val summary = buildString {
                    append("Your result is ready. It shows ${r.predictedDisease}, with ")
                    append("confidence ${r.confidenceScore.toInt()} percent. Risk level is ")
                    append(r.riskLevel.name.replaceFirstChar { it.uppercase() }.lowercase())
                    append(". ")
                    r.recommendations.firstOrNull()?.let { append(it.text) }
                }
                speaker.speak(summary, uiState.language)
            }
            onNavigateToScreeningResult()
        }
    }

    // ── Speech recognizer (Android SpeechRecognizer) ──

    val recognizer = remember {
        VoiceRecognizer(
            context = context.applicationContext,
            callbacks = object : VoiceRecognizer.Callbacks {
                override fun onPartialResult(text: String) = Unit

                override fun onFinalResult(text: String) {
                    isListening = false
                    if (text.isNotBlank()) viewModel.handleUtterance(text)
                }

                override fun onError(message: String) {
                    isListening = false
                    voiceError = message
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.destroy()
            speaker.stop()
        }
    }

    fun startRecognizer() {
        voiceError = null
        if (!recognizerAvailable || !recognizer.isAvailable) {
            voiceError = "Voice recognition isn't available here — use the text box below."
            return
        }
        isListening = true
        recognizer.start(speechLocaleFor(uiState.language))
    }

    fun stopRecognizer() {
        recognizer.stop()
        isListening = false
    }

    // ── Camera (same contract as Image Check) ──

    fun runSymptomCheck(symptoms: String) {
        viewModel.resetPending()
        screeningViewModel.resetScreening()
        screeningViewModel.setScreeningType(ScreeningType.SYMPTOM_CHECK)
        screeningViewModel.setCapturedImagePath(null)
        screeningViewModel.updateVoiceTranscript(symptoms)
        expectingResult = true
        screeningViewModel.skipVitalsAndDiagnose()
    }

    fun startImageDiagnosis(uri: Uri) {
        val scanType = pendingScanType ?: ScanType.PNEUMONIA
        pendingScanType = null
        speaker.speak(
            if (uiState.language == "hi") {
                "Photo aa gayi. Main iska analysis kar rahi hoon."
            } else {
                "Photo taken. I'm analysing it now."
            },
            uiState.language
        )
        viewModel.resetPending()
        screeningViewModel.resetScreening()
        screeningViewModel.setScreeningType(ScreeningType.IMAGE_CHECK)
        screeningViewModel.updateVoiceTranscript("")
        screeningViewModel.setCapturedImagePath(uri.toString())
        screeningViewModel.setScanType(scanType)
        expectingResult = true
        screeningViewModel.skipVitalsAndDiagnose()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri -> startImageDiagnosis(uri) }
        }
    }

    fun launchCameraIntent(scanType: ScanType) {
        val file = File(context.cacheDir, "voice_scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        photoUri = uri
        try {
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            voiceError = "Camera couldn't open. Please try the Image Check option instead."
            photoUri = null
            pendingScanType = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val scanType = pendingScanType ?: ScanType.PNEUMONIA
        if (granted) launchCameraIntent(scanType) else voiceError =
            "Camera permission is needed to take a photo. You can type another command instead."
        pendingScanType = null
    }

    fun openCamera(scanType: ScanType) {
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCamera) {
            launchCameraIntent(scanType)
        } else {
            pendingScanType = scanType
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted && pendingPermissionStart) {
            pendingPermissionStart = false
            startRecognizer()
        } else if (!granted) {
            pendingPermissionStart = false
            voiceError = "Microphone permission is needed for voice input. You can type below or tap an example instead."
        }
    }

    // Execute app-operation actions handed over by the hub.
    LaunchedEffect(uiState.action) {
        val action = uiState.action ?: return@LaunchedEffect
        when (action) {
            VoiceAction.RUN_SYMPTOM_CHECK -> runSymptomCheck(uiState.actionPayload)
            VoiceAction.OPEN_CAMERA -> {
                val scanType = ScanType.entries
                    .firstOrNull { it.name == uiState.actionPayload }
                    ?: ScanType.PNEUMONIA
                pendingScanType = scanType
                openCamera(scanType)
            }
        }
        viewModel.consumeAction()
    }

    // ── UI ──

    Column(
        modifier = modifier.fillMaxSize()
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (uiState.pending != null) "Please answer me 👂" else if (recognizerAvailable) "Tap the mic and tell me what to do" else "Type a command below",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LanguageSelector(selected = uiState.language) { viewModel.setLanguage(it) }

            Spacer(modifier = Modifier.height(32.dp))

            // Big mic button
            Box {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    )
                }
                FloatingActionButton(
                    onClick = { if (isListening) stopRecognizer() else startRecognizer() },
                    modifier = Modifier.size(96.dp),
                    containerColor = if (isListening) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Start listening",
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isListening -> "Listening… tap again to stop"
                    uiState.pending != null -> "Speak your answer, or type/tap an example below"
                    else -> "Or use the examples below"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isListening) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            voiceError?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Latest assistant reply
            if (uiState.reply.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "SwasthAI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.reply,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (uiState.lastTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You said: \"${uiState.lastTranscript}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Example commands (big, tappable)
            ExampleCommands(
                pending = uiState.pending != null,
                onCommand = {
                    commandText = it
                    viewModel.handleUtterance(it)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "English and Hindi commands are supported. Tap an example to try it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Keyboard fallback (pinned above the IME) ──
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        label = { Text("Or type a command") },
                        placeholder = { Text("e.g. set a reminder to take my medicine at 9") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commandText.isNotBlank()) {
                                    viewModel.handleUtterance(commandText)
                                    commandText = ""
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (commandText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SwasthAIPrimaryButton(
                            text = "Send command",
                            onClick = {
                                viewModel.handleUtterance(commandText)
                                commandText = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = Icons.Filled.Send
                        )
                    }
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { menuOpen = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = voiceLanguageLabel(selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Change language",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            VOICE_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (lang.code == selected) {
                                "✓ ${lang.nativeName} · ${lang.englishName}"
                            } else {
                                "${lang.nativeName} · ${lang.englishName}"
                            }
                        )
                    },
                    onClick = {
                        onSelect(lang.code)
                        menuOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExampleCommands(
    pending: Boolean,
    onCommand: (String) -> Unit
) {
    val examples = if (pending) {
        listOf(
            "I have fever and cough",
            "at 9 in the morning",
            "every day",
            "fever for three days"
        )
    } else {
        listOf(
            "Check my health",
            "Set a reminder to take my medicine at 9 in the morning",
            "Book a consultation",
            "Take a photo of my chest x-ray",
            "Show my reminders",
            "Find a hospital"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        examples.forEach { example ->
            OutlinedButton(
                onClick = { onCommand(example) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = example,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TtsSpeakerEntryPoint {
    fun ttsSpeaker(): TtsSpeaker
}