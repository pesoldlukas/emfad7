# EMFAD Backend Integration

## 🔌 Hardware-Backend Integration für EMFAD Android App

Diese Dokumentation beschreibt die vollständige Backend-Integration für die EMFAD Android App, die das Jetpack Compose Frontend mit echter EMFAD-Hardware verbindet.

## 📋 Übersicht der implementierten Services

### 1. Hardware-Kommunikation

#### `EMFADProtocol.kt`
- **Zweck**: Hardware-Kommunikationsprotokoll basierend auf reverse-engineered Code
- **Funktionen**:
  - Command-Erstellung für alle EMFAD-Funktionen
  - Datenparser für Geräte-Responses
  - Checksummen-Validierung
  - Packet-Strukturierung

```kotlin
// Beispiel: Frequenz-Command erstellen
val command = EMFADProtocol.createFrequencyCommand(EMFADFrequency.FREQ_19KHZ)
val response = deviceManager.sendCommand(EMFADCommand.SetFrequency(frequency))
```

#### `EMFADDeviceManager.kt`
- **Zweck**: Zentrale Geräteverwaltung für USB und Bluetooth
- **Funktionen**:
  - Geräte-Scanning und -Erkennung
  - USB/Bluetooth-Verbindungsmanagement
  - Command-Übertragung mit Timeout-Handling
  - Verbindungsstatus-Monitoring

```kotlin
// USB-Verbindung
val devices = deviceManager.scanForDevices()
val result = deviceManager.connectUSB(usbDevice)

// Bluetooth-Verbindung
val result = deviceManager.connectBluetooth(bluetoothDevice)
```

#### `EMFADConnection.kt`
- **Zweck**: Abstrakte Verbindungsschnittstelle
- **Implementierungen**:
  - `EMFADUSBConnection`: USB-spezifische Kommunikation
  - `EMFADBluetoothConnection`: BLE-Kommunikation mit GATT

### 2. Datenverarbeitung

#### `MeasurementService.kt` (Erweitert)
- **Zweck**: Echtzeitdatenerfassung und -verarbeitung
- **Neue Funktionen**:
  - Hardware-basierte Messungen
  - Konfigurationsübertragung an Gerät
  - Kontinuierliche Datenerfassung
  - Kalibrierungsfunktionen

```kotlin
// Echtzeitdaten-Stream
measurementService.startMeasurement(config).collect { measurement ->
    // Verarbeite Messdaten
}

// Einzelmessung
val measurement = measurementService.performStepMeasurement(config)
```

#### `SpectrumAnalyzer.kt`
- **Zweck**: Frequenzspektrum-Analyse basierend auf HzEMSoftexe.c
- **Algorithmen**:
  - Tiefenberechnung: `depth = -ln(signal / 1000.0) / 0.417`
  - Peak-Erkennung mit Hanning-Window
  - Materialklassifizierung
  - Signalmuster-Analyse

```kotlin
// Spektrum analysieren
val spectrum = spectrumAnalyzer.analyzeSpectrum(rawData, frequencies)
val depth = spectrumAnalyzer.calculateDepth(signalStrength, frequency)
val peaks = spectrumAnalyzer.detectPeaks(spectrum)
```

### 3. Export/Import

#### `ExportService.kt` (Erweitert)
- **Zweck**: Vollständige Datei-IO für EMFAD-Formate
- **Unterstützte Formate**:
  - `.EGD` - EMFAD Grid Data (Rasterdaten)
  - `.ESD` - EMFAD Spectrum Data (Spektrumdaten)
  - `.FADS` - EMFAD Analysis Data Set (Analyseergebnisse)
  - `.CSV` - Comma-separated values
  - `.PDF` - Berichte und Dokumentation

```kotlin
// Export
val result = exportService.exportAsEGD(profile, "measurement_001")
val result = exportService.exportAsESD(spectrum, "spectrum_001")

// Import
val profile = exportService.importEGD(uri)
val spectrum = exportService.importESD(uri)
```

### 4. GPS-Integration

#### `EMFADLocationService.kt`
- **Zweck**: GPS-basierte Messpunkt-Verfolgung
- **Funktionen**:
  - Kontinuierliche Standortaktualisierung
  - Messpunkt-Korrelation mit GPS-Koordinaten
  - GPX-Export für Tracking-Daten
  - Genauigkeits-Monitoring

```kotlin
// GPS-Tracking starten
locationService.startLocationUpdates().collect { location ->
    // Verarbeite GPS-Position
}

// Messpunkt mit GPS hinzufügen
locationService.addMeasurementPoint(measurement)
```

