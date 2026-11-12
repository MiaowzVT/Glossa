package de.miaowz.glossa

import android.app.Application
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.db.GlossaDatabase

class GlossaApplication : Application() {
    /** Manual dependency container; created lazily so tests and previews don't open the database. */
    val grammarRepository: GrammarRepository by lazy {
        GrammarRepository(GlossaDatabase.create(this).grammarDao())
    }
}
