package de.miaowz.glossa.data.model

import de.miaowz.glossa.richtext.RichText

/** A lecture of an [OfficialCourse]; [name] may contain ruby markup. */
data class OfficialLecture(
    val id: Long,
    val courseId: Long,
    val name: String,
    val contents: RichText,
    val updatedAt: Long,
)
