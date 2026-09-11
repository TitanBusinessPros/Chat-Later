package com.titanbusinesspros.chatlater

import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

// Two-way conversation translator between any two of SUPPORTED_LANGUAGES (picked below).
// Pipeline for each button: mic (SpeechRecognizer) -> translate (ML Kit, on-device) -> speak (TextToSpeech).
// All three steps run on the phone - no paid third-party API calls.
@Composable
fun ConversationTranslatorScreen(daysLeft: Long, isPaid: Boolean) {
    val context = LocalContext.current

    var langA by remember { mutableStateOf(SUPPORTED_LANGUAGES[0]) } // English
    var langB by remember { mutableStateOf(SUPPORTED_LANGUAGES[1]) } // Spanish

    var heardText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Press a button and speak") }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    // A -> B and B -> A translators for whichever pair is picked above.
    // Rebuilt whenever langA/langB changes; each downloads its small model once, then works offline.
    val aToB = remember(langA, langB) {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(langA.mlKitCode)
                .setTargetLanguage(langB.mlKitCode)
                .build()
        )
    }
    DisposableEffect(aToB) { onDispose { aToB.close() } }

    val bToA = remember(langA, langB) {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(langB.mlKitCode)
                .setTargetLanguage(langA.mlKitCode)
                .build()
        )
    }
    DisposableEffect(bToA) { onDispose { bToA.close() } }

    val textToSpeech = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { }
        textToSpeech[0] = tts
        onDispose { tts.shutdown() }
    }

    fun speak(text: String, locale: Locale) {
        textToSpeech[0]?.language = locale
        textToSpeech[0]?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Listens in `speechLocaleTag`, translates with `translator`, then speaks the result in `outputLocale`.
    fun listenAndTranslate(
        speechLocaleTag: String,
        translator: com.google.mlkit.nl.translate.Translator,
        outputLocale: Locale
    ) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            statusText = "Speech recognition not available on this device"
            return
        }

        statusText = "Listening..."
        heardText = ""
        translatedText = ""

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = RecognizerIntent.getVoiceDetailsIntent(context).apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocaleTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val said = matches?.firstOrNull().orEmpty()
                heardText = said
                if (said.isBlank()) {
                    statusText = "Didn't catch that - try again"
                    recognizer.destroy()
                    return
                }
                statusText = "Translating..."
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        translator.translate(said)
                            .addOnSuccessListener { translated ->
                                translatedText = translated
                                statusText = "Speaking..."
                                speak(translated, outputLocale)
                            }
                            .addOnFailureListener { e ->
                                statusText = "Translation failed: ${e.message}"
                            }
                    }
                    .addOnFailureListener { e ->
                        statusText = "Couldn't download translation model: ${e.message}"
                    }
                recognizer.destroy()
            }

            override fun onError(error: Int) {
                statusText = "Mic error (code $error) - try again"
                recognizer.destroy()
            }

            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { statusText = "Processing..." }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Chat Later Translator")
        if (!isPaid) {
            Text("Free trial: $daysLeft day(s) left")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LanguageDropdown(
                label = "Language A",
                selected = langA,
                onSelected = { langA = it },
                modifier = Modifier.weight(1f)
            )
            LanguageDropdown(
                label = "Language B",
                selected = langB,
                onSelected = { langB = it },
                modifier = Modifier.weight(1f)
            )
        }
        if (langA == langB) {
            Text(
                text = "Pick two different languages",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(text = statusText, modifier = Modifier.padding(top = 16.dp))

        if (heardText.isNotBlank()) {
            Text(text = "Heard: $heardText", modifier = Modifier.padding(top = 12.dp))
        }
        if (translatedText.isNotBlank()) {
            Text(text = "Translated: $translatedText", modifier = Modifier.padding(top = 4.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                enabled = langA != langB,
                onClick = {
                    listenAndTranslate(langA.speechTag, aToB, langB.ttsLocale)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
            ) {
                Text("🎤 Speak ${langA.label}")
            }

            Button(
                enabled = langA != langB,
                onClick = {
                    listenAndTranslate(langB.speechTag, bToA, langA.ttsLocale)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
            ) {
                Text("🎤 Speak ${langB.label}")
            }
        }
    }
}

// A dropdown for picking one of SUPPORTED_LANGUAGES.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    label: String,
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.label) },
                    onClick = {
                        onSelected(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}
