package com.titanbusinesspros.chatlater

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

// One entry per language offered in the picker.
// - mlKitCode: which on-device ML Kit translation model to use
// - speechTag: BCP-47 tag SpeechRecognizer expects when listening
// - ttsLocale: locale TextToSpeech expects when speaking the result
data class AppLanguage(
    val label: String,
    val mlKitCode: String,
    val speechTag: String,
    val ttsLocale: Locale
)

// Top 10 most-spoken languages that ML Kit Translate supports on-device.
// Add more entries here any time - ML Kit itself supports 59 languages total.
val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("English", TranslateLanguage.ENGLISH, "en-US", Locale.US),
    AppLanguage("Spanish", TranslateLanguage.SPANISH, "es-ES", Locale("es", "ES")),
    AppLanguage("Chinese", TranslateLanguage.CHINESE, "zh-CN", Locale.SIMPLIFIED_CHINESE),
    AppLanguage("Hindi", TranslateLanguage.HINDI, "hi-IN", Locale("hi", "IN")),
    AppLanguage("Arabic", TranslateLanguage.ARABIC, "ar-SA", Locale("ar", "SA")),
    AppLanguage("Portuguese", TranslateLanguage.PORTUGUESE, "pt-PT", Locale("pt", "PT")),
    AppLanguage("French", TranslateLanguage.FRENCH, "fr-FR", Locale.FRANCE),
    AppLanguage("Russian", TranslateLanguage.RUSSIAN, "ru-RU", Locale("ru", "RU")),
    AppLanguage("Urdu", TranslateLanguage.URDU, "ur-PK", Locale("ur", "PK")),
    AppLanguage("German", TranslateLanguage.GERMAN, "de-DE", Locale.GERMANY),
)
