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
    ],
    version = 2,
    autoMigrations = [
        // Adds vocabulary tables and scopes categories by CategoryType.
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class GlossaDatabase : RoomDatabase() {
    abstract fun taxonomyDao(): TaxonomyDao
    abstract fun grammarDao(): GrammarDao
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        fun create(context: Context): GlossaDatabase =
            Room.databaseBuilder(context, GlossaDatabase::class.java, "glossa.db").build()
    }
}
