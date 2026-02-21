package com.example.fitnessquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessquest.sensor.StepViewModel
import com.example.fitnessquest.ui.StepScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: StepViewModel = viewModel()
            StepScreen(viewModel)
        }
    }
}