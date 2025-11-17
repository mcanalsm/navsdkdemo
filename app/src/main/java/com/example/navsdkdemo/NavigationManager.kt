package com.example.navsdkdemo

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.Waypoint.UnsupportedPlaceIdException


/**
 * Manages interactions with the Navigation SDK.
 * @param context The application context.
 * @param navigator The Navigator instance for controlling navigation.
 */

class NavigationManager(
    private val context: Context,
    private val navigator: Navigator
) {

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

    /**
     * In case you want to pass multiple waypoints:
     * Replace Waypoint for List<Waypoint>
     * Replace setDestination for setDestinations
     */

    fun startNavigation(
        destinations: List<Waypoint> ,
        routingOptions: RoutingOptions
    ) {

        val pendingRoute = navigator.setDestinations(destinations, routingOptions)

        pendingRoute.setOnResultListener { code ->
            when (code) {
                Navigator.RouteStatus.OK -> {
                    navigator.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                    navigator.simulator.simulateLocationsAlongExistingRoute(
                        SimulationOptions().speedMultiplier(5f)
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
     * Cleans up resources and removes listeners to prevent memory leaks.
     * Should be called in onDestroy.
     */

    fun cleanup() {
        navigator.removeArrivalListener(arrivalListener)
        navigator.removeRouteChangedListener(routeChangedListener)
        navigator.simulator.unsetUserLocation()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun routeStatusToString(code: Navigator.RouteStatus?): String {
        return when (code) {
            Navigator.RouteStatus.ROUTE_CANCELED -> "Route Canceled"
            Navigator.RouteStatus.NO_ROUTE_FOUND -> "No Route Found"
            Navigator.RouteStatus.NETWORK_ERROR -> "Network Error"
            Navigator.RouteStatus.QUOTA_CHECK_FAILED -> "Quota Exceeded"
            Navigator.RouteStatus.LOCATION_DISABLED -> "Location Disabled"
            Navigator.RouteStatus.LOCATION_UNKNOWN -> "Location Unknown"
            Navigator.RouteStatus.WAYPOINT_ERROR -> "Waypoint Error"

            else -> "Unknown Error ($code)"
        }
    }

    fun createWaypoint(mWaypoints: MutableList<Waypoint>,placeId: String, title: String?) {
        try {
            mWaypoints.add(
                Waypoint.builder()
                    .setPlaceIdString(placeId)
                    .setTitle(title)
                    .build()
            )

        } catch (e: UnsupportedPlaceIdException) {
            Log.e("NavigationManager", "Failed to create waypoint for PlaceID: $placeId", e)
            showToast("Unsupported PlaceID: $placeId")
    } }
}
