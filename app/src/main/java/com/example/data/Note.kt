package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val type: String = "TEXT", // "TEXT", "MARKDOWN", "CHECKLIST", "CODE", "VOICE"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrash: Boolean = false,
    val colorHex: String = "#FFFFFF", // Default background color
    val tags: String = "", // Comma-separated tags
    val attachments: String = "" // Comma-separated attachment names/paths
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getAttachmentList(): List<String> {
        if (attachments.isBlank()) return emptyList()
        return attachments.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
