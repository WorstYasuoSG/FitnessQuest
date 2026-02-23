package com.example.fitnessquest.sensor

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class StepViewModel : ViewModel() {
    private val _steps = mutableStateOf(0)
    val steps: State<Int> = _steps

    fun updateSteps(newSteps: Int) {
        _steps.value = newSteps
    }
}