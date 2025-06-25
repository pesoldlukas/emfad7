package com.emfad.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.emfad.app.ar.ArDataOverlay
import com.emfad.app.ar.ArScene
import com.emfad.app.utils.Logger
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.SceneView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var arCoreSession: Session? by remember { mutableStateOf(null) }
    var arSceneView: SceneView? by remember { mutableStateOf(null) }
    var arScene: ArScene? by remember { mutableStateOf(null) }
    var arDataOverlay: ArDataOverlay? by remember { mutableStateOf(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Logger.e("ARScreen", "Camera permission denied.")
            // Handle permission denial, e.g., show a message or disable AR features
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (hasCameraPermission) {
                        try {
                            when (ArCoreApk.getInstance().requestInstall(context, true)) {
                                ArCoreApk.InstallStatus.INSTALLED -> {
                                    arCoreSession = Session(context)
                                    arScene?.setupSession(arCoreSession!!)
                                    arSceneView?.resume()
                                    arCoreSession?.resume()
                                }
                                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                                    // ARCore installation requested, will resume when installed
                                }
                            }
                        } catch (e: UnavailableUserDeclinedInstallationException) {
                            Logger.e("ARScreen", "User declined ARCore installation: ${e.message}")
                        } catch (e: UnavailableArcoreNotInstalledException) {
                            Logger.e("ARScreen", "ARCore not installed: ${e.message}")
                        } catch (e: UnavailableDeviceNotCompatibleException) {
                            Logger.e("ARScreen", "Device not compatible with ARCore: ${e.message}")
                        } catch (e: UnavailableException) {
                            Logger.e("ARScreen", "ARCore unavailable: ${e.message}")
                        } catch (e: Exception) {
                            Logger.e("ARScreen", "Failed to create ARCore session: ${e.message}")
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    arSceneView?.pause()
                    arCoreSession?.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    arSceneView?.destroy()
                    arCoreSession?.close()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AR View") }) }
    ) { paddingValues ->
        if (hasCameraPermission) {
            Box(modifier = modifier.padding(paddingValues)) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                    Button(onClick = { arScene?.toggleHeatmapVisibility() }) {
                        Text("Toggle Heatmap")
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        SceneView(ctx).apply {
                            arSceneView = this
                            arScene = ArScene(ctx, this)
                            arDataOverlay = ArDataOverlay(ctx)
                            this.scene.addOnUpdateListener { frameTime ->
                                arScene?.onUpdate(frameTime)
                                arDataOverlay?.updateOverlay(arScene?.getTrackingStateInfo() ?: "")
                            }
                            // Handle touch events for object placement
                            this.setOnTouchListener { hitTestResult, motionEvent ->
                                if (motionEvent.action == MotionEvent.ACTION_UP) {
                                    arScene?.onSurfaceTap(motionEvent.x, motionEvent.y)
                                    return@setOnTouchListener true
                                }
                                false
                            }
                        }
                    },
                    update = {
                        // Update logic if needed
                    }
                )
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        arDataOverlay?.getOverlayView() ?: TextView(ctx) // Return the actual overlay view
                    },
                    update = {
                        // Update logic for the overlay if needed
                    }
                )
            }
        } else {
            Text("Camera permission is required for AR functionality.", modifier = modifier.padding(paddingValues))
        }
    }
}