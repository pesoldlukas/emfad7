# EMFAD Analyzer

An Android application for electromagnetic field analysis, designed to run on a Samsung Galaxy S21 Ultra. It reads, analyzes, visualizes, and optionally displays BLE measurements in Augmented Reality (AR). The goal is to create a technically robust and scientifically precise prototype.

## 🔧 Technology Stack

- **Programming Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Android API**: Minimum API 24 (Android 7.0), Target API 34
- **Bluetooth Communication**: Nordic BLE Library v2.6.1
- **Data Persistence**: Room Database
- **AI Analysis**: TensorFlow Lite
- **Visualization**: OpenGL ES 3.0 + ARCore with Sceneform
- **Build System**: Gradle + CI/CD with Codemagic

## 📱 Main Features

1.  **Real-time Data Acquisition**: Via BLE from the EMFAD UG12 DS WL device.
2.  **AI-powered Material Analysis**: (e.g., cluster and crystal structure recognition, conductivity, symmetry).
3.  **3D Visualization**: Using Jetpack Compose & OpenGL.
4.  **AR Representation**: Display of analyzed measurement data in space with Sceneform / ARCore.
5.  **Settings Menu**: For measurement frequency, mode, orientation, filter level, offset, gain.
6.  **Export Functions**: CSV, PDF, MATLAB-compatible.

## 📁 Project Structure

```
com.emfad.app/
├── bluetooth/              // BLE Manager
├── models/                // Data, Analysis, Profiles, AR Objects
├── services/              // Measurement Logic, Analysis, ARController
├── ui/                    // Compose UI (Main, Settings, Visualizer, ARScreen)
├── viewmodels/            // MVVM Logic
├── ar/                    // ARCore Support & Visualization
├── utils/                 // Configuration, Logging
├── database/              // Room Database components
├── ai/                    // TensorFlow Lite models and classifiers
└── MainActivity.kt
```

## 🧠 AR Integration (ARCore)

-   Initializes ARCore session.
-   Builds virtual objects (e.g., clusters, crystals, layers) in 3D from analysis results.
-   Places them via touch or automatically using Plane Detection.
-   Uses **Sceneform** or an alternative AR rendering system (e.g., OpenGL Fallback).

## 📤 Export & Storage

-   Save measurement data locally & optionally export.
-   Export formats: CSV, PDF, MATLAB.
-   Persistently save measurement sessions with timestamp & results.

## 🧪 Analysis Models

-   TensorFlow Lite Models
-   DBSCAN Classification
-   Symmetry Recognition
-   Impedance Analysis (Skin Effect)
-   Cavity Detection & Layering (for AR)

## 📄 Additional Files

-   `codemagic.yaml` for Build & Deploy
-   `AndroidManifest.xml` with AR & Bluetooth permissions
-   `proguard-rules.pro`
-   `.gitignore`, `LICENSE`, etc.

## 🎯 Goal

To create a complete Android Studio project with all `.kt` files, resources, manifests, and configurations, ready to run immediately on a Samsung Galaxy S21 Ultra. AR should be correctly integrated with ARCore. The app must also be fallback-capable without AR.