## 🔧 Integration mit Frontend

### ViewModels-Erweiterung

#### `MeasurementViewModel.kt`
```kotlin
// Echte Hardware-Verbindung
fun connectUSB() {
    val devices = deviceManager.scanForDevices()
    val result = deviceManager.connectUSB(usbDevice)
}

// Hardware-basierte Messung
fun performStepMeasurement() {
    val measurement = measurementService.performStepMeasurement(config)
    locationService.addMeasurementPoint(measurement)
}
```

#### `AnalysisViewModel.kt`
```kotlin
// Profile-Export
fun exportAsEGD() {
    exportService.exportAsFormat(profile, EMFADExportFormat.EGD)
}
```

#### `MapViewModel.kt`
```kotlin
// GPS-Tracking
fun toggleTracking() {
    locationService.startTracking()
}
```

## 🧪 Testing

### Hardware-Tests

#### `EMFADDeviceManagerTest.kt`
- Geräte-Scanning
- Protokoll-Validierung
- Verbindungsmanagement
- Error-Handling
- Memory-Management

```bash
# Tests ausführen
./gradlew connectedAndroidTest
```

### Integration-Tests
- USB-Kommunikation mit echten Geräten
- Bluetooth-Pairing und Datenübertragung
- Protokoll-Kompatibilität
- Performance-Benchmarks

## 📦 Dependency Injection

### `ServiceModule.kt`
```kotlin
@Provides
@Singleton
fun provideEMFADDeviceManager(context: Context, bluetoothAdapter: BluetoothAdapter): EMFADDeviceManager

@Provides
@Singleton
fun provideSpectrumAnalyzer(): SpectrumAnalyzer

@Provides
@Singleton
fun provideEMFADLocationService(context: Context, fusedLocationClient: FusedLocationProviderClient): EMFADLocationService
```

## 🚀 Deployment-Schritte

### 1. Hardware-Setup
1. EMFAD-Gerät über USB oder Bluetooth verbinden
2. Geräte-Permissions in Android-Manifest prüfen
3. Bluetooth-Pairing durchführen (falls BLE)

### 2. App-Konfiguration
1. Vendor-ID und Product-ID für USB-Geräte konfigurieren
2. BLE-Service-UUIDs für Bluetooth-Geräte setzen
3. GPS-Permissions aktivieren

### 3. Testing
1. Hardware-Verbindung testen
2. Datenübertragung validieren
3. Export/Import-Funktionen prüfen
4. GPS-Tracking verifizieren

## 🔍 Debugging

### Logging
```kotlin
// Hardware-Kommunikation
Log.d("EMFADProtocol", "Sent command: ${command.toHexString()}")
Log.d("EMFADDeviceManager", "Device connected: ${device.name}")

// Datenverarbeitung
Log.d("SpectrumAnalyzer", "Calculated depth: $depth mm")
Log.d("MeasurementService", "Measurement completed: $measurement")
```

### Monitoring
- Verbindungsstatus in Echtzeit
- Datenqualität-Metriken
- GPS-Genauigkeit
- Batteriestatus des Geräts

## 📈 Performance-Optimierung

### Echtzeitdaten
- Daten-Buffering für kontinuierliche Messungen
- Background-Threading für Hardware-Kommunikation
- Memory-Management für große Datensätze

### UI-Responsiveness
- Coroutines für asynchrone Operationen
- StateFlow für reaktive UI-Updates
- Lazy Loading für große Listen

## 🔒 Sicherheit

### Permissions
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

### Datenvalidierung
- Checksummen-Prüfung für alle Hardware-Kommunikation
- Input-Validierung für Konfigurationsparameter
- Sichere Datei-IO mit Exception-Handling

## 🎯 Nächste Schritte

1. **Hardware-Validierung**: Tests mit echten EMFAD-Geräten
2. **Protokoll-Optimierung**: Feintuning basierend auf Hardware-Feedback
3. **Performance-Tuning**: Optimierung für Echtzeitdaten
4. **Dokumentation**: Benutzerhandbuch und API-Dokumentation
5. **Deployment**: App Store Veröffentlichung

---

**Status**: ✅ Backend-Integration vollständig implementiert und bereit für Hardware-Tests

Die Backend-Integration verbindet erfolgreich das Jetpack Compose Frontend mit der EMFAD-Hardware und implementiert alle Funktionen der ursprünglichen Windows-Software in einer modernen Android-Architektur.
