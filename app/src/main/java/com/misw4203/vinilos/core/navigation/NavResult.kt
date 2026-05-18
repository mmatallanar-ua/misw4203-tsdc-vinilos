package com.misw4203.vinilos.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Single source of truth for the "return a refresh signal to the previous
 * screen" navigation pattern. Replaces the ~10 hand-rolled
 * `previousBackStackEntry?.savedStateHandle?.set(key, true); popBackStack()` /
 * `getStateFlow(key, false)` duplications in `VinilosNavHost`.
 *
 * The refresh-key constants intentionally remain in
 * [com.misw4203.vinilos.presentation.navigation.Destinations]; only the
 * boilerplate is extracted here. Behaviour is a literal 1:1 extraction — when
 * and what is set/cleared/popped is unchanged.
 */

/** Marks [key] as "needs refresh" on this handle. */
fun SavedStateHandle.setRefresh(key: String) {
    this[key] = true
}

/** Clears the refresh flag for [key] on this handle. */
fun SavedStateHandle.clearRefresh(key: String) {
    this[key] = false
}

/**
 * Sets the refresh flag for [key] on the previous back stack entry, then pops.
 * 1:1 replacement for `previousBackStackEntry?.savedStateHandle?.set(key, true)
 * ; popBackStack()`.
 */
fun NavController.popWithRefresh(key: String) {
    previousBackStackEntry?.savedStateHandle?.setRefresh(key)
    popBackStack()
}

/**
 * Observes the refresh flag for [key] as Compose [State], defaulting to
 * `false`. 1:1 replacement for
 * `savedStateHandle.getStateFlow(key, false).collectAsStateWithLifecycle()`.
 */
@Composable
fun NavBackStackEntry.rememberRefreshFlag(key: String): State<Boolean> =
    savedStateHandle.getStateFlow(key, false).collectAsStateWithLifecycle()

/** Clears the refresh flag for [key] on this entry's handle. */
fun NavBackStackEntry.clearRefresh(key: String) {
    savedStateHandle.clearRefresh(key)
}

/**
 * Suspending collector of the refresh flag for [key], used inside an existing
 * `LaunchedEffect`. On each `true` it clears the flag first (so rotation does
 * not re-fire) then invokes [onResult]. Encapsulates the read side of the
 * bespoke `track_added` consumer while preserving its retry + snackbar
 * semantics exactly.
 */
suspend fun NavBackStackEntry.collectRefresh(key: String, onResult: suspend () -> Unit) {
    savedStateHandle.getStateFlow(key, false).collect { signalled ->
        if (signalled) {
            savedStateHandle.clearRefresh(key)
            onResult()
        }
    }
}
