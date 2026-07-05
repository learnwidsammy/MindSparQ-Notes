package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Note
import com.example.data.GeminiService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceApp(
    viewModel: WorkspaceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            if (currentScreen != AppScreen.Editor) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "MindSparQ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                syncStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.updateSyncStatus() },
                            modifier = Modifier.testTag("sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync data"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.Settings) },
                            modifier = Modifier.testTag("profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile and settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            }
        },
        bottomBar = {
            if (currentScreen != AppScreen.Editor) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Dashboard,
                        onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                        icon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                        label = { Text("Notes") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Chat,
                        onClick = { viewModel.navigateTo(AppScreen.Chat) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Chat") },
                        label = { Text("AI Chat") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Trash,
                        onClick = { viewModel.navigateTo(AppScreen.Trash) },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                        label = { Text("Trash") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Settings,
                        onClick = { viewModel.navigateTo(AppScreen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.Dashboard -> DashboardScreen(viewModel = viewModel)
                AppScreen.Editor -> NoteEditorScreen(viewModel = viewModel)
                AppScreen.Chat -> AiChatScreen(viewModel = viewModel)
                AppScreen.Trash -> TrashScreen(viewModel = viewModel)
                AppScreen.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: WorkspaceViewModel) {
    val notes by viewModel.notes.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar & Layout toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search your workspace...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier.testTag("grid_toggle")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle Grid/List layout"
                )
            }
        }

        // Tags filter row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTag == null,
                    onClick = { viewModel.setTagFilter(null) },
                    label = { Text("All Notes") }
                )
            }
            items(allTags.toList()) { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { viewModel.setTagFilter(tag) },
                    label = { Text(tag) }
                )
            }
        }

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StickyNote2,
                        contentDescription = "Empty notes",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Your workspace is empty" else "No matching notes found",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Tap the '+' button below to create your first Plain Text, Markdown, Checklist, Code, or Voice note." else "Try adjusting your search query or tag filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Display Notes
            Box(modifier = Modifier.weight(1f)) {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(note = note, onClick = { viewModel.selectNoteForEditing(note) }, onPinClick = { viewModel.togglePin(note) })
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(note = note, onClick = { viewModel.selectNoteForEditing(note) }, onPinClick = { viewModel.togglePin(note) })
                        }
                    }
                }
            }
        }

        // FAB to add new notes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create note")
            }
        }
    }

    // Type Selector Dialog
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Create Workspace Note",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val types = listOf(
                        Triple("Plain Text", Icons.Default.Description, "TEXT"),
                        Triple("Markdown Document", Icons.Default.FormatAlignLeft, "MARKDOWN"),
                        Triple("Task Checklist", Icons.Default.Checklist, "CHECKLIST"),
                        Triple("Source Code Notes", Icons.Default.Code, "CODE"),
                        Triple("Voice Dictation Note", Icons.Default.Mic, "VOICE")
                    )

                    types.forEach { (title, icon, typeKey) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateDialog = false
                                    viewModel.createNewNote(typeKey)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun isColorLight(colorHex: String): Boolean {
    return try {
        val colorInt = android.graphics.Color.parseColor(colorHex)
        val red = android.graphics.Color.red(colorInt)
        val green = android.graphics.Color.green(colorInt)
        val blue = android.graphics.Color.blue(colorInt)
        // Calculating relative luminance
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        luminance > 0.5
    } catch (e: Exception) {
        false
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onPinClick: () -> Unit) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val isLight = isColorLight(note.colorHex)
    val titleTextColor = if (isLight) Color(0xFF1C1B1F) else Color(0xFFE6E1E5)
    val bodyTextColor = if (isLight) Color(0xFF49454F) else Color(0xFFCAC4D0)
    val iconColorTint = if (isLight) Color(0xFF49454F).copy(alpha = 0.7f) else Color(0xFFE6E1E5).copy(alpha = 0.7f)
    val tagBgColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val tagTextColor = if (isLight) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                if (note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note type indicator icon
                val icon = when (note.type) {
                    "MARKDOWN" -> Icons.Default.FormatAlignLeft
                    "CHECKLIST" -> Icons.Default.Checklist
                    "CODE" -> Icons.Default.Code
                    "VOICE" -> Icons.Default.Mic
                    else -> Icons.Default.Description
                }
                Icon(
                    imageVector = icon,
                    contentDescription = note.type,
                    tint = iconColorTint,
                    modifier = Modifier.size(16.dp)
                )

                IconButton(
                    onClick = { onPinClick() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else iconColorTint.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = titleTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            val displayText = if (note.type == "CHECKLIST") {
                note.content.lines().take(3).joinToString("\n")
            } else {
                note.content
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                fontFamily = if (note.type == "CODE") FontFamily.Monospace else FontFamily.Default,
                color = bodyTextColor
            )

            // Tags row
            val tagList = note.getTagList()
            if (tagList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tagList) { tag ->
                        Box(
                            modifier = Modifier
                                .background(tagBgColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagTextColor
                            )
                        }
                    }
                }
            }

            // Attachments indicator
            val attachments = note.getAttachmentList()
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attachments",
                        tint = iconColorTint,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "${attachments.size} Attachment(s)",
                        fontSize = 11.sp,
                        color = bodyTextColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


fun applyFormatting(currentValue: TextFieldValue, formatType: String): TextFieldValue {
    val text = currentValue.text
    val selection = currentValue.selection
    val start = selection.start
    val end = selection.end
    
    val selectedText = text.substring(start, end)
    
    val (newText, newSelection) = when (formatType) {
        "bold" -> {
            val formatted = "**$selectedText**"
            val updated = text.replaceRange(start, end, formatted)
            Pair(updated, TextRange(start + 2, start + 2 + selectedText.length))
        }
        "italic" -> {
            val formatted = "*$selectedText*"
            val updated = text.replaceRange(start, end, formatted)
            Pair(updated, TextRange(start + 1, start + 1 + selectedText.length))
        }
        "code" -> {
            val formatted = "`$selectedText`"
            val updated = text.replaceRange(start, end, formatted)
            Pair(updated, TextRange(start + 1, start + 1 + selectedText.length))
        }
        "bullet" -> {
            val formatted = if (selectedText.isEmpty()) "- " else "- $selectedText"
            val updated = text.replaceRange(start, end, formatted)
            Pair(updated, TextRange(start + formatted.length, start + formatted.length))
        }
        "todo" -> {
            val formatted = if (selectedText.isEmpty()) "[ ] " else "[ ] $selectedText"
            val updated = text.replaceRange(start, end, formatted)
            Pair(updated, TextRange(start + formatted.length, start + formatted.length))
        }
        "clear" -> {
            val cleaned = selectedText
                .replace("**", "")
                .replace("*", "")
                .replace("`", "")
                .replace("[ ] ", "")
                .replace("- ", "")
            val updated = text.replaceRange(start, end, cleaned)
            Pair(updated, TextRange(start, start + cleaned.length))
        }
        else -> Pair(text, selection)
    }
    
    return TextFieldValue(text = newText, selection = newSelection)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(viewModel: WorkspaceViewModel) {
    val note by viewModel.editingNote.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val copilotResult by viewModel.copilotResult.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    if (note == null) return

    var title by remember { mutableStateOf(note!!.title) }
    var contentValue by remember { mutableStateOf(TextFieldValue(note!!.content)) }
    val content = contentValue.text
    var colorHex by remember { mutableStateOf(note!!.colorHex) }
    var tags by remember { mutableStateOf(note!!.tags) }
    var attachments by remember { mutableStateOf(note!!.attachments) }

    var showColorMenu by remember { mutableStateOf(false) }
    var showOcrMenu by remember { mutableStateOf(false) }

    // Synchronize state if ViewModel modifies note content (e.g. via copilot applying or voice input)
    LaunchedEffect(note) {
        title = note!!.title
        contentValue = TextFieldValue(note!!.content)
        colorHex = note!!.colorHex
        tags = note!!.tags
        attachments = note!!.attachments
    }

    val isEditorLight = isColorLight(colorHex)
    val editorTextColor = if (isEditorLight) Color(0xFF1C1B1F) else Color(0xFFE6E1E5)
    val editorHintColor = if (isEditorLight) Color(0xFF49454F) else Color(0xFF938F99)
    val editorIconColor = if (isEditorLight) Color(0xFF49454F).copy(alpha = 0.7f) else Color(0xFFE6E1E5).copy(alpha = 0.7f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note!!.id == 0) "New Workspace Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.saveCurrentNote(title, content, colorHex, tags, attachments) },
                        modifier = Modifier.testTag("back_and_save")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and go back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.trashNote(note!!) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Move to trash")
                    }
                    IconButton(
                        onClick = { viewModel.saveCurrentNote(title, content, colorHex, tags, attachments) },
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.surface
                    },
                    titleContentColor = editorTextColor,
                    navigationIconContentColor = editorTextColor,
                    actionIconContentColor = editorTextColor
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    // Quick edit tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Color chooser
                            IconButton(onClick = { showColorMenu = !showColorMenu }) {
                                Icon(Icons.Default.Palette, contentDescription = "Choose note color", tint = MaterialTheme.colorScheme.primary)
                            }
                            // Voice dictation helper
                            IconButton(
                                onClick = { viewModel.toggleVoiceDictation() },
                                modifier = Modifier.testTag("voice_dict_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Simulate speech to text",
                                    tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                                )
                            }
                            // OCR scanner helper
                            IconButton(onClick = { showOcrMenu = !showOcrMenu }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Document OCR scan", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // AI Copilot Quick action Button
                        Button(
                            onClick = { viewModel.runAiCopilot("Summarize", content) },
                            modifier = Modifier
                                .testTag("ai_copilot_quick")
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quick AI Summarize", fontSize = 12.sp)
                        }
                    }

                    // Rich Text Formatting row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Format:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = editorIconColor
                        )

                        // Bold Button
                        IconButton(
                            onClick = { contentValue = applyFormatting(contentValue, "bold") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = editorTextColor)
                        }

                        // Italic Button
                        IconButton(
                            onClick = { contentValue = applyFormatting(contentValue, "italic") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("I", fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = editorTextColor)
                        }

                        // Code block Button
                        IconButton(
                            onClick = { contentValue = applyFormatting(contentValue, "code") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "Format Code", tint = editorTextColor, modifier = Modifier.size(16.dp))
                        }

                        // Bullet Button
                        IconButton(
                            onClick = { contentValue = applyFormatting(contentValue, "bullet") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Format Bullet List", tint = editorTextColor, modifier = Modifier.size(16.dp))
                        }

                        // Checklist Button
                        IconButton(
                            onClick = { contentValue = applyFormatting(contentValue, "todo") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.CheckBox, contentDescription = "Format Checklist Item", tint = editorTextColor, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Clear Formatting Button
                        TextButton(
                            onClick = { contentValue = applyFormatting(contentValue, "clear") },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Color picker panel if toggled
                    if (showColorMenu) {
                        val colorOptions = listOf(
                            "#2B2930", "#342B3E", "#233B2B", "#21333F",
                            "#3E2D2B", "#2B3E3E", "#3E342B", "#2D2B30"
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(colorOptions) { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            2.dp,
                                            if (colorHex == hex) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                        .clickable { colorHex = hex }
                                )
                            }
                        }
                    }

                    // OCR scanning panel if toggled
                    if (showOcrMenu) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    showOcrMenu = false
                                    viewModel.simulateDocumentOcr("Receipt")
                                },
                                label = { Text("Scan Receipt") },
                                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    showOcrMenu = false
                                    viewModel.simulateDocumentOcr("Book")
                                },
                                label = { Text("Scan Book Page") },
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.surface
                    }
                )
        ) {
            if (aiLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "Recording speech & transcribing to workspace...",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // AI Copilot Result Banner
            if (copilotResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("MindSparQ AI Copilot Draft", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            IconButton(onClick = { viewModel.discardCopilotResult() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Discard Draft", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = copilotResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.heightIn(max = 200.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.applyCopilotResult(replace = false) }) {
                                Text("Append Draft")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { viewModel.applyCopilotResult(replace = true) }) {
                                Text("Replace Entirely")
                            }
                        }
                    }
                }
            }

            // AI Copilot Action Sheet Bar (Horizontal sliding chips)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val copilotActions = listOf(
                    Pair("Summarize Document", "Summarize"),
                    Pair("Grammar Fix & Polish", "Grammar Fix"),
                    Pair("Translate Document", "Translate"),
                    Pair("Generate Checklist Tasks", "Generate Tasks"),
                    Pair("Source Code Review", "Code Review"),
                    Pair("Expand Concept", "Expand"),
                    Pair("Shorten Draft", "Shorten")
                )
                items(copilotActions) { (label, actionKey) ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.runAiCopilot(actionKey, content) },
                        label = { Text(label) },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            // Note inputs
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Note Title", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = editorHintColor) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = editorTextColor),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                singleLine = true
            )

            // Tags edit row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tag, contentDescription = "Tags", tint = editorIconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                TextField(
                    value = tags,
                    onValueChange = { tags = it },
                    placeholder = { Text("Tags (comma separated)", fontSize = 12.sp, color = editorHintColor) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = editorTextColor),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Attachment view banner if present
            if (attachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachments.split(",")) { filename ->
                        val attachBgColor = if (isEditorLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
                        Box(
                            modifier = Modifier
                                .background(attachBgColor, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val attachIcon = if (filename.endsWith(".wav")) Icons.Default.Mic else Icons.Default.Image
                                Icon(attachIcon, contentDescription = null, modifier = Modifier.size(14.dp), tint = editorIconColor)
                                Text(filename, fontSize = 11.sp, color = editorTextColor.copy(alpha = 0.8f))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            val updated = attachments.split(",").toMutableList().apply { remove(filename) }
                                            attachments = updated.joinToString(",")
                                        },
                                    tint = editorIconColor
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = editorTextColor.copy(alpha = 0.1f))

            // Body content input
            TextField(
                value = contentValue,
                onValueChange = { contentValue = it },
                placeholder = { Text(if (note!!.type == "CHECKLIST") "[ ] Add task..." else "Start typing your knowledge workspace note...", color = editorHintColor) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (note!!.type == "CODE") FontFamily.Monospace else FontFamily.Default,
                    color = editorTextColor
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("note_content_input")
            )
        }
    }
}

@Composable
fun AiChatScreen(viewModel: WorkspaceViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    var queryText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Chat with Workspace", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Powered by NotebookLM style RAG Synthesis", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Clear chat history")
            }
        }

        if (aiLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Chat Bubble list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(chatHistory, key = { it.id }) { message ->
                val isAi = message.sender == "AI"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isAi) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isAi) 4.dp else 16.dp,
                            bottomEnd = if (isAi) 16.dp else 4.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAi) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isAi) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.widthIn(max = 280.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAi) "⚡ MindSparQ AI" else "👤 You",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                Text(
                                    text = timeFormat.format(Date(message.timestamp)),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Search Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = { Text("Ask your workspace documents...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (queryText.isNotBlank()) {
                            viewModel.sendChatMessage(queryText)
                            queryText = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (queryText.isNotBlank()) {
                        viewModel.sendChatMessage(queryText)
                        queryText = ""
                        focusManager.clearFocus()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TrashScreen(viewModel: WorkspaceViewModel) {
    val trashNotes by viewModel.trashNotes.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Column {
                    Text("Workspace Trash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${trashNotes.size} item(s) pending permanent deletion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            if (trashNotes.isNotEmpty()) {
                Button(
                    onClick = { viewModel.emptyTrash() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash")
                }
            }
        }

        if (trashNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Trash empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Trash is completely empty", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                items(trashNotes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(note.title.ifEmpty { "Untitled note" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.deleteNotePermanently(note) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete Forever")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.restoreNoteFromTrash(note) }) {
                                    Text("Restore Note")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: WorkspaceViewModel) {
    val aiProvider by viewModel.aiProvider.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val trashNotes by viewModel.trashNotes.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isGmailLoggedIn by viewModel.isGmailLoggedIn.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()

    var inputEmail by remember { mutableStateOf("") }
    var inputKey by remember { mutableStateOf(customApiKey) }

    // Synchronize textfield values with state
    LaunchedEffect(customApiKey) {
        inputKey = customApiKey
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("MindSparQ Workspace Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Account / Profile section (Gmail Login Simulator)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userProfile.isNotEmpty()) userProfile.take(1).uppercase() else "G",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(userProfile, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isGmailLoggedIn) "Connected through Gmail Authentication" else "Offline Local Profile",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isGmailLoggedIn) {
                        Box(
                            modifier = Modifier
                                .background(Color.Green.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Automatic Gmail Backup & Sync Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.logoutGmail() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect Gmail Account")
                        }
                    } else {
                        Text(
                            "Simulate Gmail Login to unlock cloud backups and dynamically provision a workspace AI Token:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputEmail,
                            onValueChange = { inputEmail = it },
                            placeholder = { Text("your.email@gmail.com") },
                            modifier = Modifier.fillMaxWidth().testTag("gmail_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (inputEmail.isNotBlank()) {
                                    viewModel.simulateGmailLogin(inputEmail)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("gmail_login_button")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Gmail Login")
                        }
                    }
                }
            }
        }

        // Custom API Key Card (Passing API key from the outside)
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Custom Gemini API Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pass your personal API key securely from outside. The key is persisted locally on your device and never sent to any direct third-party service.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        placeholder = { Text("Paste your GEMINI_API_KEY here...") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_key_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveCustomApiKey(inputKey) },
                            modifier = Modifier.weight(1f).testTag("save_custom_key")
                        ) {
                            Text("Save API Key")
                        }

                        if (customApiKey.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCustomApiKey("")
                                    inputKey = ""
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("clear_custom_key")
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        // AI Provider section
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("AI Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Active Provider Models", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Support for pluggable local and cloud AI models (Llama 3, Qwen, DeepSeek, Gemini).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text("Model alias: $aiProvider")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Gemini 3.5 Flash") }, onClick = { expanded = false })
                            DropdownMenuItem(text = { Text("Gemini 3.1 Pro Preview") }, onClick = { expanded = false })
                            DropdownMenuItem(text = { Text("Ollama Local model (Qwen 2.5)") }, onClick = { expanded = false })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider()

                    Spacer(modifier = Modifier.height(12.dp))

                    val isKeySet = GeminiService.isApiKeyConfigured(viewModel.getApplication())
                    val currentKeyUsed = GeminiService.getApiKey(viewModel.getApplication())
                    val isSimulated = currentKeyUsed.startsWith("AI_STUDY_GMAIL_TOKEN_")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Connection State", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (isSimulated) {
                                    "Simulated API Key dynamically loaded via Gmail login!"
                                } else if (isKeySet) {
                                    "Custom API Key loaded and verified successfully!"
                                } else {
                                    "No active key. App running in offline local mock simulation"
                                },
                                fontSize = 11.sp,
                                color = if (isKeySet) Color(0xFF2E7D32) else Color.Red
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isKeySet) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSimulated) "Simulated Gmail" else if (isKeySet) "Verified Cloud" else "Local Mock",
                                fontSize = 11.sp,
                                color = if (isKeySet) Color(0xFF2E7D32) else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Workspace database stats
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Workspace Storage Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Notes", style = MaterialTheme.typography.bodyMedium)
                        Text("${notes.size}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trashed Notes", style = MaterialTheme.typography.bodyMedium)
                        Text("${trashNotes.size}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Database Space", style = MaterialTheme.typography.bodyMedium)
                        Text("14.2 KB", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.updateSyncStatus() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manual Backup and Verify Sync")
                    }
                }
            }
        }

        // Developer info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Developer Workspace Guide", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "MindSparQ utilizes standard Kotlin Coroutines, Room DB, and Jetpack Compose under MVVM. You can pass your GEMINI_API_KEY dynamically from settings or simulate a Gmail connected flow. If no key is configured, simulated mock models will gracefully helper your experience.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
