package com.example.navsdkdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.CustomRoutesOptions
import com.google.android.libraries.navigation.DisplayOptions
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RouteCalloutInfoFormat
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.Waypoint.UnsupportedPlaceIdException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var navView: NavigationView
    private var navigationManager: NavigationManager? = null

    private val mWaypoints = mutableListOf<Waypoint>()

    companion object {
        val DESTINATION_LATLNG = LatLng(41.38871, 2.13872)
        val DESTINATION_PLACEID = "ChIJw2Q7CVSvEmsR3sf73C6Qtw0"
        val startLocation = LatLng(41.38690, 2.13961)
        val routeToken =
            "CskCCtwBMtkBGr0BCjcCFhJviiokrs4FrQTCBBtmjpEH4ZEHysMCDLu4mgS1nJwE7LxMl4ZRsqJQg7QKnaogqLkBnmUAEkSKs01ozcowdA340nrBXCM_2Nd1Rq6Zd5aeWFe-kkpWwo6_kdriksXjwlS9-P7RHqx_BFDuIfWn8SrT4la4z0jfkgV3YRoSAAECAm4DBgjAAQkAKAMVDAIEKhEdAg0PCgsNEBBoBHQAFRoRDDIBAj35M6M-RVHrAT9I1ZDR76nz2OFlIhc1UElkYVliVkNaR0FpdllQcWMtbTBRRRAFGk8KTQoYCg0KAggBEQAAAAAAgGZAEXNoke18opBAEhIIABADEAYQExASGAJCBBoCCAUiGwoXNVBJZGFkV1pDWkdBaXZZUHFjLW0wUUVwASgBIhUA7gqEs75uqdtSKAd7NmQcDUngTIg"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        navView = findViewById(R.id.navigation_view)
        navView.onCreate(savedInstanceState)
        navView.setCalloutInfoFormatOverride(RouteCalloutInfoFormat.TIME)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigation_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /** ---------- Ask for the permissions ----------- **/
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        if (permissions.any { !checkPermissionGranted(it) }) {
            if (permissions.any { shouldShowRequestPermissionRationale(it) }) {
                // Display a dialogue explaining the required permissions.
            }

            val permissionsLauncher =
                registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { permissionResults ->
                    if (permissionResults.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            false
                        )
                    ) {
                        onLocationPermissionGranted()
                    } else {
                        finish()
                    }
                }
            permissionsLauncher.launch(permissions)
        } else {
            android.os.Handler(Looper.getMainLooper())
                .postDelayed({ onLocationPermissionGranted() }, TimeUnit.SECONDS.toMillis(2))
        }
    }

    private fun checkPermissionGranted(permissionToCheck: String): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            permissionToCheck
        ) == PackageManager.PERMISSION_GRANTED

    private fun onLocationPermissionGranted() {
        initializeNavigationApi()
    }

    private fun initializeNavigationApi() {
        NavigationApi.getNavigator(
            this,
            object : NavigationApi.NavigatorListener {
                @SuppressLint("MissingPermission")
                override fun onNavigatorReady(navigator: Navigator) {
                    navigationManager = NavigationManager(this@MainActivity, navigator)

                    navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)
                    navigator.simulator.setUserLocation(startLocation)

                    navView.getMapAsync { googleMap ->
                        googleMap.followMyLocation(GoogleMap.CameraPerspective.TILTED)
                    }

                    navigateToPlace()
                }

                override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                    when (errorCode) {
                        NavigationApi.ErrorCode.NOT_AUTHORIZED -> {
                            showToast(
                                "Error : Your API key is " +
                                        "invalid or not authorized to use Navigation."
                            )
                        }

                        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> {
                            showToast(
                                "Error: User did not " +
                                        "accept the Navigation Terms of Use."
                            )
                        }

                        else -> showToast("Error loading the Navigation SDK: $errorCode")
                    }
                }
            },
        )
    }

    private fun navigateToPlace() {
        /**
         * @param routingStrategy The strategy to use (e.g. SHORTER or DEFAULT_BEST).
         * @param avoidFerries True to avoid routes with ferries, false to include them.
         * @param travelMode The mode of transportation for the route (e.g TravelMode.DRIVING or WALKING)
         */

        // 1. Define common options (required for all scenarios)
        val routingOptions = RoutingOptions()
            .travelMode(RoutingOptions.TravelMode.DRIVING)
            .routingStrategy(RoutingOptions.RoutingStrategy.SHORTER)
            .avoidFerries(true)

        // 2. Define CustomRoutesOptions (used only for planned route)
        val customRoutesOptions: CustomRoutesOptions = CustomRoutesOptions.builder()
            .setRouteToken(routeToken)
            .setTravelMode(CustomRoutesOptions.TravelMode.DRIVING)
            .build()

        // 3. Define a simple DisplayOptions
        val displayOptions =
            DisplayOptions().showTrafficLights(true).showStopSigns(true)


        // 4. Choose ONE scenario
        // Note: Uncomment ONLY one of the function calls below to demonstrate that specific scenario.

        /** ---------- Navigate to a single-destination route  ----------- **/

        fun startSingleDestinationNavigation(
            destination: LatLng, // LatLng or String
            routingOptions: RoutingOptions,
            displayOptions: DisplayOptions
        ) {

            /**
             * HERE you can set a LatLng value with
             * .setLatLng(DESTINATION_LATLNG.latitude, DESTINATION_LATLNG.longitude)
             * instead of
             * .setPlaceIdString(DESTINATION_PLACEID)
             * and change the parameter destination: String for
             * destination: LatLng
             **/

            val destination: Waypoint = try {
               // In case you want to use LatLng values
                val lat = DESTINATION_LATLNG.latitude
                val lng = DESTINATION_LATLNG.longitude

                Waypoint.builder()
                    .setLatLng(lat, lng) // setLatLng or setPlaceId
                    //    .setPreferSameSideOfRoad(true)
                    //    .setPreferredHeading(230)
                    //    .setVehicleStopover(true)
                    .setPreferSameSideOfRoad(true)
                    .build()
            } catch (e: UnsupportedPlaceIdException) {
                showToast("Error: Place ID was unsupported.")
                return
            }
            navigationManager?.startSingleDestinationNavigation(
                destination,
                routingOptions,
                displayOptions
            )
        }

        startSingleDestinationNavigation(DESTINATION_LATLNG, routingOptions, displayOptions)


        /** ---------- Navigate to multiple waypoints ----------- **/

        fun startMultiWaypointNavigation(
            mWaypoints: MutableList<Waypoint>,
            routingOptions: RoutingOptions,
            displayOptions: DisplayOptions,
        ) {
            mWaypoints.clear()
            navigationManager?.createWaypoint(
                mWaypoints,
                "ChIJw2Q7CVSvEmsR3sf73C6Qtw0",
                "Sydney Star"
            )
            navigationManager?.createWaypoint(
                mWaypoints,
                "ChIJ3S-JXmauEmsRUcIaWtf4MzE",
                "Sydney Opera House"
            )
            navigationManager?.createWaypoint(
                mWaypoints,
                "ChIJ_Zm6E2muEmsRHnEV3HnFoy8",
                "Sydney Conservatorium"
            )
            if (mWaypoints.isNotEmpty()) {
                navigationManager?.startMultiWaypointNavigation(
                    mWaypoints,
                    routingOptions,
                    displayOptions
                )
            } else {
                navigationManager?.showToast("No destinations added")
            }
        }

      //  startMultiWaypointNavigation(mWaypoints, routingOptions, displayOptions)


        /** ---------- Navigate to a planned Route with a TOKEN  ----------- **/

        fun startTokenNavigation(
            mWaypoints: MutableList<Waypoint>,
            customRoutesOptions: CustomRoutesOptions,
            displayOptions: DisplayOptions
        ) {
            mWaypoints.clear() // Always start fresh

            // NOTE: When using a route token, the waypoint list must match the one used
            // to generate the token.

            val finalDestination: Waypoint = try {
                Waypoint.builder().setPlaceIdString(DESTINATION_PLACEID).build()
            } catch (e: UnsupportedPlaceIdException) {
                showToast("Place ID was unsupported.")
                return
            }
            mWaypoints.add(finalDestination)

            if (mWaypoints.isNotEmpty()) {
                navigationManager?.startTokenNavigation(
                    mWaypoints,
                    customRoutesOptions,
                    displayOptions
                )
            } else {
                navigationManager?.showToast("Final destination required for token route")
            }
        }

      //  startTokenNavigation(mWaypoints, customRoutesOptions, displayOptions)

    }


    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        navView.onSaveInstanceState(savedInstanceState)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        navView.onTrimMemory(level)
    }

    override fun onStart() {
        super.onStart()
        navView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navView.onResume()
    }

    override fun onPause() {
        navView.onPause()
        super.onPause()
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        super.onConfigurationChanged(configuration)
        navView.onConfigurationChanged(configuration)
    }

    override fun onStop() {
        navView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        navView.onDestroy()
        navigationManager?.cleanup()
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

}

