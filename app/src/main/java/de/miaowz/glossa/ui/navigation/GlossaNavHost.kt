package de.miaowz.glossa.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.miaowz.glossa.ui.community.CommunityOverviewScreen
import de.miaowz.glossa.ui.community.CommunityScreenTarget
import de.miaowz.glossa.ui.community.impl.OfficialSetsScreen
import de.miaowz.glossa.ui.grammar.GrammarDetailScreen
import de.miaowz.glossa.ui.grammar.GrammarEditorScreen
import de.miaowz.glossa.ui.grammar.GrammarLibraryScreen
import de.miaowz.glossa.ui.home.HomeScreen
import de.miaowz.glossa.ui.review.ReviewScreen
import de.miaowz.glossa.ui.vocabulary.VocabularyDetailScreen
import de.miaowz.glossa.ui.vocabulary.VocabularyEditorScreen
import de.miaowz.glossa.ui.vocabulary.VocabularyLibraryScreen
import kotlinx.serialization.Serializable

private val TAG: String = "Glossa::GlossaNavHost"

@Serializable
data object HomeRoute

/** Vocabulary repetition session. */
@Serializable
data object ReviewRoute

@Serializable
data object GrammarLibraryRoute

@Serializable
data class GrammarDetailRoute(val entryId: Long)

/** [entryId] 0 creates a new entry. */
@Serializable
data class GrammarEditorRoute(val entryId: Long = 0L)

@Serializable
data object VocabularyLibraryRoute

@Serializable
data class VocabularyDetailRoute(val entryId: Long)

/** [entryId] 0 creates a new entry. */
@Serializable
data class VocabularyEditorRoute(val entryId: Long = 0L)

@Serializable
data object CommunityOverviewRoute

@Serializable
data object OfficialSetsRoute

/** Top-level destinations shown in the bottom navigation bar. */
private enum class TopLevelDestination(val route: Any, val label: String, val icon: ImageVector) {
    Home(HomeRoute, "Home", Icons.Filled.Home),
    Grammar(GrammarLibraryRoute, "Grammar", Icons.AutoMirrored.Filled.MenuBook),
    Vocabulary(VocabularyLibraryRoute, "Vocabulary", Icons.Filled.Translate),
    Community(CommunityOverviewRoute, "Community", Icons.Filled.Group),
}

@Composable
fun GlossaNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selected = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }

    // Each screen has its own Scaffold and handles system insets itself; this one only adds the
    // navigation bar, which is hidden on detail and editor screens.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (selected != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                        Log.i(TAG, "Built NavigationBarItem for ${destination.label}")
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<HomeRoute> {
                HomeScreen(onStartReview = { navController.navigate(ReviewRoute) })
            }
            composable<ReviewRoute> {
                ReviewScreen(onClose = { navController.popBackStack() })
            }

            composable<GrammarLibraryRoute> {
                GrammarLibraryScreen(
                    onAddEntry = { navController.navigate(GrammarEditorRoute()) },
                    onOpenEntry = { id -> navController.navigate(GrammarDetailRoute(id)) },
                )
            }
            composable<GrammarDetailRoute> {
                GrammarDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(GrammarEditorRoute(id)) },
                )
            }
            composable<GrammarEditorRoute> {
                GrammarEditorScreen(
                    onClose = { navController.popBackStack() },
                    onSaved = { id, wasNew ->
                        if (wasNew) {
                            navController.navigate(GrammarDetailRoute(id)) {
                                popUpTo<GrammarEditorRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

            composable<VocabularyLibraryRoute> {
                VocabularyLibraryScreen(
                    onAddEntry = { navController.navigate(VocabularyEditorRoute()) },
                    onOpenEntry = { id -> navController.navigate(VocabularyDetailRoute(id)) },
                )
            }
            composable<VocabularyDetailRoute> {
                VocabularyDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(VocabularyEditorRoute(id)) },
                )
            }
            composable<VocabularyEditorRoute> {
                VocabularyEditorScreen(
                    onClose = { navController.popBackStack() },
                    onSaved = { id, wasNew ->
                        if (wasNew) {
                            navController.navigate(VocabularyDetailRoute(id)) {
                                popUpTo<VocabularyEditorRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

            composable<CommunityOverviewRoute> {
                CommunityOverviewScreen(onButtonClick = { target ->
                    when (target) {
                        CommunityScreenTarget.OFFICIAL_SETS_MAIN_PAGE -> {
                            navController.navigate(OfficialSetsRoute) {}
                        }
                        CommunityScreenTarget.COMMUNITY_SETS_MAIN_PAGE -> {

                        }
                        else -> {
                            Log.e(TAG, "Invalid target provided: $target")
                        }
                    }
                })
            }
            composable<OfficialSetsRoute> {
                OfficialSetsScreen()
            }
        }
    }
}

/** Switches tabs, keeping one back stack entry per tab and restoring its state (scroll, filters). */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
