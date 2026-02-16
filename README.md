# Navigation SDK for Android Demo

This repository contains a sample application demonstrating the core features of the Navigation SDK for Android. It serves as a practical guide and starting point for developers looking to integrate Google Maps turn-by-turn navigation into their applications.

This demo is based on the official Google documentation.

## Features

*   **Full-Screen Navigation:** Demonstrates the use of `NavigationView` for a complete, out-of-the-box navigation experience.
*   **Runtime Permissions:** Handles requesting the `ACCESS_FINE_LOCATION` permission required for navigation.
*   **UI Customization:**
    *   **Night Mode:** Programmatically sets the UI to `FORCE_DAY` mode.
    *   **Custom Controls:** Adds custom buttons to the UI to show the route overview and display a directions list.
    *   **Styling:** Includes a commented-out example of `StylingOptions` to customize colors, fonts, and text sizes of the navigation header.
*   **Directions List:** Shows how to integrate and display `DirectionsListView`.
*   **Map Interaction:**
    *   Enables the display of the traffic layer.
    *   Adds custom markers to the map.
*   **Navigation Simulator:** Uses the built-in simulator to start navigation from a predefined location for easy testing.

## Getting Started

### 1. Prerequisites
- Android Studio (latest stable version recommended).
- An API key from the Google Cloud Platform with the **Navigation SDK** enabled for your project's package name.

### 2. Follow the Nav SDK for Android Playbook
Follow the instructions in the Set up a Navigation SDK demo app  in the
[Navigation SDK Playbook](https://docs.google.com/document/d/1xS5ktFoWbmtNOaDZW_tsa_qj_PDgL-Gj_LoINjUS58I/edit?usp=sharing&resourcekey=0-Chx0JYD7R8ypTWxgQJCgog)

### 3. Add Your API Key
This project uses the [Secrets Gradle Plugin for Android](https://github.com/google/secrets-gradle-plugin) to securely manage the API key.

1.  In the `app` module, create a new file named `secrets.properties`.
2.  Add your API key to this file:
    ```properties
    MAPS_API_KEY="YOUR_API_KEY"
    ```
    (Replace `YOUR_API_KEY` with your actual key).

The project is already configured to use this key in the `AndroidManifest.xml`.

### 4. Build and Run
Open the project in Android Studio, allow Gradle to sync the dependencies, and run the `app` configuration on an emulator or physical device.

## Configuration & Customization

*   **Destination:** The starting location and destination are hardcoded in the `MainActivity.kt` companion object. You can change the `DESTINATION_PLACEID` and `startLocation` constants to test different routes.
*   **UI Styling:** The `setupNavigationUiSettings()` method in `MainActivity.kt` contains a commented-out `StylingOptions` block. You can uncomment and modify this to see how to customize the look and feel of the navigation UI.
*   **Layouts:** The custom control buttons are defined in `app/src/main/res/layout/custom_control.xml`, and the main activity layout is in `app/src/main/res/layout/activity_main.xml`.
