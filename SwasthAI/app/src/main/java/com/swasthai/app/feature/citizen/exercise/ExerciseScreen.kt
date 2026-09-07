package com.swasthai.app.feature.citizen.exercise

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swasthai.app.R
import kotlinx.coroutines.delay

data class SimpleExercise(
    val id: String, val name: String, val imageRes: Int, val durationMinutes: Int,
    val instructions: List<String>, val ageGroups: List<String>
)

val allSimpleExercises = listOf(
    SimpleExercise("neck", "Neck", R.drawable.img_ex_stretch, 2, listOf("Move slowly", "Do not force"), listOf("18-35", "36-55", "56+")),
    SimpleExercise("shoulder", "Shoulder", R.drawable.img_ex_stretch, 2, listOf("Move slowly", "Do not force"), listOf("18-35", "36-55", "56+")),
    SimpleExercise("back", "Back", R.drawable.img_ex_stretch, 3, listOf("Move slowly", "Do not force"), listOf("18-35", "36-55", "56+")),
    SimpleExercise("legs", "Legs", R.drawable.img_ex_chair, 3, listOf("Move slowly", "Do not force"), listOf("18-35", "36-55", "56+")),
    SimpleExercise("walk", "Walk", R.drawable.img_ex_walk, 5, listOf("Walk comfortably", "Breathe normally"), listOf("18-35", "36-55")),
    SimpleExercise("slow_walk", "Slow Walk", R.drawable.img_ex_walk, 5, listOf("Walk slowly", "Use support if needed"), listOf("56+")),
    SimpleExercise("sit_stand", "Sit & Stand", R.drawable.img_ex_chair, 3, listOf("Use a sturdy chair", "Move slowly"), listOf("36-55", "56+")),
    SimpleExercise("balance", "Balance", R.drawable.img_ex_balance, 3, listOf("Hold wall for support", "Do not force"), listOf("56+")),
    SimpleExercise("stretch", "Stretch", R.drawable.img_ex_stretch, 3, listOf("Move slowly", "Do not force"), listOf("18-35", "36-55")),
    SimpleExercise("gentle_stretch", "Gentle Stretch", R.drawable.img_ex_stretch, 3, listOf("Move slowly", "Do not force"), listOf("56+")),
    SimpleExercise("yoga", "Yoga", R.drawable.img_ex_yoga, 5, listOf("Sit comfortably", "Breathe slowly"), listOf("18-35")),
    SimpleExercise("easy_exercise", "Easy Exercise", R.drawable.img_ex_chair, 3, listOf("Move slowly", "Do not force"), listOf("18-35"))
)

fun getExercisesForAge(age: Int): List<SimpleExercise> {
    val group = when { age <= 35 -> "18-35"; age <= 55 -> "36-55"; else -> "56+" }
    return allSimpleExercises.filter { it.ageGroups.contains(group) }
}

fun getTodayRoutine(age: Int): List<SimpleExercise> {
    return when {
        age <= 35 -> listOf(allSimpleExercises.first { it.id == "walk" }, allSimpleExercises.first { it.id == "stretch" }, allSimpleExercises.first { it.id == "yoga" })
        age <= 55 -> listOf(allSimpleExercises.first { it.id == "walk" }, allSimpleExercises.first { it.id == "back" }, allSimpleExercises.first { it.id == "legs" })
        else -> listOf(allSimpleExercises.first { it.id == "slow_walk" }, allSimpleExercises.first { it.id == "sit_stand" }, allSimpleExercises.first { it.id == "balance" })
    }
}

