package com.example.proximitysensor

import androidx.appcompat.app.AppCompatActivity

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.atan2

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var gyroscopeSensor: Sensor
    private lateinit var magnetometerSensor: Sensor
    private lateinit var distanceTextView: TextView
    private lateinit var heightTextView: TextView

    private val alpha: Float = 0.8f
    private var gravity: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var linearAcceleration: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var initialHeight: Float = 0f
    private var distance: Float = 0f
    private var height: Float = 0f
    private var gyroRotation: FloatArray = floatArrayOf(0f, 0f, 0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        distanceTextView = findViewById(R.id.distanceTextView)
        heightTextView = findViewById(R.id.heightTextView)

        // Get the SensorManager instance
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Get the accelerometer, gyroscope, and magnetometer sensors
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onResume() {
        super.onResume()

        // Register the sensor listeners
        sensorManager.registerListener(
            this,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            gyroscopeSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            magnetometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()

        // Unregister the sensor listeners to save resources
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this example
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Apply a low-pass filter to remove noise from the accelerometer readings
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                // Calculate the linear acceleration by subtracting the gravity component
                linearAcceleration[0] = event.values[0] - gravity[0]
                linearAcceleration[1] = event.values[1] - gravity[1]
                linearAcceleration[2] = event.values[2] - gravity[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // Get the rotation matrix using the accelerometer and magnetometer readings
                val rotationMatrix = FloatArray(9)
                val inclinationMatrix = FloatArray(9)
                val remappedMatrix = FloatArray(9)
                SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, event.values)
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)

                // Calculate the orientation angles using the remapped rotation matrix
                val orientation = FloatArray(3)
                SensorManager.getOrientation(remappedMatrix, orientation)

                // Calculate the azimuth angle (yaw) from the orientation values
                val azimuth = orientation[0]
                val azimuthDegrees = Math.toDegrees(azimuth.toDouble()).toFloat()

                // Calculate the distance based on the azimuth angle
                distance -= azimuthDegrees * 0.1f // Multiply by a scaling factor to adjust the distance
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Calculate the rotation change in degrees per second around the X, Y, and Z axes
                val deltaRotationX = event.values[0] * 0.1f
                val deltaRotationY = event.values[1] * 0.1f
                val deltaRotationZ = event.values[2] * 0.1f

                // Integrate the rotation change to obtain the cumulative rotation
                gyroRotation[0] += deltaRotationX
                gyroRotation[1] += deltaRotationY
                gyroRotation[2] += deltaRotationZ

                // Calculate the height based on the cumulative rotation around the X-axis
                height = gyroRotation[0] * 0.1f // Multiply by a scaling factor to adjust the height
            }
        }

        distanceTextView.text = "Distance: ${String.format("%.2f", distance)} meters"
        heightTextView.text = "Height: ${String.format("%.2f", height)} meters"
    }
}

