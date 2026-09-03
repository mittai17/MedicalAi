package com.swasthai.app.feature.citizen.aichat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.swasthai.app.core.components.SwasthAITopBar
import com.swasthai.app.core.theme.SwasthAIColors
import com.swasthai.app.feature.citizen.screening.VoiceRecognizer
import java.io.File
import java.util.Locale

/**
 * AI Doctor chat console — an on-device medical assistant.
 *
 * Live (streaming) conversation over the local RAG engine, voice input,
 * image attach with real TFLite vision analysis, AI skill chips, and a
 * guided symptom questionnaire that runs the same evidence-based diagnosis
 * as the screening wizard and persists a real screening record.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    onNavigateToReport: (String) -> Unit,
    viewModel: AIChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingPermissionStart by remember { mutableStateOf(false) }
    val recognizerAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    // Voice recognizer wiring.
    val recognizer = remember {
        VoiceRecognizer(
            context = context.applicationContext,
            callbacks = object : VoiceRecognizer.Callbacks {
                override fun onPartialResult(text: String) {
                    viewModel.onVoiceText(text)
                }

                override fun onFinalResult(text: String) {
                    if (text.isNotBlank()) viewModel.onVoiceText(text)
                    viewModel.setRecording(false)
                    isListening = false
                }

                override fun onError(message: String) {
                    viewModel.setRecording(false)
                    isListening = false
                    voiceError = message
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted && pendingPermissionStart) {
            pendingPermissionStart = false
            isListening = true
            viewModel.setRecording(true)
            recognizer.start(Locale.US)
        } else if (!granted) {
            pendingPermissionStart = false
            voiceError = "Microphone permission is needed for voice input. You can type instead."
        }
    }

    fun startRecording() {
        voiceError = null
        if (!recognizerAvailable || !recognizer.isAvailable) {
            voiceError = "Voice recognition isn't available on this device — use the keyboard."
            return
        }
        if (!permissionGranted) {
            pendingPermissionStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        isListening = true
        viewModel.setRecording(true)
        recognizer.start(Locale.US)
    }

    fun stopRecording() {
        recognizer.stop()
        isListening = false
        viewModel.setRecording(false)
    }

    // Image attach: camera + gallery (image path = content:// URI, classifier handles it).
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var launchCamera: () -> Unit = {}
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { viewModel.attachImage(it.toString()) }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachImage(it.toString()) }
    }
    launchCamera = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            val file = File(context.cacheDir, "ai_chat_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Navigate to the shareable report once a chat diagnosis is ready.
    LaunchedEffect(uiState.pendingReportScreeningId) {
        uiState.pendingReportScreeningId?.let { screeningId ->
            viewModel.clearPendingReport()
            onNavigateToReport(screeningId)
        }
    }

    // Auto-scroll to the newest message.
    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            SwasthAITopBar(
                title = "AI Doctor",
                showBackButton = false,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Privacy / capability banner.
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "On-device · offline · answers from the local health library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (uiState.isWorking) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Messages.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    ChatBubble(message = message)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Options: result actions (from the last message) or the active
            // questionnaire step's chips (persisted in state, so they stay
            // visible while streamed confirmations arrive).
            val lastMessage = uiState.messages.lastOrNull()
            val isLastResult = lastMessage != null && lastMessage.result != null
            val options = if (isLastResult) lastMessage.options else uiState.activeOptions
            val optionsMode = if (isLastResult) lastMessage.optionsMode else uiState.activeOptionsMode
            if (options.isNotEmpty()) {
                MessageOptions(
                    options = options,
                    mode = optionsMode,
                    isResult = isLastResult,
                    onSymptomToggle = viewModel::toggleSymptom,
                    onDurationSelect = viewModel::selectDuration,
                    onVitalsSourceSelect = viewModel::selectVitalsSource,
                    onScanTypeSelect = viewModel::chooseScanType,
                    onResultOption = { option ->
                        viewModel.onResultOption(option, lastMessage?.screeningId)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Manual vitals entry card.
            if (uiState.showVitalsInput) {
                VitalsInputCard(
                    temperature = uiState.vitalsTemperature,
                    pulse = uiState.vitalsPulse,
                    spo2 = uiState.vitalsSpo2,
                    onTemperatureChange = { vm -> viewModel.updateVitals(vm, uiState.vitalsPulse, uiState.vitalsSpo2) },
                    onPulseChange = { v -> viewModel.updateVitals(uiState.vitalsTemperature, v, uiState.vitalsSpo2) },
                    onSpo2Change = { v -> viewModel.updateVitals(uiState.vitalsTemperature, uiState.vitalsPulse, v) },
                    onDone = viewModel::confirmVitals
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Skill chips.
            SkillChipsRow(onSkillTap = viewModel::onSkillTap)

            Spacer(modifier = Modifier.height(6.dp))

            // Composer.
            ChatComposer(
                inputText = uiState.inputText,
                isListening = isListening,
                onInputChange = viewModel::updateInput,
                onAttach = {
                    // Camera then gallery.
                    launchCamera()
                },
                onGallery = { galleryLauncher.launch("image/*") },
                onSend = { viewModel.sendText() },
                onVoiceToggle = {
                    if (isListening) stopRecording() else startRecording()
                }
            )
        }
    }
}

/**
 * Renders a single chat bubble (user right / AI left) with optional image.
 */
