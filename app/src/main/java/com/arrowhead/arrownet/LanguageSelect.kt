package com.arrowhead.arrownet

import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.translate.TranslateLanguage

data class Language(val image: Int, val name: String, val languageID: String)

object Languages {
    private val images = intArrayOf(
        R.drawable.ic_clear_black_24dp,
        R.drawable.flag_united_kingdom,
        R.drawable.flag_spain,
        R.drawable.flag_russian_federation,
        R.drawable.flag_brazil,
        R.drawable.flag_france,
        R.drawable.flag_germany,
        R.drawable.flag_italy,
        R.drawable.flag_china
    )

    private val language = arrayOf(
        "Select Your Preferred Reading Language",
        "English",
        "Spanish",
        "Russian",
        "Portuguese",
        "French",
        "German",
        "Italian",
        "Chinese"
    )

    private val languageIDs = arrayOf(
        "51",
        TranslateLanguage.ENGLISH,
        TranslateLanguage.SPANISH,
        TranslateLanguage.RUSSIAN,
        TranslateLanguage.PORTUGUESE,
        TranslateLanguage.FRENCH,
        TranslateLanguage.GERMAN,
        TranslateLanguage.ITALIAN,
        TranslateLanguage.CHINESE
    )

    var list: ArrayList<Language>? = null
    get() {
        if(field != null) {
            return field
        }
        field = ArrayList()
        for(i in images.indices) {
            val imageID = images[i]
            val languageSelected = language[i]
            val languageSelectedID = languageIDs[i]

            val language = Language(imageID, languageSelected, languageSelectedID)
            field!!.add(language)
        }
        return field
    }
}