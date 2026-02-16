package com.example.navsdkdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.AlternateRoutesStrategy
import com.google.android.libraries.navigation.CustomControlPosition
import com.google.android.libraries.navigation.DirectionsListView
import com.google.android.libraries.navigation.DisplayOptions
import com.google.android.libraries.navigation.ForceNightMode.FORCE_DAY
import com.google.android.libraries.navigation.ForceNightMode.FORCE_NIGHT
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.StylingOptions
import com.google.android.libraries.navigation.Waypoint
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private var mNavigator: Navigator? = null

    // Reference to the Navigation View
    private lateinit var navView: NavigationView

    // Reference to our helper Manager
    private var navigationManager: NavigationManager? = null

    // UI Elements
    private var mDirectionsListView: DirectionsListView? = null

    // Global options so we can pass them to the Manager
    private var mDisplayOptions: DisplayOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        navView = findViewById(R.id.navigation_view)
        navView.onCreate(savedInstanceState)

        // --- For custom UI navigation settings ---
        setupNavigationUiSettings()

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

    private fun setupNavigationUiSettings() {

        // --- Night Mode Settings ---
        // Change between AUTO, FORCE_DAY or FORCE_NIGHT
        navView.setForceNightMode(FORCE_DAY)

        // --- Styling (Commented out for Playground usage) ---
/*
        val myStyle = StylingOptions().apply {
            primaryDayModeThemeColor(0xff1A73E8.toInt())
            secondaryDayModeThemeColor(0xff1557B0.toInt())
            primaryNightModeThemeColor(0xff202124.toInt())
            secondaryNightModeThemeColor(0xff303134.toInt())
            headerLargeManeuverIconColor(0xffFFFFFF.toInt())
            headerSmallManeuverIconColor(0xffFFFFFF.toInt())
            headerGuidanceRecommendedLaneColor(0xffFFFFFF.toInt())
            headerInstructionsTypefacePath("/system/fonts/NotoSerif-BoldItalic.ttf")
            headerInstructionsTextColor(0xffFFFFFF.toInt())
            headerInstructionsFirstRowTextSize(24f)
            headerInstructionsSecondRowTextSize(20f)
            headerDistanceTypefacePath("/system/fonts/NotoSerif-Italic.ttf")
            headerDistanceValueTextColor(0xffFFFFFF.toInt())
            headerDistanceValueTextSize(20f)
            headerDistanceUnitsTextColor(0xffD2E3FC.toInt())
            headerDistanceUnitsTextSize(18f)
            headerNextStepTypefacePath("/system/fonts/NotoSerif-BoldItalic.ttf")
            headerNextStepTextColor(0xffD2E3FC.toInt())
            headerNextStepTextSize(20f)
        }
        navView.setStylingOptions(myStyle)
*/

        // --- SHOW DIRECTION'S LIST + CUSTOM CONTROLS ---
        val listContainer = findViewById(R.id.directions_list_container) as FrameLayout
        val closeButton = findViewById(R.id.close_directions_button) as Button
        val customControlView = layoutInflater.inflate(R.layout.custom_control, null)

        // Find the buttons inside the custom view
        // Note: Make sure custom_control.xml has these IDs

        val showListButton = customControlView.findViewById<Button>(R.id.btn_show_list)
        val overviewButton = customControlView.findViewById<Button>(R.id.btn_overview)

        // Setup Directions List
        mDirectionsListView = DirectionsListView(this)
        listContainer.addView(mDirectionsListView, 0)

        // Initialize List Lifecycle
        mDirectionsListView?.onCreate(Bundle())
        mDirectionsListView?.onStart()
        mDirectionsListView?.onResume()

        // ADD CUSTOM CONTROL

        navView.setCustomControl(
            customControlView,
            CustomControlPosition.BOTTOM_START_BELOW
        )

        // CLICK LISTENERS
        showListButton.setOnClickListener {
            listContainer.visibility = View.VISIBLE
        }

        // This button might be null if you are using the single-button XML.
        // We use safe call ?. just in case.
        overviewButton?.setOnClickListener {
            navView.showRouteOverview()
        }

        closeButton.setOnClickListener {
            listContainer.visibility = View.GONE
        }

        listContainer.visibility = View.GONE

        // --- Visual Elements ---
        navView.setTripProgressBarEnabled(true)
        navView.setSpeedometerEnabled(true)
        navView.setSpeedLimitIconEnabled(true)

        // --- Map Layer (Traffic & Markers) ---
        navView.getMapAsync { googleMap ->
            googleMap.isTrafficEnabled = true
            googleMap.clear()

            // NOTE: We moved followMyLocation() to onNavigatorReady to prevent crashes.

            // --- Add Markers ---
            val standardMarkerLoc = LatLng(-37.67, 144.85)
            googleMap.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(standardMarkerLoc)
                    .title("Standard Marker")
            )

            val customMarkerLoc = LatLng(-37.672, 144.855)
            val blueIcon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)

            val myCustomMarker = googleMap.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(customMarkerLoc)
                    .title("Floating Text Here!")
                    .snippet("This is a custom blue pin")
                    .icon(blueIcon)
                    .draggable(true)
            )
            myCustomMarker?.showInfoWindow()
        }

        // --- Display Options (Global) ---
        mDisplayOptions = DisplayOptions()
            .hideDestinationMarkers(false)
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

                    // 1. Setup Manager and References
                    mNavigator = navigator
                    navigationManager = NavigationManager(this@MainActivity, navigator)

                    // 2. Essential Configuration
                    navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)
                    navigator.simulator.setUserLocation(startLocation)

                    // 3. Setup Camera (CRITICAL FIX: Call this here, not in setup UI)
                    // Set the camera: TOP_DOWN_HEADING_UP, TOP_DOWN_NORTH_UP, TILTED
                    navView.getMapAsync { googleMap ->
                        googleMap.followMyLocation(GoogleMap.CameraPerspective.TILTED)
                    }

                    // 4. Define Routing Options
                    val routingOptions = RoutingOptions()
                        .travelMode(RoutingOptions.TravelMode.DRIVING)
                        .alternateRoutesStrategy(AlternateRoutesStrategy.SHOW_NONE)


                    // 5. Start Navigation via Manager
                    // We use the Waypoint helper from the manager or build it here.
                    // For the demo, let's build it here safely.

                    try {
                        val destination = Waypoint.builder().setPlaceIdString(DESTINATION_PLACEID).build()

                        navigationManager?.startSingleDestinationNavigation(
                            destination,
                            routingOptions,
                            mDisplayOptions
                        )
                    } catch (e: Waypoint.UnsupportedPlaceIdException) {
                        showToast("Place ID was unsupported.")
                    }
                }

                override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                    when (errorCode) {
                        NavigationApi.ErrorCode.NOT_AUTHORIZED -> {
                            showToast("Error: Your API key is invalid or not authorized.")
                        }
                        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> {
                            showToast("Error: User did not accept Terms of Use.")
                        }
                        else -> showToast("Error loading Navigation API: $errorCode")
                    }
                }
            },
        )
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
        // 1. Clean up UI elements
        navView.onDestroy()
        mDirectionsListView?.onPause()
        mDirectionsListView?.onStop()
        mDirectionsListView?.onDestroy()

        // 2. Clean up Logic via Manager
        navigationManager?.cleanup()

        // 3. Final SDK Teardown
        mNavigator?.cleanup()
        mNavigator = null

        super.onDestroy()
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        val DESTINATION_LATLNG = LatLng(-37.667971, 144.849707)
        val DESTINATION_PLACEID = "ChIJGT5P9L1Z1moReHsUe9EXdxY"
        val startLocation = LatLng( -37.726148, 144.886216)
    }
}