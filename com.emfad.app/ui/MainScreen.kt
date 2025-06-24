package com.emfad.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToSettings: () -> Unit, onNavigateToVisualization: () -> Unit, onNavigateToAR: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("EMFAD Analyzer") })
        }
    ) {\ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Button(onClick = onNavigateToVisualization) {
                Text("Open Visualization")
            }
            Button(onClick = onNavigateToAR) {
                Text("Open AR View")
            }
            Button(onClick = onNavigateToSettings) {
                Text("Settings")
            }
            // TODO: Add more main screen elements like current status, last measurement, etc.
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    MainScreen(onNavigateToSettings = {}, onNavigateToVisualization = {}, onNavigateToAR = {})
}