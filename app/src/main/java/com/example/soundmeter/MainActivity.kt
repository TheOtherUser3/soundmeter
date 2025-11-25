package com.example.soundmeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.sqrt
import androidx.compose.ui.draw.clip


class MainActivity : ComponentActivity() {

    private var recorder: AudioRecord? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var micPermissionGranted = false

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                micPermissionGranted = granted
                if (granted) startAudioRecording()
            }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            micPermissionGranted = true
            startAudioRecording()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SoundMeterScreen(
                        decibels = decibelState,
                        thresholdExceeded = thresholdState
                    )
                }
            }
        }
    }

    private var decibelState by mutableStateOf(0f)
    private var thresholdState by mutableStateOf(false)

    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        if (isRecording) return

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        recorder?.startRecording()
        isRecording = true

        val buffer = ShortArray(bufferSize)

        Thread {
            while (isRecording) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val amplitude = calculateAmplitude(buffer, read)
                    val db = amplitudeToDb(amplitude)
                    decibelState = db

                    thresholdState = db > 80f   // ALERT threshold
                }
            }
        }.start()
    }

    private fun calculateAmplitude(buffer: ShortArray, read: Int): Double {
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / read)
    }

    private fun amplitudeToDb(amp: Double): Float {
        if (amp <= 0) return 0f
        val db = (20 * log10(amp / 32767.0)).toFloat() + 90f
        return db.coerceIn(0f, 120f)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        recorder?.stop()
        recorder?.release()
    }
}

@Composable
fun SoundMeterScreen(decibels: Float, thresholdExceeded: Boolean) {

    val barProgress by animateFloatAsState(
        targetValue = (decibels / 120f).coerceIn(0f, 1f),
        label = "barAnim"
    )

    val barColor by animateColorAsState(
        targetValue =
            when {
                decibels < 40f -> Color(0xFF64FFDA)
                decibels < 70f -> Color(0xFFFFC107)
                else -> Color(0xFFFF5252)
            },
        label = "colorAnim"
    )

    val alertColor by animateColorAsState(
        targetValue = if (thresholdExceeded) Color(0xFFFF1744) else Color.Transparent,
        label = "alertColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Text(
            text = "SOUND METER",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        // dB readout
        Text(
            text = String.format("%.1f dB", decibels),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = barColor
        )

        // Visual bar meter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1C))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(barColor)
            )
        }

        // Alert Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(alertColor),
            contentAlignment = Alignment.Center
        ) {
            if (thresholdExceeded) {
                Text(
                    text = "⚠ TOO LOUD! Lower the noise!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            } else {
                Text(
                    text = "Environment is safe",
                    color = Color(0xFFB0BEC5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
