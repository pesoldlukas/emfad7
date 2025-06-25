package com.emfad.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.models.*
import com.emfad.app.ui.components.EMFADStatusBar
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.MeasurementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onNavigateToSurvey: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "EMFAD Analyzer",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Bar
            EMFADStatusBar(deviceStatus = deviceStatus)
            
            // EMFAD Logo/Title Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EMFAD",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Electromagnetic Field Analyzer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "19-135 kHz • Samsung S21 Ultra Optimized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Device Connection Section
            DeviceConnectionCard(
                deviceStatus = deviceStatus,
                connectionState = connectionState,
                onConnectUSB = { viewModel.connectUSB() },
                onConnectBluetooth = { viewModel.connectBluetooth() },
                onDisconnect = { viewModel.disconnect() }
            )
            
            // Main Navigation Buttons
            if (deviceStatus.isConnected) {
                MainNavigationButtons(
                    onNavigateToSurvey = onNavigateToSurvey,
                    onNavigateToSetup = onNavigateToSetup
                )
            }
            
            // Quick Status Information
            QuickStatusCard(deviceStatus = deviceStatus)
        }
    }
}

@Composable
private fun DeviceConnectionCard(
    deviceStatus: EMFADDeviceStatus,
    connectionState: String,
    onConnectUSB: () -> Unit,
    onConnectBluetooth: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Device Connection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!deviceStatus.isConnected) {
                Text(
                    text = "Connect your EMFAD device to start measurements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConnectUSB,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADBlue
                        )
                    ) {
                        Icon(Icons.Default.Usb, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("USB")
                    }
                    
                    Button(
                        onClick = onConnectBluetooth,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADBlue
                        )
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bluetooth")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Connected via ${deviceStatus.connectionType.displayName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = EMFADGreen
                        )
                        if (deviceStatus.deviceName.isNotEmpty()) {
                            Text(
                                text = deviceStatus.deviceName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADRed
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }
            
            if (connectionState.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = connectionState,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MainNavigationButtons(
    onNavigateToSurvey: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Main Functions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavigationCard(
                    title = "Survey",
                    description = "Start measurement survey",
                    icon = Icons.Default.Timeline,
                    onClick = onNavigateToSurvey,
                    modifier = Modifier.weight(1f)
                )
                
                NavigationCard(
                    title = "Setup",
                    description = "Configure measurement parameters",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSetup,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickStatusCard(deviceStatus: EMFADDeviceStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    label = "Temperature",
                    value = "${deviceStatus.temperature}°C",
                    icon = Icons.Default.Thermostat
                )
                StatusItem(
                    label = "Signal",
                    value = "${deviceStatus.signalStrength}%",
                    icon = Icons.Default.SignalCellular4Bar
                )
                StatusItem(
                    label = "Battery",
                    value = "${deviceStatus.batteryLevel}%",
                    icon = Icons.Default.BatteryFull
                )
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    EMFADAnalyzerTheme {
        StartScreen(
            onNavigateToSurvey = {},
            onNavigateToSetup = {},
            onNavigateToSettings = {}
        )
    }
}
