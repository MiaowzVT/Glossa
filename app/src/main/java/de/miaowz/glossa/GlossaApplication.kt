package de.miaowz.glossa

import android.app.Application
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.data.db.GlossaDatabase

class GlossaApplication : Application() {
    // Manual dependency container; created lazily so tests and previews don't open the database.
    private val database: GlossaDatabase by lazy { GlossaDatabase.create(this) }

    val grammarRepository: GrammarRepository by lazy {
        GrammarRepository(database.grammarDao(), database.taxonomyDao())
    }

    val vocabularyRepository: VocabularyRepository by lazy {
        VocabularyRepository(database.vocabularyDao(), database.taxonomyDao())
    }
}
