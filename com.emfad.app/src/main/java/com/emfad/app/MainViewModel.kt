package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.services.DatabaseService
import com.emfad.app.services.MeasurementService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val measurementService: MeasurementService, private val databaseService: DatabaseService) : ViewModel() {

    val measurementCount: StateFlow<Int> = measurementService.measurements
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // TODO: Add more states and logic related to the main screen, e.g., connection status, last measurement summary
}