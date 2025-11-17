package com.example.navsdkdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Navigator.RouteStatus
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.Waypoint.UnsupportedPlaceIdException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var navView: NavigationView
    private var navigationManager: NavigationManager? = null

    private val mWaypoints = mutableListOf<Waypoint>()

    companion object {
        val DESTINATION_LATLNG = LatLng(41.38302344858377, 2.1881624610309394)
        val DESTINATION_PLACEID = "ChIJBVY2Bxh-j4ARa2zO8Jd6H2A"
        val startLocation = LatLng(-33.912182,151.259678)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        navView = findViewById(R.id.navigation_view)
        navView.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigation_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /** ---------- Ask for the permissions ----------- **/
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
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
                    if (permissionResults.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                        onLocationPermissionGranted()
                    } else {
                        finish()
                    }
                }
            permissionsLauncher.launch(permissions)
        } else {
            android.os.Handler(Looper.getMainLooper()).postDelayed({ onLocationPermissionGranted() }, TimeUnit.SECONDS.toMillis(2))
        }
    }

    private fun checkPermissionGranted(permissionToCheck: String): Boolean =
        ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED

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

        val routingOptions = RoutingOptions()
            .travelMode(RoutingOptions.TravelMode.DRIVING)
            .routingStrategy(RoutingOptions.RoutingStrategy.DEFAULT_BEST)
            .avoidFerries(true)

        //TODO add displayOptions



        /** ---------- Navigate to a single-destination route  ----------- **/

//        val waypoint: Waypoint =
//            try {
//                Waypoint.builder().setLatLng(DESTINATION_LATLNG.latitude, DESTINATION_LATLNG.longitude).build()
//            } catch (e: UnsupportedPlaceIdException) {
//                showToast("Place ID was unsupported.")
//                return
//            }

//        navigationManager?.startNavigation(waypoint, routingOptions)


        /** ---------- Navigate to multiple waypoints ----------- **/

        mWaypoints.clear()

        navigationManager?.createWaypoint(mWaypoints,"ChIJw2Q7CVSvEmsR3sf73C6Qtw0", "Sydney Star");
        navigationManager?.createWaypoint(mWaypoints,"ChIJ3S-JXmauEmsRUcIaWtf4MzE", "Sydney Opera House");
        navigationManager?.createWaypoint(mWaypoints,"ChIJ_Zm6E2muEmsRHnEV3HnFoy8", "Sydney Conservatorium of Music");

        if (mWaypoints.isNotEmpty()) {
            navigationManager?.startNavigation(mWaypoints, routingOptions)
        } else {
            showToast("No destinations added")
        }

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

