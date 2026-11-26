package de.miaowz.glossa.data.model

import de.miaowz.glossa.richtext.RichText

/**
 * A curated course from the backend (read-only in the app). [name] and [description] are plain
 * strings that may contain `{base|reading}` ruby markup; [level] and [language] are free text
 * set in the admin panel (e.g. "N5", "Japanese").
 */
data class OfficialCourse(
    val id: Long,
    val name: String,
    val description: String,
    val level: String,
    val language: String,
    val updatedAt: Long,
)