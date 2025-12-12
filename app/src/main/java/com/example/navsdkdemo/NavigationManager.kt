package com.example.navsdkdemo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.libraries.navigation.CustomRoutesOptions
import com.google.android.libraries.navigation.DisplayOptions
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.Waypoint.UnsupportedPlaceIdException

/**
 * Manages interactions with the Navigation SDK.
 * Handles: Route calculation, Guidance start, and Cleanup.
 */
class NavigationManager(
    private val context: Context,
    private val navigator: Navigator
) {

    // --- Listeners ---
    private val arrivalListener = Navigator.ArrivalListener { arrivalEvent ->
        showToast("onArrival: User has arrived")
        if (arrivalEvent.isFinalDestination) {
            navigator.simulator.unsetUserLocation()
            navigator.clearDestinations()
        } else {
            navigator.continueToNextDestination()
        }
    }

    private val routeChangedListener = Navigator.RouteChangedListener {
        showToast("onRouteChanged: The driver's route changed")
    }

    init {
        registerListeners()
    }

    private fun registerListeners() {
        navigator.addArrivalListener(arrivalListener)
        navigator.addRouteChangedListener(routeChangedListener)
    }

    // --- Navigation Scenario Methods ---

    /**
     * Scenario 1: Starts navigation to a single destination.
     */
    fun startSingleDestinationNavigation(
        destination: Waypoint,
        routingOptions: RoutingOptions? = null,
        displayOptions: DisplayOptions? = null
    ) {
        val pendingRoute = navigator.setDestination(
            destination,
            routingOptions,
            displayOptions
        )
        handlePendingRouteResult(pendingRoute)
    }

    /**
     * Scenario 2: Starts navigation through multiple waypoints.
     */
    fun startMultiWaypointNavigation(
        destinations: List<Waypoint>,
        routingOptions: RoutingOptions? = null,
        displayOptions: DisplayOptions? = null,
    ) {
        if (destinations.isEmpty()) {
            showToast("No destinations provided")
            return
        }

        val pendingRoute = navigator.setDestinations(
            destinations,
            routingOptions,
            displayOptions
        )
        handlePendingRouteResult(pendingRoute)
    }

    /**
     * Scenario 3: Starts navigation using a pre-planned route token.
     */
    fun startTokenNavigation(
        waypoints: List<Waypoint>,
        customRoutesOptions: CustomRoutesOptions,
        displayOptions: DisplayOptions? = null
    ) {
        val pendingRoute = navigator.setDestinations(
            waypoints,
            customRoutesOptions,
            displayOptions
        )
        handlePendingRouteResult(pendingRoute)
    }

    // --- Utility Functions ---

    private fun handlePendingRouteResult(pendingRoute: ListenableResultFuture<Navigator.RouteStatus>) {
        pendingRoute.setOnResultListener { code ->
            when (code) {
                Navigator.RouteStatus.OK -> {
                    navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                    navigator.simulator.simulateLocationsAlongExistingRoute(
                        SimulationOptions().speedMultiplier(1f)
                    )
                    navigator.startGuidance()
                }
                else -> {
                    val errorReason = routeStatusToString(code)
                    showToast("Error starting guidance: $errorReason")
                }
            }
        }
    }

    /**
     * HELPER: Creates a Waypoint safely and adds it to the list.
     * Handles the UnsupportedPlaceIdException so the Activity doesn't have to.
     */
    fun createWaypoint(listToAddTo: MutableList<Waypoint>, placeId: String, title: String?) {
        try {
            val wp = Waypoint.builder()
                .setPlaceIdString(placeId)
                .setTitle(title)
                .build()

            listToAddTo.add(wp)

        } catch (e: UnsupportedPlaceIdException) {
            Log.e("NavManager", "Invalid Place ID: $placeId", e)
            showToast("Error: Place ID $placeId is not supported")
        }
    }

    /**
     * Cleans up resources to prevent memory leaks.
     */
    fun cleanup() {
        navigator.stopGuidance()
        navigator.clearDestinations()
        navigator.removeArrivalListener(arrivalListener)
        navigator.removeRouteChangedListener(routeChangedListener)
        navigator.simulator.unsetUserLocation()
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun routeStatusToString(code: Navigator.RouteStatus?): String {
        return when (code) {
            Navigator.RouteStatus.ROUTE_CANCELED -> "Route Canceled"
            Navigator.RouteStatus.NO_ROUTE_FOUND -> "No Route Found"
            Navigator.RouteStatus.NETWORK_ERROR -> "Network Error"
            Navigator.RouteStatus.QUOTA_CHECK_FAILED -> "Quota Exceeded"
            Navigator.RouteStatus.LOCATION_DISABLED -> "Location Disabled"
            else -> "Unknown Error ($code)"
        }
    }
}