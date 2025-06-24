package com.emfad.app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emfad.app.models.*
import com.emfad.app.ui.theme.*

// Export Dialog (ExportDAT1Click equivalent)
@Composable
fun ExportDialog(
    isVisible: Boolean,
    profiles: List<EMFADProfile>,
    onDismiss: () -> Unit,
    onExport: (EMFADProfile, EMFADExportFormat, String) -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            ExportDialogContent(
                profiles = profiles,
                onDismiss = onDismiss,
                onExport = onExport
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialogContent(
    profiles: List<EMFADProfile>,
    onDismiss: () -> Unit,
    onExport: (EMFADProfile, EMFADExportFormat, String) -> Unit
) {
    var selectedProfile by remember { mutableStateOf<EMFADProfile?>(null) }
    var selectedFormat by remember { mutableStateOf(EMFADExportFormat.EGD) }
    var fileName by remember { mutableStateOf("") }
    var includeMetadata by remember { mutableStateOf(true) }
    var includeAnalysis by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Export Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Selection
            Text(
                text = "Select Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(profiles) { profile ->
                    ProfileSelectionItem(
                        profile = profile,
                        isSelected = profile == selectedProfile,
                        onSelect = { selectedProfile = profile }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Format Selection
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADExportFormat.values().take(4).forEach { format ->
                    FilterChip(
                        onClick = { selectedFormat = format },
                        label = { Text(format.extension.uppercase()) },
                        selected = format == selectedFormat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADExportFormat.values().drop(4).forEach { format ->
                    FilterChip(
                        onClick = { selectedFormat = format },
                        label = { Text(format.extension.uppercase()) },
                        selected = format == selectedFormat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // File Name Input
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("File Name") },
                placeholder = { Text("Enter file name") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Text(
                        text = selectedFormat.extension,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export Options
            Text(
                text = "Export Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeMetadata,
                    onCheckedChange = { includeMetadata = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Include Metadata")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeAnalysis,
                    onCheckedChange = { includeAnalysis = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Include Analysis Results")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        selectedProfile?.let { profile ->
                            onExport(profile, selectedFormat, fileName)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedProfile != null && fileName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADGreen
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }
            }
        }
    }
}

// Import Dialog (ImportTabletFile1Click equivalent)
@Composable
fun ImportDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onImport: (String, EMFADExportFormat) -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            ImportDialogContent(
                onDismiss = onDismiss,
                onImport = onImport
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDialogContent(
    onDismiss: () -> Unit,
    onImport: (String, EMFADExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(EMFADExportFormat.EGD) }
    var filePath by remember { mutableStateOf("") }
    var validateOnImport by remember { mutableStateOf(true) }
    var mergeWithExisting by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Import Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Format Selection
            Text(
                text = "Import Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADExportFormat.values().forEach { format ->
                    FormatSelectionItem(
                        format = format,
                        isSelected = format == selectedFormat,
                        onSelect = { selectedFormat = format }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // File Selection
            Text(
                text = "File Selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File Path") },
                    placeholder = { Text("Select file to import") },
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
                
                Button(
                    onClick = { /* Open file picker */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADBlue
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Import Options
            Text(
                text = "Import Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = validateOnImport,
                    onCheckedChange = { validateOnImport = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validate data on import")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = mergeWithExisting,
                    onCheckedChange = { mergeWithExisting = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Merge with existing data")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Format Information
            FormatInfoCard(selectedFormat)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onImport(filePath, selectedFormat)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = filePath.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADGreen
                    )
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import")
                }
            }
        }
    }
}

@Composable
private fun ProfileSelectionItem(
    profile: EMFADProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${profile.measurements.size} measurements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FormatSelectionItem(
    format: EMFADExportFormat,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                getFormatIcon(format),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = format.extension,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FormatInfoCard(format: EMFADExportFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Format Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getFormatDescription(format),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getFormatIcon(format: EMFADExportFormat): ImageVector {
    return when (format) {
        EMFADExportFormat.EGD -> Icons.Default.GridOn
        EMFADExportFormat.ESD -> Icons.Default.GraphicEq
        EMFADExportFormat.FADS -> Icons.Default.Analytics
        EMFADExportFormat.CSV -> Icons.Default.TableChart
        EMFADExportFormat.PDF -> Icons.Default.PictureAsPdf
        EMFADExportFormat.MATLAB -> Icons.Default.Functions
    }
}

private fun getFormatDescription(format: EMFADExportFormat): String {
    return when (format) {
        EMFADExportFormat.EGD -> "EMFAD Grid Data format containing spatial measurement data"
        EMFADExportFormat.ESD -> "EMFAD Spectrum Data format containing frequency analysis"
        EMFADExportFormat.FADS -> "EMFAD Analysis Data Set with complete analysis results"
        EMFADExportFormat.CSV -> "Comma-separated values for spreadsheet applications"
        EMFADExportFormat.PDF -> "Portable Document Format for reports and documentation"
        EMFADExportFormat.MATLAB -> "MATLAB data file for scientific analysis"
    }
}

@Preview(showBackground = true)
@Composable
fun ExportDialogPreview() {
    EMFADAnalyzerTheme {
        ExportDialog(
            isVisible = true,
            profiles = emptyList(),
            onDismiss = {},
            onExport = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ImportDialogPreview() {
    EMFADAnalyzerTheme {
        ImportDialog(
            isVisible = true,
            onDismiss = {},
            onImport = { _, _ -> }
        )
    }
}
