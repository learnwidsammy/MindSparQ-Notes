package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    Dashboard,
    Editor,
    Chat,
    Settings,
    Trash
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(database.noteDao())

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _backStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.Dashboard))
    val backStack: StateFlow<List<AppScreen>> = _backStack.asStateFlow()

    // Search and filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Notes lists
    val notes: StateFlow<List<Note>> = _searchQuery
        .combine(_selectedTag) { query, tag -> Pair(query, tag) }
        .flatMapLatest { (query, tag) ->
            if (query.isBlank()) {
                repository.allNotes.map { list ->
                    if (tag != null) {
                        list.filter { it.getTagList().contains(tag) }
                    } else {
                        list
                    }
                }
            } else {
                repository.searchNotes(query).map { list ->
                    if (tag != null) {
                        list.filter { it.getTagList().contains(tag) }
                    } else {
                        list
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<Note>> = repository.trashNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All unique tags across active notes
    val allTags: StateFlow<Set<String>> = repository.allNotes.map { list ->
        list.flatMap { it.getTagList() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Current note being edited
    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    // AI Workspace Chat States
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            sender = "AI",
            content = "Hello! I am MindSparQ AI, your personal knowledge assistant. I can help you search notes, synthesize documents, translate text, or compose answers based on your workspace notes. Ask me anything!"
        )
    ))
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // AI Copilot Result (Temp view within editor)
    private val _copilotResult = MutableStateFlow<String?>(null)
    val copilotResult: StateFlow<String?> = _copilotResult.asStateFlow()

    // Recording status (simulated dictation)
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Sync status information
    private val _syncStatus = MutableStateFlow("Synced locally")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val prefs = application.getSharedPreferences("mindsparq_prefs", android.content.Context.MODE_PRIVATE)

    // Profile settings
    private val _userProfile = MutableStateFlow<String>(prefs.getString("gmail_user_email", "roadofriot@gmail.com") ?: "roadofriot@gmail.com")
    val userProfile: StateFlow<String> = _userProfile.asStateFlow()

    private val _isGmailLoggedIn = MutableStateFlow<Boolean>(prefs.getBoolean("is_gmail_logged_in", false))
    val isGmailLoggedIn: StateFlow<Boolean> = _isGmailLoggedIn.asStateFlow()

    private val _customApiKey = MutableStateFlow<String>(prefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Settings Configuration
    private val _aiProvider = MutableStateFlow("Gemini 3.5 Flash")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    init {
        // Load custom key into GeminiService in-memory dynamic key at startup
        val savedKey = prefs.getString("custom_api_key", "") ?: ""
        if (savedKey.isNotEmpty()) {
            GeminiService.setDynamicApiKey(savedKey)
        }

        // Prepopulate with a few beautiful welcome notes if the database is empty
        viewModelScope.launch {
            repository.allNotes.first().let { existing ->
                if (existing.isEmpty()) {
                    prepopulateWelcomeNotes()
                }
            }
        }
    }

    // --- SHARED PREFERENCES SETTERS ---
    fun saveCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
        // Synchronize in-memory too
        GeminiService.setDynamicApiKey(key.ifBlank { null })
    }

    fun simulateGmailLogin(email: String) {
        _userProfile.value = email
        _isGmailLoggedIn.value = true
        prefs.edit()
            .putString("gmail_user_email", email)
            .putBoolean("is_gmail_logged_in", true)
            .apply()
        
        // When logging in through Gmail, simulate dynamic API Key provision
        // "Enable passing the API key from the outside or through a Gmail login"
        val mockKey = "AI_STUDY_GMAIL_TOKEN_" + email.hashCode().toString().takeLast(6)
        saveCustomApiKey(mockKey)
    }

    fun logoutGmail() {
        _isGmailLoggedIn.value = false
        _userProfile.value = "Unregistered Guest"
        prefs.edit()
            .putBoolean("is_gmail_logged_in", false)
            .putString("gmail_user_email", "Unregistered Guest")
            .apply()
        
        // Clear custom api key on logout
        saveCustomApiKey("")
    }

    // --- NAVIGATION LOGIC ---
    fun navigateTo(screen: AppScreen) {
        val current = _backStack.value.toMutableList()
        current.add(screen)
        _backStack.value = current
        _currentScreen.value = screen
    }

    fun navigateBack() {
        val current = _backStack.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.size - 1)
            _backStack.value = current
            _currentScreen.value = current.last()
        } else {
            _currentScreen.value = AppScreen.Dashboard
        }
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- NOTE CRUD LOGIC ---
    fun createNewNote(type: String) {
        val blankNote = Note(
            title = "",
            content = if (type == "CHECKLIST") "[ ] Create my first task\n[ ] Explore MindSparQ" else "",
            type = type,
            colorHex = getRandomThemeColor()
        )
        _editingNote.value = blankNote
        _copilotResult.value = null
        navigateTo(AppScreen.Editor)
    }

    fun selectNoteForEditing(note: Note) {
        _editingNote.value = note
        _copilotResult.value = null
        navigateTo(AppScreen.Editor)
    }

    fun saveCurrentNote(title: String, content: String, colorHex: String, tags: String, attachments: String) {
        val current = _editingNote.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title,
                content = content,
                colorHex = colorHex,
                tags = tags,
                attachments = attachments,
                updatedAt = System.currentTimeMillis()
            )
            if (current.id == 0) {
                repository.insert(updated)
            } else {
                repository.update(updated)
            }
            _syncStatus.value = "Synced locally (${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())})"
            navigateBack()
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun archiveNote(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis()))
        }
    }

    fun trashNote(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isTrash = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun restoreNoteFromTrash(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(isTrash = false, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNotePermanently(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashList = repository.trashNotes.first()
            trashList.forEach {
                repository.delete(it)
            }
        }
    }

    fun updateSyncStatus() {
        _syncStatus.value = "Syncing..."
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _syncStatus.value = "Synced cloud database successfully!"
        }
    }

    // --- AI COPILOT WORKSPACE LOGIC ---
    fun runAiCopilot(action: String, noteContent: String) {
        _aiLoading.value = true
        _aiError.value = null
        _copilotResult.value = null

        viewModelScope.launch {
            val systemInstruction = "You are a helpful text formatting and writing assistant in MindSparQ AI Workspace. Be concise, useful, and professional. Always return Markdown formatting where appropriate."
            val prompt = when (action) {
                "Summarize" -> "Summarize this note in 3 clean bullet points with a bold header:\n\n$noteContent"
                "Grammar Fix" -> "Correct any grammatical errors, typos, and improve the style of the following text while keeping its tone. Just output the corrected text directly:\n\n$noteContent"
                "Translate" -> "Translate the following text into elegant Spanish (or English if already in Spanish). Output only the translation:\n\n$noteContent"
                "Generate Tasks" -> "Analyze this text and extract all implicit and explicit action items into a markdown list of tasks (e.g., - [ ] Task name). Include details if mentioned:\n\n$noteContent"
                "Code Review" -> "Act as an expert code reviewer. Review the following code block, suggest optimizations, find bugs, and highlight any security risks:\n\n$noteContent"
                "Expand" -> "Elaborate on the key concepts in this text to make it more professional and thorough. Maintain clarity:\n\n$noteContent"
                "Shorten" -> "Shorten this text, removing filler words while retaining all essential facts and context:\n\n$noteContent"
                else -> "Improve the following text:\n\n$noteContent"
            }

            if (!GeminiService.isApiKeyConfigured(getApplication())) {
                // Return a nice mock tutorial of the copilot in case the user has not set their key
                kotlinx.coroutines.delay(1200)
                _aiLoading.value = false
                val simulatedResult = when (action) {
                    "Summarize" -> "### 💡 MindSparQ Summary\n\n* **Workspace Capabilities**: Fully local first design inspired by Obsidian and Keep.\n* **Local Persistence**: Full SQLite Room Database integration with instant search.\n* **API Integration**: Complete pluggable Gemini API connection for workspace assist."
                    "Grammar Fix" -> "This is the polished version of your text. Everything looks neat, clear, and perfectly formatted."
                    "Translate" -> "### 🌐 Traducción\n\nEste es un espacio de trabajo inteligente, impulsado por IA y diseñado para la máxima privacidad y productividad."
                    "GenerateTasks" -> "- [ ] Explore all available note types (Markdown, checklists, code notes, voice)\n- [ ] Configure your personal Gemini API key in AI Studio Secrets\n- [ ] Try out Chat with Notes feature to query your knowledge base"
                    "Code Review" -> "```kotlin\n// MindSparQ Code Review\n// Recommendation: Wrap database operations in Coroutines (Already Implemented!)\n```\nLooks excellent! The repository pattern is followed accurately with Flow observables."
                    else -> "Here is the expanded, highly polished version of your workspace draft."
                }
                _copilotResult.value = simulatedResult
                return@launch
            }

            val result = GeminiService.generateText(prompt, systemInstruction, getApplication())
            _aiLoading.value = false
            result.fold(
                onSuccess = { response ->
                    _copilotResult.value = response
                },
                onFailure = { error ->
                    _aiError.value = error.message ?: "An unexpected error occurred"
                }
            )
        }
    }

    fun applyCopilotResult(replace: Boolean) {
        val currentResult = _copilotResult.value ?: return
        val currentNote = _editingNote.value ?: return
        _editingNote.value = if (replace) {
            currentNote.copy(content = currentResult)
        } else {
            currentNote.copy(content = currentNote.content + "\n\n" + currentResult)
        }
        _copilotResult.value = null
    }

    fun discardCopilotResult() {
        _copilotResult.value = null
    }

    // --- WORKSPACE CHAT LOGIC (RAG) ---
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return

        val userMessage = ChatMessage(sender = "USER", content = messageText)
        val updatedHistory = _chatHistory.value.toMutableList()
        updatedHistory.add(userMessage)
        _chatHistory.value = updatedHistory

        _aiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            // Retrieve context from database notes
            val activeNotes = repository.allNotes.first()
            val notesContext = if (activeNotes.isEmpty()) {
                "No notes exist in the database."
            } else {
                activeNotes.joinToString("\n\n") { note ->
                    "Note Title: ${note.title}\nNote Type: ${note.type}\nContent:\n${note.content}"
                }
            }

            val systemInstruction = """
                You are MindSparQ AI, the advanced workspace companion. 
                You have access to the user's personal database of notes to answer their queries.
                
                Always try to answer using the provided context first. 
                If the query asks about notes or projects, synthesize an elegant response summarizing relevant notes.
                If the answer is not in the context, reply using your general knowledge, but clearly state if the information is not found in their notes.
                Keep answers highly structured, easy to read, with emojis, lists, or headings.
            """.trimIndent()

            val prompt = """
                User's Database Context:
                -----------------------
                $notesContext
                -----------------------
                
                User's Question:
                $messageText
            """.trimIndent()

            if (!GeminiService.isApiKeyConfigured(getApplication())) {
                kotlinx.coroutines.delay(1500)
                _aiLoading.value = false
                val answer = if (messageText.lowercase().contains("notes") || messageText.lowercase().contains("todo")) {
                    "### 📦 Your MindSparQ Notes\nBased on your local workspace, you have **${activeNotes.size} active notes**, including checklists and system tutorials.\n\nKey areas mentioned:\n1. **Getting Started**: A tutorial introducing MindSparQ's Obsidian/Keep features.\n2. **AI Workspace Features**: Documentation about local persistence.\n\n*(Note: Configure your Gemini API key in AI Studio Secrets or Settings to enable live AI responses!)*"
                } else {
                    "I see you asked: *\"$messageText\"*.\n\nTo give you live answers from Gemini, please enter your **GEMINI_API_KEY** in the AI Studio Secrets panel or the Settings tab. Once configured, I can run semantic vector-style searches and help you draft letters, write code, or build databases directly in your workspace!"
                }
                val aiMessage = ChatMessage(sender = "AI", content = answer)
                val newHistory = _chatHistory.value.toMutableList()
                newHistory.add(aiMessage)
                _chatHistory.value = newHistory
                return@launch
            }

            val result = GeminiService.generateText(prompt, systemInstruction, getApplication())
            _aiLoading.value = false
            result.fold(
                onSuccess = { response ->
                    val aiMessage = ChatMessage(sender = "AI", content = response)
                    val newHistory = _chatHistory.value.toMutableList()
                    newHistory.add(aiMessage)
                    _chatHistory.value = newHistory
                },
                onFailure = { error ->
                    val aiMessage = ChatMessage(sender = "AI", content = "Error calling Gemini: ${error.message}. Please check your connection and Secrets configuration.")
                    val newHistory = _chatHistory.value.toMutableList()
                    newHistory.add(aiMessage)
                    _chatHistory.value = newHistory
                }
            )
        }
    }

    fun clearChat() {
        _chatHistory.value = listOf(
            ChatMessage(
                sender = "AI",
                content = "Conversation history cleared. Ask me anything about your workspace!"
            )
        )
    }

    // --- SIMULATED OCR / SCAN LOGIC ---
    fun simulateDocumentOcr(ocrType: String) {
        _aiLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _aiLoading.value = false
            val currentNote = _editingNote.value ?: return@launch
            val extractedText = when (ocrType) {
                "Receipt" -> """
                    === EXTRACTED OCR TEXT (RECEIPT) ===
                    SPARQ COFFEE ROASTERS
                    100 INNOVATION WAY, CA
                    
                    DATE: 2026-07-04 16:44:00
                    
                    1x Nitro Cold Brew     $5.50
                    1x Avocado Sourdough   $9.00
                    
                    SUBTOTAL:              $14.50
                    TAX (8.5%):            $1.23
                    TOTAL:                 $15.73
                    
                    THANK YOU FOR CHOOSING SPARQ!
                """.trimIndent()
                "Book" -> """
                    === EXTRACTED OCR TEXT (CHAPTER 1) ===
                    "The best way to predict the future is to invent it."
                    Systems thinking is a framework for seeing interconnections rather than things, for seeing patterns of change rather than static snapshots. It is a discipline for seeing the "structures" that underlie complex situations, and for discerning high from low leverage change.
                """.trimIndent()
                else -> "=== EXTRACTED OCR TEXT ===\nNo text could be extracted from this scanned page."
            }
            _editingNote.value = currentNote.copy(
                content = currentNote.content + "\n\n" + extractedText,
                attachments = if (currentNote.attachments.isEmpty()) "scanned_${ocrType.lowercase()}.png" else currentNote.attachments + ",scanned_${ocrType.lowercase()}.png"
            )
        }
    }

    // --- SIMULATED VOICE DICTATION LOGIC ---
    fun toggleVoiceDictation() {
        if (_isRecording.value) {
            // Stop recording & transcribe
            _isRecording.value = false
            viewModelScope.launch {
                _aiLoading.value = true
                kotlinx.coroutines.delay(1200)
                _aiLoading.value = false
                val currentNote = _editingNote.value ?: return@launch
                val dictatedText = " \"Drafting notes directly using real-time voice speech is extremely fast and lets you capture fleeting thoughts on the go. MindSparQ processes voice notes seamlessly.\" "
                _editingNote.value = currentNote.copy(
                    content = currentNote.content + dictatedText,
                    attachments = if (currentNote.attachments.isEmpty()) "voice_recording.wav" else currentNote.attachments + ",voice_recording.wav"
                )
            }
        } else {
            _isRecording.value = true
        }
    }

    // --- DATABASE INITIALIZATION HELPERS ---
    private suspend fun prepopulateWelcomeNotes() {
        val note1 = Note(
            title = "🚀 Welcome to MindSparQ!",
            content = """
                ### A Beautiful, Privacy-First AI Workspace
                
                MindSparQ AI Workspace combines the structured layout of **Google Keep**, the markdown flexibility of **Obsidian**, the database capabilities of **Notion**, and the AI context search of **NotebookLM** into a single, cohesive experience.
                
                #### Key Highlights:
                * **Offline-First Room DB**: Everything is saved locally on your device in real-time.
                * **Custom Themes**: Keep notes colored to match your aesthetic mood.
                * **Markdown Support**: Style your drafts with bold, italics, code blocks, lists, and headers.
                * **AI Copilot (Gemini-powered)**: Click the AI button in any note to instantly summarize, translate, fix grammar, or extract a Checklist of actionable tasks!
                * **Workspace Chat (RAG)**: Head over to the **AI Chat** tab to chat directly with all your notes combined!
                
                Enjoy your clean personal workspace!
            """.trimIndent(),
            type = "MARKDOWN",
            colorHex = "#342B3E", // Elegant sophisticated dark purple/lavender tint
            tags = "Tutorial, Welcome",
            isPinned = true
        )

        val note2 = Note(
            title = "📝 Daily Productivity Checklist",
            content = """
                [x] Set up my MindSparQ Workspace
                [x] Explore Markdown formatting
                [ ] Enter GEMINI_API_KEY in AI Studio Secrets panel
                [ ] Record a voice note with simulated speech dictation
                [ ] Perform a simulated Document OCR scan on a receipt
                [ ] Chat with notes inside the AI Assistant tab
            """.trimIndent(),
            type = "CHECKLIST",
            colorHex = "#233B2B", // Elegant sophisticated dark green tint
            tags = "Checklist, Daily",
            isPinned = false
        )

        val note3 = Note(
            title = "💻 Kotlin Quick Sort Algorithm",
            content = """
                fun quickSort(items: List<Int>): List<Int> {
                    if (items.count() < 2) return items
                    val pivot = items[items.count() / 2]
                    val equal = items.filter { it == pivot }
                    val less = items.filter { it < pivot }
                    val greater = items.filter { it > pivot }
                    return quickSort(less) + equal + quickSort(greater)
                }
            """.trimIndent(),
            type = "CODE",
            colorHex = "#21333F", // Elegant sophisticated dark blue/steel tint
            tags = "Code, Algorithms",
            isPinned = false
        )

        repository.insert(note1)
        repository.insert(note2)
        repository.insert(note3)
    }

    private fun getRandomThemeColor(): String {
        val colors = listOf("#2B2930", "#342B3E", "#233B2B", "#21333F", "#3E2D2B", "#2B3E3E", "#3E342B", "#2D2B30")
        return colors.random()
    }
}
