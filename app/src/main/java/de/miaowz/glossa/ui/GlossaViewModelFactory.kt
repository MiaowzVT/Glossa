package de.miaowz.glossa.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import de.miaowz.glossa.GlossaApplication
import de.miaowz.glossa.data.GrammarRepository

/** Access to app-wide dependencies from a `viewModel { ... }` initializer. */
val CreationExtras.grammarRepository: GrammarRepository
    get() = (this[APPLICATION_KEY] as GlossaApplication).grammarRepository
