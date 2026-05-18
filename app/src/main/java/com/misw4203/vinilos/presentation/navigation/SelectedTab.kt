package com.misw4203.vinilos.presentation.navigation

import com.misw4203.vinilos.presentation.ui.components.VinilosDestination

/**
 * Maps the current nav route to the bottom-nav tab that should appear selected.
 *
 * Pure JVM function (no Android deps) so it is unit-testable. The mapping is the
 * verbatim `when` that previously lived inline in [VinilosNavHost]; behavior is
 * unchanged — any route not matched by an Artists/Collectors/Prizes rule (incl.
 * `null` and all album routes) falls back to [VinilosDestination.Albums].
 */
fun selectedTab(currentRoute: String?): VinilosDestination = when {
    currentRoute == Destinations.ArtistList ||
        currentRoute?.startsWith(Destinations.ArtistRoutePrefix) == true ||
        currentRoute?.startsWith(Destinations.BandRoutePrefix) == true ||
        currentRoute?.startsWith(Destinations.MusicianRoutePrefix) == true -> VinilosDestination.Artists
    currentRoute == Destinations.Collectors ||
        currentRoute?.startsWith(Destinations.CollectorRoutePrefix) == true -> VinilosDestination.Collectors
    currentRoute == Destinations.Prizes || currentRoute == Destinations.CreatePrize -> VinilosDestination.Prizes
    else -> VinilosDestination.Albums
}