val simpleCategories = listOf(Pair("Walk", R.drawable.img_ex_walk), Pair("Stretch", R.drawable.img_ex_stretch), Pair("Yoga", R.drawable.img_ex_yoga), Pair("Easy Exercise", R.drawable.img_ex_chair))
val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    var selectedExercise by remember { mutableStateOf<SimpleExercise?>(null) }
    var activeExercise by remember { mutableStateOf<SimpleExercise?>(null) }
    var showDayDetails by remember { mutableStateOf<DailyExerciseSummary?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    val weeklyState by viewModel.weeklyState.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsState()
    val mockAge = 45

    if (activeExercise != null) { 
        SimpleTimerScreen(
            exercise = activeExercise!!, 
            onDone = { 
                viewModel.recordExerciseSession(activeExercise!!)
                activeExercise = null 
            }
        )
        return 
    }
    
    if (selectedExercise != null) { 
        SimpleDetailScreen(selectedExercise!!, { selectedExercise = null }) { 
            activeExercise = selectedExercise
            selectedExercise = null 
        }
        return 
    }

    if (showGoalDialog) {
        GoalSetDialog(
            currentGoal = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                viewModel.setDailyGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

    if (showDayDetails != null) {
        AlertDialog(
            onDismissRequest = { showDayDetails = null },
            title = {
                Text(
                    text = weekDays[showDayDetails!!.date.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("${showDayDetails!!.totalMinutes} min total", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (showDayDetails!!.sessions.isEmpty()) {
                        Text("No exercise on this day.")
                    } else {
                        showDayDetails!!.sessions.forEach { session ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(session.exerciseName, style = MaterialTheme.typography.bodyLarge)
                                Text("${session.durationMinutes} min", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDayDetails = null }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Column { Text("Exercise", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Move a little every day", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // ── Today's Goal Card ──────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Today's Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("$todayMinutes / $dailyGoal min", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { showGoalDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Set Goal", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val progress = (todayMinutes.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
                        val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "goal_progress")
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)),
                            color = if (todayMinutes >= dailyGoal) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (todayMinutes >= dailyGoal) "🎉 Goal achieved!" else "${(dailyGoal - todayMinutes)} min remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (todayMinutes >= dailyGoal) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            // ── This Week Streak Card ──────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("This Week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${weeklyState.activeDays} days active", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.weight(1f))
                            Text("${weeklyState.totalMinutes} min this week", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            if (!weeklyState.isLoading) {
                                val maxMin = weeklyState.maxMinutesInDay.coerceAtLeast(1)
                                weeklyState.dailySummaries.forEachIndexed { idx, summary ->
                                    val dayName = weekDays[summary.date.dayOfWeek.value - 1]
                                    val heightFraction = summary.totalMinutes.toFloat() / maxMin
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { showDayDetails = summary }) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .fillMaxHeight(fraction = heightFraction.coerceAtLeast(0.05f)) // minimum height
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(if (summary.totalMinutes > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.1f))
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(dayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(12.dp)) { Text("Exercise slowly. Stop if you feel pain or dizziness.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE65100), fontWeight = FontWeight.Medium) } }
            item { Text("Today's Exercise", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)) }
            items(getTodayRoutine(mockAge)) { ex -> TodayCard(ex) { activeExercise = ex } }
            item { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)); Spacer(Modifier.height(8.dp)); LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(simpleCategories) { cat -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Image(painter = painterResource(id = cat.second), contentDescription = cat.first, modifier = Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit) }; Spacer(Modifier.height(4.dp)); Text(cat.first, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) } } } }
            item { Text("Simple Exercises", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp)) }
            items(getExercisesForAge(mockAge)) { ex -> SimpleLibraryCard(ex) { selectedExercise = ex } }
        }
    }
}

@Composable
fun TodayCard(exercise: SimpleExercise, onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = exercise.imageRes), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(" min", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Button(onClick = onStart, shape = RoundedCornerShape(12.dp)) { Text("Start", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun SimpleLibraryCard(exercise: SimpleExercise, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = exercise.imageRes), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(" min", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDetailScreen(exercise: SimpleExercise, onBack: () -> Unit, onStart: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = {}, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = exercise.imageRes), contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color.White), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(32.dp))
            Text(exercise.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(" min", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            exercise.instructions.forEach { Text(it, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp)) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp)); Spacer(Modifier.width(12.dp)); Text("Start", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimerScreen(exercise: SimpleExercise, onDone: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(exercise.durationMinutes * 60) }
    var isRunning by remember { mutableStateOf(true) }
    LaunchedEffect(isRunning, secondsLeft) { if (isRunning && secondsLeft > 0) { delay(1000); secondsLeft-- } }
    val isDone = secondsLeft == 0
    Scaffold(topBar = { TopAppBar(title = { Text(exercise.name, style = MaterialTheme.typography.headlineSmall) }, navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Stop") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = exercise.imageRes), contentDescription = null, modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(24.dp)).background(Color.White), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(48.dp))
            if (isDone) Text("Done!", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) else Text("%02d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (isDone) Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp)) { Text("Finish", fontSize = 24.sp, fontWeight = FontWeight.Bold) } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { isRunning = !isRunning }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(16.dp)) { Text(if (isRunning) "Pause" else "Resume", fontSize = 20.sp, fontWeight = FontWeight.Bold) }; Button(onClick = onDone, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Stop", fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
fun GoalSetDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var input by remember { mutableStateOf(currentGoal.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Exercise Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your daily exercise goal in minutes:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) input = it },
                    label = { Text("Minutes") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Quick preset chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 45, 60).forEach { preset ->
                        FilterChip(
                            selected = input == preset.toString(),
                            onClick = { input = preset.toString() },
                            label = { Text("${preset}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { input.toIntOrNull()?.let { if (it > 0) onSave(it) } },
                enabled = input.toIntOrNull()?.let { it > 0 } == true
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