@Composable
private fun ChatBubble(
    message: ChatMessage
) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false).widthIn(max = 320.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    message.imagePath?.let { path ->
                        AsyncImage(
                            model = path,
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (message.streaming) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▍",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Chip-driven answer options for the questionnaire steps, scan types, or
 * result actions.
 */
@Composable
private fun MessageOptions(
    options: List<String>,
    mode: OptionsMode,
    isResult: Boolean,
    onSymptomToggle: (String) -> Unit,
    onDurationSelect: (String) -> Unit,
    onVitalsSourceSelect: (String) -> Unit,
    onScanTypeSelect: (String) -> Unit,
    onResultOption: (String) -> Unit
) {
    if (isResult || mode == OptionsMode.VITALS_SOURCE) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                Button(
                    onClick = {
                        if (isResult) onResultOption(option)
                        else onVitalsSourceSelect(option)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            when (mode) {
                OptionsMode.SINGLE_DURATION -> FilledTonalButton(onClick = { onDurationSelect(option) }) {
                    Text(option)
                }
                OptionsMode.SCAN_TYPE -> FilledTonalButton(onClick = { onScanTypeSelect(option) }) {
                    Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                else -> FilterChip(
                    selected = false,
                    onClick = { onSymptomToggle(option) },
                    label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

/**
 * Horizontal scrollable row of AI skill chips.
 */
@Composable
private fun SkillChipsRow(onSkillTap: (AiSkill) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AiSkill.entries.forEach { skill ->
            SuggestionChip(
                onClick = { onSkillTap(skill) },
                label = { Text(skill.chipLabel) }
            )
        }
    }
}

/**
 * Manual vitals entry (Step 3 fallback).
 */
@Composable
private fun VitalsInputCard(
    temperature: String,
    pulse: String,
    spo2: String,
    onTemperatureChange: (String) -> Unit,
    onPulseChange: (String) -> Unit,
    onSpo2Change: (String) -> Unit,
    onDone: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    label = { Text("Temp (°C)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pulse,
                    onValueChange = onPulseChange,
                    label = { Text("Pulse (bpm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = spo2,
                    onValueChange = onSpo2Change,
                    label = { Text("SpO2 (%)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done — analyse my answers")
            }
        }
    }
}

/**
 * Bottom composer: attach / gallery, text input, mic, send.
 */
@Composable
private fun ChatComposer(
    inputText: String,
    isListening: Boolean,
    onInputChange: (String) -> Unit,
    onAttach: () -> Unit,
    onGallery: () -> Unit,
    onSend: () -> Unit,
    onVoiceToggle: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onGallery,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Attach image",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask me anything about your health…") },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )
                if (inputText.isBlank()) {
                    IconButton(
                        onClick = onVoiceToggle,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(
                            if (isListening) SwasthAIColors.RiskHigh else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isListening) "Stop voice input" else "Voice input",
                            tint = if (isListening) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(
                            MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}