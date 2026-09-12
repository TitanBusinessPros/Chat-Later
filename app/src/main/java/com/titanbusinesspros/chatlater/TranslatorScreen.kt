package com.titanbusinesspros.chatlater

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

// Plain (non-Compose-State) holder for whether a screen instance is still current. Not
// tracked by the snapshot/recomposition system and never rendered itself - it exists
// only so an async callback can check, before writing any UI state, whether the screen
// that started its request is still around. `remember { ScreenLifetime() }` gives every
// fresh composition of ConversationTranslatorScreen its own instance starting `true`;
// nothing but disposal ever sets it `false`, and it never closes any resource itself.
private class ScreenLifetime {
    var isActive = true
}

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
    // True from the moment a mic button starts a request until that request's full
    // success/failure chain finishes. While true, both Speak buttons and both language
    // pickers are disabled - this is the single guard against a second concurrent
    // request and against langA/langB changing mid-request.
    var isBusy by remember { mutableStateOf(false) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    // See ScreenLifetime above. An async callback checks screenLifetime.isActive before
    // writing any UI state, so a callback that fires after the screen has been disposed
    // (trial expiring, sign-out, a configuration change) doesn't act on a screen that's
    // already gone. This never closes anything - each request creates its own Translator
    // (see listenAndTranslate below) and that same request's own terminal callback is
    // solely responsible for closing it, exactly once, whether or not the screen is
    // still around to see the result.
    val screenLifetime = remember { ScreenLifetime() }
    DisposableEffect(Unit) { onDispose { screenLifetime.isActive = false } }

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

    // Listens in `speechLocaleTag`, translates sourceCode->targetCode, then speaks the
    // result in `outputLocale`. sourceCode/targetCode are ML Kit language codes for
    // whichever direction this button speaks.
    fun listenAndTranslate(
        speechLocaleTag: String,
        sourceCode: String,
        targetCode: String,
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

        if (screenLifetime.isActive) {
            isBusy = true
            statusText = "Listening..."
            heardText = ""
            translatedText = ""
        }

        val recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            if (screenLifetime.isActive) {
                statusText = "Couldn't start speech recognition: ${e.message}"
                isBusy = false
            }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocaleTag)
            // Not forcing offline-only recognition: that requires the phone to already have
            // that language's offline pack downloaded, or it fails with
            // ERROR_LANGUAGE_UNAVAILABLE (code 13). The recognizer may use whichever
            // recognition service is available on this device, and the network where
            // needed for that service - actual availability still depends on the device.

            // NOTE: previously also set EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS and
            // EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS to 4000 to allow a
            // longer pause before cutoff. Removed: it caused the recognizer to finalize with
            // an empty transcript after ~4s instead of capturing speech. Restored to the
            // last confirmed-working intent (no silence-length extras).
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val said = matches?.firstOrNull().orEmpty()
                if (screenLifetime.isActive) heardText = said

                // The recognizer's job ends here either way - destroy it now, separately
                // from the translator below, which doesn't exist yet at this point.
                recognizer.destroy()

                if (said.isBlank()) {
                    // The recognizer finished normally (no error code) but returned zero
                    // transcribed words - distinct from onError below.
                    if (screenLifetime.isActive) {
                        statusText = "Didn't catch that — please try again."
                        isBusy = false
                    }
                    return
                }

                if (screenLifetime.isActive) statusText = "Translating..."

                // Created only now that there's actual text to translate, and kept alive
                // through the whole chain below regardless of what the screen does in the
                // meantime. Not tied to any DisposableEffect - this request's own terminal
                // callback (success, failure, or a caught synchronous exception) is the
                // only thing that closes it, exactly once.
                val translator = Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(sourceCode)
                        .setTargetLanguage(targetCode)
                        .build()
                )
                var translatorClosed = false
                fun closeTranslatorOnce() {
                    if (!translatorClosed) {
                        translatorClosed = true
                        translator.close()
                    }
                }

                try {
                    translator.downloadModelIfNeeded()
                        .addOnSuccessListener {
                            try {
                                translator.translate(said)
                                    .addOnSuccessListener { translated ->
                                        if (screenLifetime.isActive) {
                                            translatedText = translated
                                            statusText = "Speaking..."
                                            speak(translated, outputLocale)
                                            isBusy = false
                                        }
                                        closeTranslatorOnce()
                                    }
                                    .addOnFailureListener {
                                        if (screenLifetime.isActive) {
                                            statusText = "Translation could not finish. Please try again."
                                            isBusy = false
                                        }
                                        closeTranslatorOnce()
                                    }
                            } catch (e: IllegalStateException) {
                                // translate() throws this synchronously if the translator
                                // was somehow already closed - not delivered to the
                                // failure listener above.
                                if (screenLifetime.isActive) {
                                    statusText = "Translation could not finish. Please try again."
                                    isBusy = false
                                }
                                closeTranslatorOnce()
                            }
                        }
                        .addOnFailureListener {
                            // Translation-model download/prep failure, distinct from a mic error.
                            if (screenLifetime.isActive) {
                                statusText = "Couldn't prepare the translation language. Check your connection and try again."
                                isBusy = false
                            }
                            closeTranslatorOnce()
                        }
                } catch (e: IllegalStateException) {
                    // downloadModelIfNeeded() throws this synchronously in the same
                    // already-closed scenario.
                    if (screenLifetime.isActive) {
                        statusText = "Translation could not finish. Please try again."
                        isBusy = false
                    }
                    closeTranslatorOnce()
                }
            }

            override fun onError(error: Int) {
                // `error` is a SpeechRecognizer.ERROR_* code (see android docs) - kept out of
                // the user-facing message but useful when reported back for debugging.
                recognizer.destroy()
                if (screenLifetime.isActive) {
                    statusText = "Microphone/speech recognition error (code $error). Please try again."
                    isBusy = false
                }
            }

            override fun onReadyForSpeech(params: android.os.Bundle?) {
                if (screenLifetime.isActive) statusText = "Listening…"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { if (screenLifetime.isActive) statusText = "Processing…" }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            recognizer.destroy()
            if (screenLifetime.isActive) {
                statusText = "Couldn't start listening: ${e.message}"
                isBusy = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable main content takes all space above the footer; the footer itself
        // is a sibling below it, so it sits at the actual bottom of the screen instead
        // of after the content in scroll order.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
        AppIcon()

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
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
            )
            LanguageDropdown(
                label = "Language B",
                selected = langB,
                onSelected = { langB = it },
                enabled = !isBusy,
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
                enabled = langA != langB && !isBusy,
                onClick = {
                    listenAndTranslate(langA.speechTag, langA.mlKitCode, langB.mlKitCode, langB.ttsLocale)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
            ) {
                Text("🎤 Speak ${langA.label}")
            }

            Button(
                enabled = langA != langB && !isBusy,
                onClick = {
                    listenAndTranslate(langB.speechTag, langB.mlKitCode, langA.mlKitCode, langA.ttsLocale)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
            ) {
                Text("🎤 Speak ${langB.label}")
            }
        }
        }

        AppFooter()
    }
}

