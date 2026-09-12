package com.titanbusinesspros.chatlater

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

// One entry per language the picker offers.
// - mlKitCode: which on-device ML Kit translation model to use (from TranslateLanguage)
// - speechTag: BCP-47 tag SpeechRecognizer is asked to listen in
// - ttsLocale: locale TextToSpeech is asked to speak in
//
// Not every phone has a speech-recognition or text-to-speech voice installed for every
// language below - that depends on the device and its installed language packs, not on
// this app. When a language isn't available, the existing mic-error and TTS fallback
// handling in the translator screen surfaces that gracefully rather than pretending
// every device supports all 59.
data class AppLanguage(
    val label: String,
    val mlKitCode: String,
    val speechTag: String,
    val ttsLocale: Locale
)

// The complete set of languages ML Kit Translate supports on-device (59 total),
// matching the official com.google.mlkit.nl.translate.TranslateLanguage constants.
val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("Afrikaans", TranslateLanguage.AFRIKAANS, "af-ZA", Locale("af", "ZA")),
    AppLanguage("Albanian", TranslateLanguage.ALBANIAN, "sq-AL", Locale("sq", "AL")),
    AppLanguage("Arabic", TranslateLanguage.ARABIC, "ar-SA", Locale("ar", "SA")),
    AppLanguage("Belarusian", TranslateLanguage.BELARUSIAN, "be-BY", Locale("be", "BY")),
    AppLanguage("Bengali", TranslateLanguage.BENGALI, "bn-BD", Locale("bn", "BD")),
    AppLanguage("Bulgarian", TranslateLanguage.BULGARIAN, "bg-BG", Locale("bg", "BG")),
    AppLanguage("Catalan", TranslateLanguage.CATALAN, "ca-ES", Locale("ca", "ES")),
    // ML Kit's "CHINESE" model and Locale.SIMPLIFIED_CHINESE/zh-CN are specifically
    // Mandarin in simplified script, not the whole Chinese language family (Cantonese,
    // Wu, etc. aren't covered), so the label names the actual language, not the family.
    AppLanguage("Mandarin (Simplified)", TranslateLanguage.CHINESE, "zh-CN", Locale.SIMPLIFIED_CHINESE),
    AppLanguage("Croatian", TranslateLanguage.CROATIAN, "hr-HR", Locale("hr", "HR")),
    AppLanguage("Czech", TranslateLanguage.CZECH, "cs-CZ", Locale("cs", "CZ")),
    AppLanguage("Danish", TranslateLanguage.DANISH, "da-DK", Locale("da", "DK")),
    AppLanguage("Dutch", TranslateLanguage.DUTCH, "nl-NL", Locale("nl", "NL")),
    AppLanguage("English", TranslateLanguage.ENGLISH, "en-US", Locale.US),
    // Esperanto has no country - no real device is expected to have speech support for
    // it, but the translation itself still works. Kept as a bare language tag.
    AppLanguage("Esperanto", TranslateLanguage.ESPERANTO, "eo", Locale("eo")),
    AppLanguage("Estonian", TranslateLanguage.ESTONIAN, "et-EE", Locale("et", "EE")),
    AppLanguage("Finnish", TranslateLanguage.FINNISH, "fi-FI", Locale("fi", "FI")),
    AppLanguage("French", TranslateLanguage.FRENCH, "fr-FR", Locale.FRANCE),
    AppLanguage("Galician", TranslateLanguage.GALICIAN, "gl-ES", Locale("gl", "ES")),
    AppLanguage("Georgian", TranslateLanguage.GEORGIAN, "ka-GE", Locale("ka", "GE")),
    AppLanguage("German", TranslateLanguage.GERMAN, "de-DE", Locale.GERMANY),
    AppLanguage("Greek", TranslateLanguage.GREEK, "el-GR", Locale("el", "GR")),
    AppLanguage("Gujarati", TranslateLanguage.GUJARATI, "gu-IN", Locale("gu", "IN")),
    AppLanguage("Haitian Creole", TranslateLanguage.HAITIAN_CREOLE, "ht-HT", Locale("ht", "HT")),
    AppLanguage("Hebrew", TranslateLanguage.HEBREW, "he-IL", Locale("he", "IL")),
    AppLanguage("Hindi", TranslateLanguage.HINDI, "hi-IN", Locale("hi", "IN")),
    AppLanguage("Hungarian", TranslateLanguage.HUNGARIAN, "hu-HU", Locale("hu", "HU")),
    AppLanguage("Icelandic", TranslateLanguage.ICELANDIC, "is-IS", Locale("is", "IS")),
    AppLanguage("Indonesian", TranslateLanguage.INDONESIAN, "id-ID", Locale("id", "ID")),
    AppLanguage("Irish", TranslateLanguage.IRISH, "ga-IE", Locale("ga", "IE")),
    AppLanguage("Italian", TranslateLanguage.ITALIAN, "it-IT", Locale.ITALY),
    AppLanguage("Japanese", TranslateLanguage.JAPANESE, "ja-JP", Locale.JAPAN),
    AppLanguage("Kannada", TranslateLanguage.KANNADA, "kn-IN", Locale("kn", "IN")),
    AppLanguage("Korean", TranslateLanguage.KOREAN, "ko-KR", Locale.KOREA),
    AppLanguage("Latvian", TranslateLanguage.LATVIAN, "lv-LV", Locale("lv", "LV")),
    AppLanguage("Lithuanian", TranslateLanguage.LITHUANIAN, "lt-LT", Locale("lt", "LT")),
    AppLanguage("Macedonian", TranslateLanguage.MACEDONIAN, "mk-MK", Locale("mk", "MK")),
    AppLanguage("Malay", TranslateLanguage.MALAY, "ms-MY", Locale("ms", "MY")),
    AppLanguage("Maltese", TranslateLanguage.MALTESE, "mt-MT", Locale("mt", "MT")),
    AppLanguage("Marathi", TranslateLanguage.MARATHI, "mr-IN", Locale("mr", "IN")),
    // ML Kit's translate code is generic "no" (Norwegian); Android's speech/TTS services
    // register Norwegian as Bokmal ("nb-NO"), so the speech tag intentionally differs
    // from the ML Kit code here.
    AppLanguage("Norwegian", TranslateLanguage.NORWEGIAN, "nb-NO", Locale("nb", "NO")),
    AppLanguage("Persian", TranslateLanguage.PERSIAN, "fa-IR", Locale("fa", "IR")),
    AppLanguage("Polish", TranslateLanguage.POLISH, "pl-PL", Locale("pl", "PL")),
    AppLanguage("Portuguese", TranslateLanguage.PORTUGUESE, "pt-PT", Locale("pt", "PT")),
    AppLanguage("Romanian", TranslateLanguage.ROMANIAN, "ro-RO", Locale("ro", "RO")),
    AppLanguage("Russian", TranslateLanguage.RUSSIAN, "ru-RU", Locale("ru", "RU")),
    AppLanguage("Slovak", TranslateLanguage.SLOVAK, "sk-SK", Locale("sk", "SK")),
    AppLanguage("Slovenian", TranslateLanguage.SLOVENIAN, "sl-SI", Locale("sl", "SI")),
    AppLanguage("Spanish", TranslateLanguage.SPANISH, "es-ES", Locale("es", "ES")),
    AppLanguage("Swahili", TranslateLanguage.SWAHILI, "sw-KE", Locale("sw", "KE")),
    AppLanguage("Swedish", TranslateLanguage.SWEDISH, "sv-SE", Locale("sv", "SE")),
    // ML Kit's translate code is "tl" (Tagalog); Android's speech services register the
    // modern macrolanguage tag as Filipino ("fil-PH"), so the speech tag intentionally
    // differs from the ML Kit code here, same reasoning as Norwegian above.
    AppLanguage("Tagalog", TranslateLanguage.TAGALOG, "fil-PH", Locale("fil", "PH")),
    AppLanguage("Tamil", TranslateLanguage.TAMIL, "ta-IN", Locale("ta", "IN")),
    AppLanguage("Telugu", TranslateLanguage.TELUGU, "te-IN", Locale("te", "IN")),
    AppLanguage("Thai", TranslateLanguage.THAI, "th-TH", Locale("th", "TH")),
    AppLanguage("Turkish", TranslateLanguage.TURKISH, "tr-TR", Locale("tr", "TR")),
    AppLanguage("Ukrainian", TranslateLanguage.UKRAINIAN, "uk-UA", Locale("uk", "UA")),
    AppLanguage("Urdu", TranslateLanguage.URDU, "ur-PK", Locale("ur", "PK")),
    AppLanguage("Vietnamese", TranslateLanguage.VIETNAMESE, "vi-VN", Locale("vi", "VN")),
    AppLanguage("Welsh", TranslateLanguage.WELSH, "cy-GB", Locale("cy", "GB")),
)
