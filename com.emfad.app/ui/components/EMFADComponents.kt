package com.emfad.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emfad.app.models.*
import com.emfad.app.ui.theme.*

// EMFAD Status Bar Component
@Composable
fun EMFADStatusBar(
    deviceStatus: EMFADDeviceStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (deviceStatus.isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = "Connection Status",
                    tint = if (deviceStatus.isConnected) EMFADGreen else EMFADRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = deviceStatus.connectionType.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Device Name
            if (deviceStatus.deviceName.isNotEmpty()) {
                Text(
                    text = deviceStatus.deviceName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Battery Level
            if (deviceStatus.batteryLevel > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            deviceStatus.batteryLevel > 75 -> Icons.Default.BatteryFull
                            deviceStatus.batteryLevel > 50 -> Icons.Default.Battery6Bar
                            deviceStatus.batteryLevel > 25 -> Icons.Default.Battery3Bar
                            else -> Icons.Default.Battery1Bar
                        },
                        contentDescription = "Battery Level",
                        tint = when {
                            deviceStatus.batteryLevel > 25 -> EMFADGreen
                            else -> EMFADRed
                        }
                    )
                    Text(
                        text = "${deviceStatus.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Signal Strength
            if (deviceStatus.signalStrength > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellular4Bar,
                        contentDescription = "Signal Strength",
                        tint = when {
                            deviceStatus.signalStrength > 75 -> EMFADGreen
                            deviceStatus.signalStrength > 50 -> EMFADYellow
                            else -> EMFADRed
                        }
                    )
                    Text(
                        text = "${deviceStatus.signalStrength}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// EMFAD Frequency Selection Buttons (7 frequencies)
@Composable
fun EMFADFrequencySelector(
    selectedFrequency: EMFADFrequency,
    onFrequencySelected: (EMFADFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    val frequencies = EMFADFrequency.values()
    val frequencyColors = listOf(
        FrequencyBand1, FrequencyBand2, FrequencyBand3, FrequencyBand4,
        FrequencyBand5, FrequencyBand6, FrequencyBand7
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Frequency Selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frequencies.forEachIndexed { index, frequency ->
                    val isSelected = frequency == selectedFrequency
                    val color = frequencyColors[index]
                    
                    Button(
                        onClick = { onFrequencySelected(frequency) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) color else color.copy(alpha = 0.3f),
                            contentColor = if (isSelected) EMFADWhite else EMFADBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = frequency.displayName.replace(" kHz", ""),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// EMFAD Mode Selection (A, A-B, B, B-A)
@Composable
fun EMFADModeSelector(
    selectedMode: EMFADMode,
    onModeSelected: (EMFADMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Measurement Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADMode.values().forEach { mode ->
                    val isSelected = mode == selectedMode
                    
                    Button(
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) EMFADBlue else EMFADBlue.copy(alpha = 0.3f),
                            contentColor = EMFADWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// EMFAD Signal Strength Indicator
@Composable
fun EMFADSignalIndicator(
    signalStrength: Double,
    modifier: Modifier = Modifier
) {
    val animatedSignal by animateFloatAsState(
        targetValue = signalStrength.toFloat(),
        animationSpec = tween(durationMillis = 300)
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Signal Strength",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(animatedSignal * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        animatedSignal > 0.75f -> EMFADGreen
                        animatedSignal > 0.5f -> EMFADYellow
                        animatedSignal > 0.25f -> EMFADOrange
                        else -> EMFADRed
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Signal strength bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(10) { index ->
                    val barHeight = (index + 1) * 4.dp
                    val isActive = (index + 1) / 10f <= animatedSignal
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .background(
                                color = if (isActive) {
                                    when {
                                        animatedSignal > 0.75f -> EMFADGreen
                                        animatedSignal > 0.5f -> EMFADYellow
                                        animatedSignal > 0.25f -> EMFADOrange
                                        else -> EMFADRed
                                    }
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
