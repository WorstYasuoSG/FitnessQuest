package com.example.fitnessquest.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.fitnessquest.sensor.StepSensorManager
import com.example.fitnessquest.sensor.StepViewModel

@Composable
fun StepScreen(viewModel: StepViewModel) {

    val context = LocalContext.current
    val steps by viewModel.steps

    var sensorManager by remember { mutableStateOf<StepSensorManager?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sensorManager?.start()
        }
    }

    LaunchedEffect(Unit) {

        sensorManager = StepSensorManager(context) {
            viewModel.updateSteps(it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                sensorManager?.start()
            }
        } else {
            sensorManager?.start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorManager?.stop()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Steps Today",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = steps.toString(),
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}