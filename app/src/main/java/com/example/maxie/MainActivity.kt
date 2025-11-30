package com.example.maxie

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.maxie.ui.theme.MaxieTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var recognizerIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request mic permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }

        // Init TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                // optional greeting
                tts.speak("Hi, I'm Maxie. Tap the button and speak.", TextToSpeech.QUEUE_FLUSH, null, "maxie_greet")
            }
        }

        // Init Speech Recognizer and Intent
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN") // for Indian English
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // for American English
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }



        // Compose state to show recognized text
        val recognizedText = mutableStateOf("Tap the button and speak")

        // Recognition listener updates Compose state and triggers TTS
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.getOrNull(0) ?: "Didn't catch that"
                recognizedText.value = "You said: $text"

                // simple reply logic
                if (text.contains("hello", ignoreCase = true)) {
                    tts.speak("Hello there! I'm Maxie.", TextToSpeech.QUEUE_FLUSH, null, "maxie_resp")
                } else {
                    tts.speak("You said $text", TextToSpeech.QUEUE_FLUSH, null, "maxie_resp")
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        recognizedText.value = "Didn't catch that. Try again!"
                        // Optionally restart listening
                        // speechRecognizer.startListening(recognizerIntent)
                    }
                    else -> recognizedText.value = "Recognition error: $error"
                }
            }


            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partial?.getOrNull(0)?.let { recognizedText.value = it }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

        })

        // Compose UI
        setContent {
            MaxieTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val text by recognizedText
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = text, modifier = Modifier.padding(bottom = 24.dp))
                        Button(onClick = {
                            // start listening
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                recognizedText.value = "Listening..."
                                speechRecognizer.startListening(recognizerIntent)
                            } else {
                                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
                            }
                        }) {
                            Text("🎙 Talk to Maxie")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::tts.isInitialized) tts.shutdown()
    }
}
