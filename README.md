<h1 align="center">Geofences</h1> 

<p align="center">
This app was created to learn more about Android maps, geofencing and the use of sonarqube with jacoco for unit tests and coverage reports.

A Kodeco tutorial was used as a reference to learn about the use of GeoFences in Android. More information in the following link: [Kodeco Geofencing](https://www.kodeco.com/7372-geofencing-api-tutorial-for-android)
</p>

## Installation

Clone this repository and import into **Android Studio**

```bash
git clone https://github.com/munbonecci/Geofences.git
```

## Build variants

Use the Android Studio *Build Variants* button to choose between **debug** and **release** flavors

## Maintainers

This project is maintained by:

* [Edmundo Bonequi](http://github.com/munbonecci)

## Built with

- [Kotlin](https://kotlinlang.org/) - For coding.
- [Maps](https://developers.google.com/maps/documentation/android-sdk/start) - Maps.
- [Geofencing](https://developer.android.com/training/location/geofencing) - Geofencing.
- [Junit](https://developer.android.com/training/testing/junit-runner?hl=es-419) - For unit tests.
- [Sonarqube](https://docs.sonarqube.org/latest/) -Self-managed, automatic code review tool that systematically helps you deliver clean code.
- [Jacoco](https://www.jacoco.org) -For coverage reports.


## How I run the app?

- Clone the repository
- Open it in Android Studio
- Wait until dependencies are installed
- If you experience problems when compiling the project you may need to change your JDK to Corretto 11
- Run app in your emulator or physical device

## How I run the coverage test in Sonarqube?

- Make sure you are running Sonarqube locally or somewhere online
- If you are running Sonarqube locally add your sonar.login and sonar.password from build.gradle(:app)
- From terminal in android studio preferably execute the following command: ./gradlew clean connectedAndroidTest test createDebugCoverageReport jacocoTestReport sonarqube
- When build finish open or refresh your Sonarqube from localhost or origin in your browser.