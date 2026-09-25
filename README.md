# Clima-Tact

Fresh Android Studio weather and climate visualizer.

## Open in Android Studio

Clone the repository:

    git clone https://github.com/ayushkumar72555-code/Clima-Tact.git

Then open the **Clima-Tact repository root**, not the app folder:

    C:\\Users\\ayush\\AndroidStudioProjects\\Clima-Tact

Android Studio should detect settings.gradle.kts and import the :app module automatically. After opening the root project, use **File → Sync Project with Gradle Files**.

If Android Studio reports that com.android.application has no version, the app folder was likely opened as a standalone Gradle project. Close it and reopen the repository root.

## Current build

Clean-room Kotlin + Jetpack Compose Android app with Weather, Atmosphere and Climate screens. Current weather values are prototype data until a live weather service is connected.

## Project structure

    Clima-Tact/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    └── app/
        ├── build.gradle.kts
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/climatact/app/MainActivity.kt
            └── res/values/styles.xml
