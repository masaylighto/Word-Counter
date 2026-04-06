package com.anonymous.wordcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anonymous.wordcounter.data.WordEntity
import com.anonymous.wordcounter.ui.MainViewModel
import com.anonymous.wordcounter.ui.SortMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val BlossomColorScheme = lightColorScheme(
    primary = Color(0xFFC93D7B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E7),
    onPrimaryContainer = Color(0xFF4D1730),
    secondary = Color(0xFF9F4E72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDEA),
    onSecondaryContainer = Color(0xFF3F1528),
    tertiary = Color(0xFFB4698B),
    onTertiary = Color.White,
    background = Color(0xFFFFF5FA),
    onBackground = Color(0xFF3D1B2D),
    surface = Color(0xFFFFFBFD),
    onSurface = Color(0xFF3D1B2D),
    surfaceVariant = Color(0xFFFBE8F1),
    onSurfaceVariant = Color(0xFF6A3A50),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val BlossomTypography = Typography().copy(
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
    ),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyMedium = Typography().bodyMedium.copy(lineHeight = 21.sp),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BlossomTheme {
                WordCounterApp()
            }
        }
    }
}

@Composable
private fun BlossomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlossomColorScheme,
        typography = BlossomTypography,
        content = content,
    )
}

@Composable
private fun WordCounterApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val words by viewModel.words.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        FloralBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PageSwitcher(
                    page = page,
                    onPageChange = { page = it },
                )

                if (page == 0) {
                    WordsPage(
                        words = words,
                        wordInput = uiState.wordInput,
                        meaningInput = uiState.meaningInput,
                        needsDefinitionStep = uiState.needsDefinitionStep,
                        sortMethod = uiState.sortMethod,
                        sortAscending = uiState.sortAscending,
                        isBusy = uiState.isBusy,
                        errorText = uiState.errorText,
                        onWordChange = viewModel::onWordInputChange,
                        onMeaningChange = viewModel::onMeaningInputChange,
                        onSortMethodChange = viewModel::onSortMethodChange,
                        onSortDirectionChange = viewModel::onSortDirectionChange,
                        onAdd = viewModel::addWord,
                        onGenerate = viewModel::generateDefinition,
                        onDelete = viewModel::deleteWord,
                        onSaveEdit = { id, word, meaning, occ, onDone ->
                            viewModel.saveEdit(id, word, meaning, occ, onDone)
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    SettingsPage(
                        apiKeyInput = uiState.apiKeyInput,
                        maskedApiKey = viewModel.maskedApiKey(),
                        isBusy = uiState.isBusy,
                        errorText = uiState.errorText,
                        onApiKeyChange = viewModel::onApiKeyInputChange,
                        onSave = viewModel::saveApiKey,
                        onRemove = viewModel::removeApiKey,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollHeaderTitle() {
    Text(
        text = "Bloom Lexicon",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun PageSwitcher(page: Int, onPageChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PageButton(
                label = "Words",
                selected = page == 0,
                onClick = { onPageChange(0) },
                modifier = Modifier.weight(1f),
            )
            PageButton(
                label = "Settings",
                selected = page == 1,
                onClick = { onPageChange(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PageButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun WordsPage(
    words: List<WordEntity>,
    wordInput: String,
    meaningInput: String,
    needsDefinitionStep: Boolean,
    sortMethod: SortMethod,
    sortAscending: Boolean,
    isBusy: Boolean,
    errorText: String,
    onWordChange: (String) -> Unit,
    onMeaningChange: (String) -> Unit,
    onSortMethodChange: (SortMethod) -> Unit,
    onSortDirectionChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onGenerate: () -> Unit,
    onDelete: (Long) -> Unit,
    onSaveEdit: (Long, String, String, String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editTarget by remember { mutableStateOf<WordEntity?>(null) }
    var expandedRowId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sortOptions = listOf(SortMethod.OCCURRENCE, SortMethod.ALPHABETICAL, SortMethod.ADDED_DATE)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 6.dp),
    ) {
        item {
            ScrollHeaderTitle()
        }

        item {
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (needsDefinitionStep) "Phase 2: Add Meaning" else "Phase 1: Add Word",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (needsDefinitionStep) {
                            "This word is new. Add meaning or use GPT definition, then save."
                        } else {
                            "Enter a word first. Existing words increase count automatically."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = wordInput,
                        onValueChange = onWordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("German word") },
                        singleLine = true,
                        readOnly = needsDefinitionStep,
                        shape = RoundedCornerShape(16.dp),
                    )

                    if (needsDefinitionStep) {
                        OutlinedTextField(
                            value = meaningInput,
                            onValueChange = onMeaningChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Meaning") },
                            minLines = 3,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onGenerate,
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(if (isBusy) "Working..." else "GPT Definition")
                            }
                            Button(
                                onClick = onAdd,
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(if (isBusy) "Working..." else "Save Word")
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAdd,
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(if (isBusy) "Working..." else "Check / Add")
                            }
                        }
                        Text(
                            text = "If the word already exists, occurrence will increase automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (errorText.isNotBlank()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        item {
            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Filters", style = MaterialTheme.typography.titleSmall)
                    DropdownSelector(
                        label = "Sort type",
                        selectedText = sortMethodLabel(sortMethod),
                        options = sortOptions.map(::sortMethodLabel),
                        enabled = !isBusy,
                        onOptionSelected = { index -> onSortMethodChange(sortOptions[index]) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownSelector(
                        label = "Order",
                        selectedText = if (sortAscending) "Ascending" else "Descending",
                        options = listOf("Descending", "Ascending"),
                        enabled = !isBusy,
                        onOptionSelected = { index -> onSortDirectionChange(index == 1) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (words.isEmpty()) {
            item {
                SoftCard {
                    Text(
                        text = "No words yet. Start your first bloom.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                SoftCard(contentPadding = PaddingValues(12.dp)) {
                    Text(
                        text = "Saved words",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tap Show to open meaning and actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(words, key = { it.id }) { entry ->
                WordTableRow(
                    entry = entry,
                    expanded = expandedRowId == entry.id,
                    busy = isBusy,
                    onToggle = {
                        expandedRowId = if (expandedRowId == entry.id) null else entry.id
                    },
                    onEdit = { editTarget = entry },
                    onDelete = { onDelete(entry.id) },
                )
            }
        }
    }

    if (editTarget != null) {
        EditDialog(
            entry = editTarget!!,
            busy = isBusy,
            onDismiss = { editTarget = null },
            onSave = { id, word, meaning, occ ->
                onSaveEdit(id, word, meaning, occ) {
                    editTarget = null
                }
            },
        )
    }
}

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    selectedText: String,
    options: List<String>,
    enabled: Boolean,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Text("v")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onOptionSelected(index)
                        },
                    )
                }
            }
        }
    }
}

private fun sortMethodLabel(method: SortMethod): String {
    return when (method) {
        SortMethod.OCCURRENCE -> "Occurrence"
        SortMethod.ALPHABETICAL -> "Alphabetical"
        SortMethod.ADDED_DATE -> "Added date"
    }
}

@Composable
private fun WordTableRow(
    entry: WordEntity,
    expanded: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = RoundedCornerShape(18.dp),
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.word,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "x${entry.occurrence}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Added ${formatAddedDate(entry.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    OutlinedButton(
                        onClick = onToggle,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text(if (expanded) "Hide" else "Show")
                    }
                }
            }

            if (expanded) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Text(
                        text = entry.meaning,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Edit")
                    }
                    Button(
                        onClick = onDelete,
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDialog(
    entry: WordEntity,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit,
) {
    var word by remember(entry.id) { mutableStateOf(entry.word) }
    var meaning by remember(entry.id) { mutableStateOf(entry.meaning) }
    var occurrence by remember(entry.id) { mutableStateOf(entry.occurrence.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("German word") },
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("Meaning") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = occurrence,
                    onValueChange = { occurrence = it },
                    label = { Text("Occurrence") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(entry.id, word, meaning, occurrence) },
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SettingsPage(
    apiKeyInput: String,
    maskedApiKey: String,
    isBusy: Boolean,
    errorText: String,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScrollHeaderTitle()

        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI Settings", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Current key: $maskedApiKey",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Model: gpt-4.1-mini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenAI API key") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSave,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Save API Key")
                    }
                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Remove Key")
                    }
                }

                if (errorText.isNotBlank()) {
                    Text(
                        errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SoftCard {
            Text("Tips", style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep your API key private and rotate it regularly. You can still add and edit words without AI definitions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FloralBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF5FA),
                    Color(0xFFFFE7F2),
                    Color(0xFFFBD5E8),
                ),
            ),
        )

        drawCircle(
            color = Color(0x22FFFFFF),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.76f, size.height * 0.2f),
        )

        drawCircle(
            color = Color(0x22FFFFFF),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.12f, size.height * 0.78f),
        )

        drawFlower(
            center = Offset(size.width * 0.15f, size.height * 0.17f),
            petalRadius = size.minDimension * 0.033f,
            petals = 8,
            petalSpread = size.minDimension * 0.052f,
            petalColor = Color(0x66FFFFFF),
            coreColor = Color(0x66E89AB9),
        )

        drawFlower(
            center = Offset(size.width * 0.82f, size.height * 0.14f),
            petalRadius = size.minDimension * 0.028f,
            petals = 7,
            petalSpread = size.minDimension * 0.046f,
            petalColor = Color(0x55FFFFFF),
            coreColor = Color(0x66D87AA0),
        )

        drawFlower(
            center = Offset(size.width * 0.82f, size.height * 0.86f),
            petalRadius = size.minDimension * 0.04f,
            petals = 9,
            petalSpread = size.minDimension * 0.062f,
            petalColor = Color(0x55FFFFFF),
            coreColor = Color(0x66C95D88),
        )

        drawFlower(
            center = Offset(size.width * 0.22f, size.height * 0.84f),
            petalRadius = size.minDimension * 0.025f,
            petals = 6,
            petalSpread = size.minDimension * 0.041f,
            petalColor = Color(0x55FFFFFF),
            coreColor = Color(0x668B4A65),
        )
    }
}

private fun DrawScope.drawFlower(
    center: Offset,
    petalRadius: Float,
    petals: Int,
    petalSpread: Float,
    petalColor: Color,
    coreColor: Color,
) {
    repeat(petals) { index ->
        val angleDegrees = index * (360f / petals)
        val angleRadians = Math.toRadians(angleDegrees.toDouble())
        val petalCenter = Offset(
            x = center.x + (cos(angleRadians) * petalSpread).toFloat(),
            y = center.y + (sin(angleRadians) * petalSpread).toFloat(),
        )

        drawCircle(
            color = petalColor,
            radius = petalRadius,
            center = petalCenter,
        )
    }

    drawCircle(
        color = coreColor,
        radius = petalRadius * 0.62f,
        center = center,
    )

    drawCircle(
        color = coreColor.copy(alpha = 0.6f),
        radius = petalRadius * 0.95f,
        center = center,
        style = Stroke(width = petalRadius * 0.18f),
    )
}

private fun formatAddedDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yy-MM-dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
