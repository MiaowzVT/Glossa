package de.miaowz.glossa.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import de.miaowz.glossa.srs.Rating

@Entity(
    tableName = "languages",
    indices = [Index(value = ["name"], unique = true)],
)
data class LanguageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

/** Which library a category belongs to; categories are not shared between libraries. */
enum class CategoryType { GRAMMAR, VOCABULARY }

/** A sub-category inside a language, e.g. "Particles" in Japanese. */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["languageId", "type", "name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val languageId: Long,
    @ColumnInfo(defaultValue = "GRAMMAR") val type: CategoryType,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

/** Rich text columns hold [de.miaowz.glossa.richtext.RichText] JSON. */
@Entity(
    tableName = "grammar_entries",
    foreignKeys = [
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("languageId"), Index("categoryId")],
)
data class GrammarEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    /** Description without formatting and ruby readings, for search. */
    val descriptionPlain: String,
    val languageId: Long,
    val categoryId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "example_sentences",
    foreignKeys = [
        ForeignKey(
            entity = GrammarEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class ExampleSentenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val position: Int,
    val sentence: String,
    val translation: String,
)

data class GrammarEntryWithDetails(
    @Embedded val entry: GrammarEntryEntity,
    @Relation(parentColumn = "languageId", entityColumn = "id")
    val language: LanguageEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val examples: List<ExampleSentenceEntity>,
)

/** [term] may contain ruby markup; [notes] holds [de.miaowz.glossa.richtext.RichText] JSON. */
@Entity(
    tableName = "vocabulary_entries",
    foreignKeys = [
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("languageId"), Index("categoryId")],
)
data class VocabularyEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val meaning: String,
    val notes: String,
    /** Notes without formatting and ruby readings, for search. */
    val notesPlain: String,
    val languageId: Long,
    val categoryId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "vocabulary_examples",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class VocabularyExampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val position: Int,
    val sentence: String,
    val translation: String,
)

data class VocabularyEntryWithDetails(
    @Embedded val entry: VocabularyEntryEntity,
    @Relation(parentColumn = "languageId", entityColumn = "id")
    val language: LanguageEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val examples: List<VocabularyExampleEntity>,
)

/** FSRS state of a vocabulary card (see [de.miaowz.glossa.srs.CardState]); no row means the card is new. */
@Entity(
    tableName = "vocabulary_review_states",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("due")],
)
data class VocabularyReviewStateEntity(
    @PrimaryKey val entryId: Long,
    val stability: Double,
    val difficulty: Double,
    /** Epoch ms; the card is due from this point on. */
    val due: Long,
    val lastReview: Long,
    val reps: Int,
    val lapses: Int,
)

/** One answer per row, kept for statistics and future FSRS parameter optimization. */
@Entity(
    tableName = "vocabulary_review_logs",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class VocabularyReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val rating: Rating,
    val reviewedAt: Long,
)

@Entity(tableName = "official_course")
data class OfficialCourseEntity(
    @PrimaryKey val id: Long,
    val courseName: String,
    val courseDescription: String,
    val courseLevel: String,
    val courseLanguage: String,
    val createdAt: Long,
    val updatedAt: Long
)

