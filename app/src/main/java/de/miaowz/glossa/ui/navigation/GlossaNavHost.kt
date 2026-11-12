package de.miaowz.glossa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.miaowz.glossa.ui.detail.GrammarDetailScreen
import de.miaowz.glossa.ui.editor.GrammarEditorScreen
import de.miaowz.glossa.ui.library.GrammarLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data class EntryDetailRoute(val entryId: Long)

/** [entryId] 0 creates a new entry. */
@Serializable
data class EntryEditorRoute(val entryId: Long = 0L)

@Composable
fun GlossaNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = LibraryRoute, modifier = modifier) {
        composable<LibraryRoute> {
            GrammarLibraryScreen(
                onAddEntry = { navController.navigate(EntryEditorRoute()) },
                onOpenEntry = { id -> navController.navigate(EntryDetailRoute(id)) },
            )
        }
        composable<EntryDetailRoute> {
            GrammarDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(EntryEditorRoute(id)) },
            )
        }
        composable<EntryEditorRoute> {
            GrammarEditorScreen(
                onClose = { navController.popBackStack() },
                onSaved = { id, wasNew ->
                    if (wasNew) {
                        navController.navigate(EntryDetailRoute(id)) {
                            popUpTo<EntryEditorRoute> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}
