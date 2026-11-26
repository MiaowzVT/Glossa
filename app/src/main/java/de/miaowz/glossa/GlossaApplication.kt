package de.miaowz.glossa

import android.app.Application
import android.util.Log
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.data.db.GlossaDatabase
import de.miaowz.glossa.network.OfficialCourseApi
import de.miaowz.glossa.network.OfficialCourseLectureApi
import de.miaowz.glossa.network.httpClient
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import timber.log.Timber

class GlossaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
        Timber.i("Glossa %s (%d) started", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    // Manual dependency container; created lazily so tests and previews don't open the database.
    private val database: GlossaDatabase by lazy { GlossaDatabase.create(this) }

    val grammarRepository: GrammarRepository by lazy {
        GrammarRepository(database.grammarDao(), database.taxonomyDao())
    }

    val vocabularyRepository: VocabularyRepository by lazy {
        VocabularyRepository(database.vocabularyDao(), database.taxonomyDao())
    }

    val officialCourseRepository: OfficialCourseRepository by lazy {
        OfficialCourseRepository(
            api = OfficialCourseApi(httpClient),
            dao = database.officialCourseDao()
        )
    }

    val officialCourseLectureRepository: OfficialCourseLectureRepository by lazy {
        OfficialCourseLectureRepository(
            api = OfficialCourseLectureApi(httpClient),
            dao = database.officialCourseLectureDao(),
        )
    }
}

/**
 * Keeps info, warnings and errors in release builds for troubleshooting (`adb logcat`); debug
 * details (requests, FSRS numbers) are dropped. Tags are the calling class, like in debug builds.
 */
private class ReleaseTree : Timber.DebugTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO
}
