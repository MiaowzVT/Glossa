package de.miaowz.glossa.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LanguageEntity::class,
        CategoryEntity::class,
        GrammarEntryEntity::class,
        ExampleSentenceEntity::class,
        VocabularyEntryEntity::class,
        VocabularyExampleEntity::class,
        VocabularyReviewStateEntity::class,
        VocabularyReviewLogEntity::class,
        OfficialCourseEntity::class,
        OfficialCourseLectureEntity::class,
    ],
    version = 6,
    autoMigrations = [
        // Adds vocabulary tables and scopes categories by CategoryType.
        AutoMigration(from = 1, to = 2),
        // Adds the vocabulary review (FSRS) tables.
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
abstract class GlossaDatabase : RoomDatabase() {
    abstract fun taxonomyDao(): TaxonomyDao
    abstract fun grammarDao(): GrammarDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun officialCourseDao(): OfficialCourseDao
    abstract fun officialCourseLectureDao(): OfficialCourseLectureDao

    companion object {
        fun create(context: Context): GlossaDatabase =
            Room.databaseBuilder(context, GlossaDatabase::class.java, "glossa.db").build()
    }
}
