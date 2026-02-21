package com.example.fitnessquest.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepSensorManager(
    context: Context,
    private val onStepChanged: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var initialStepCount: Int? = null

    fun start() {
        stepSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val totalSteps = event.values[0].toInt()

        if (initialStepCount == null) {
            initialStepCount = totalSteps
        }

        val currentSessionSteps = totalSteps - (initialStepCount ?: 0)

        onStepChanged(currentSessionSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}