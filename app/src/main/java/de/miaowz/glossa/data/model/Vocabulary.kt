package de.miaowz.glossa.data.model

import de.miaowz.glossa.richtext.RichText

data class VocabularyEntry(
    val id: Long,
    /** The word or phrase, may contain ruby markup such as `{漢字|かんじ}`. */
    val term: String,
    /** Short translation or gloss, may contain ruby markup; empty if not set. */
    val meaning: String,
    val notes: RichText,
    val language: Language,
    val category: Category?,
    val examples: List<ExampleSentence>,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Content of a vocabulary entry as edited by the user, before it is saved. */
data class VocabularyEntryDraft(
    val id: Long = 0,
    val term: String,
    val meaning: String,
    val notes: RichText,
    val languageId: Long,
    val categoryId: Long?,
    val examples: List<ExampleSentence>,
)
