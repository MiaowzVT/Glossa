package de.miaowz.glossa.data.model

import de.miaowz.glossa.richtext.RichText

data class Language(
    val id: Long,
    val name: String,
)

/** Sub-category inside a [Language]. */
data class Category(
    val id: Long,
    val languageId: Long,
    val name: String,
)

data class ExampleSentence(
    val sentence: RichText,
    val translation: RichText = RichText.Empty,
)

data class GrammarEntry(
    val id: Long,
    /** Short title, may contain ruby markup such as `{間|あいだ}に`. */
    val title: String,
    val description: RichText,
    val language: Language,
    val category: Category?,
    val examples: List<ExampleSentence>,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Content of an entry as edited by the user, before it is saved. */
data class GrammarEntryDraft(
    val id: Long = 0,
    val title: String,
    val description: RichText,
    val languageId: Long,
    val categoryId: Long?,
    val examples: List<ExampleSentence>,
)
