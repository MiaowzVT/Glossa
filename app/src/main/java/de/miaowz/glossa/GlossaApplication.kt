package de.miaowz.glossa

import android.app.Application
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.data.db.GlossaDatabase
import de.miaowz.glossa.network.OfficialCourseApi
import de.miaowz.glossa.network.OfficialCourseLectureApi
import de.miaowz.glossa.network.httpClient
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.network.repository.OfficialCourseRepository

class GlossaApplication : Application() {
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
