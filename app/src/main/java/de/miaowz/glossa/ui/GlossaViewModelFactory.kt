package de.miaowz.glossa.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import de.miaowz.glossa.GlossaApplication
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.network.repository.OfficialCourseRepository

private val CreationExtras.application: GlossaApplication
    get() = this[APPLICATION_KEY] as GlossaApplication

/** Access to app-wide dependencies from a `viewModel { ... }` initializer. */
val CreationExtras.grammarRepository: GrammarRepository
    get() = application.grammarRepository

val CreationExtras.vocabularyRepository: VocabularyRepository
    get() = application.vocabularyRepository

val CreationExtras.officialCourseRepository: OfficialCourseRepository
    get() = application.officialCourseRepository

val CreationExtras.officialCourseLectureRepository: OfficialCourseLectureRepository
    get() = application.officialCourseLectureRepository