// Tapping this field opens a searchable full-list picker (LanguagePickerDialog) instead
// of a plain dropdown, since scrolling through all 59 languages unfiltered is unwieldy
// on a phone screen.
@Composable
private fun LanguageDropdown(
    label: String,
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    // A read-only OutlinedTextField can absorb its own taps for cursor/focus handling
    // instead of reliably passing them to an outer clickable. A transparent Box on top,
    // matching its size, reliably intercepts the tap while the field underneath still
    // renders and looks like a normal outlined field.
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Opens a language picker dialog",
                    role = Role.Button
                ) { showPicker = true }
                // This overlay - not the text field underneath - is what actually
                // receives the tap, so it needs its own description for TalkBack:
                // the field's label plus the language currently selected.
                .semantics {
                    contentDescription = "$label, currently ${selected.label}"
                }
        )
    }

    if (showPicker) {
        LanguagePickerDialog(
            selected = selected,
            onSelected = { lang ->
                onSelected(lang)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

// Full-screen-ish dialog: a search field plus a scrollable, filtered list of all 59
// languages. Filtering is case-insensitive and updates as the user types.
@Composable
private fun LanguagePickerDialog(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) {
            SUPPORTED_LANGUAGES
        } else {
            SUPPORTED_LANGUAGES.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose a language")

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                )

                if (filtered.isEmpty()) {
                    Text("No languages match \"$query\"", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(filtered) { lang ->
                            val isSelected = lang == selected
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    // .selectable() (rather than plain .clickable()) also
                                    // attaches the selected/not-selected state to this row's
                                    // accessibility semantics, so screen readers announce it.
                                    .selectable(selected = isSelected, onClick = { onSelected(lang) })
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = lang.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                // Fixed-width slot so labels line up the same whether or
                                // not this row is selected. The row's own .selectable()
                                // above already announces selected/not-selected to
                                // TalkBack, so this glyph is purely decorative and is
                                // excluded from the accessibility tree to avoid a second,
                                // redundant announcement.
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .clearAndSetSemantics {},
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
