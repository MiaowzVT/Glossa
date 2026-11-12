package de.miaowz.glossa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LanguageEntity::class,
        CategoryEntity::class,
        GrammarEntryEntity::class,
        ExampleSentenceEntity::class,
    ],
    version = 1,
)
abstract class GlossaDatabase : RoomDatabase() {
    abstract fun grammarDao(): GrammarDao

    companion object {
        fun create(context: Context): GlossaDatabase =
            Room.databaseBuilder(context, GlossaDatabase::class.java, "glossa.db").build()
    }
}
