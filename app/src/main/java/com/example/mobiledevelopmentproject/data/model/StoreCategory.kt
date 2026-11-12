package com.example.mobiledevelopmentproject.data.model

import androidx.annotation.StringRes
import com.example.mobiledevelopmentproject.R

// value is de tekst die in Firestore in het veld "category" komt.
// label is de tekst die de gebruiker ziet, uit strings.xml.
// Zo blijft Firestore hetzelfde, ook als de app in een andere taal staat.
enum class StoreCategory(val value: String, @StringRes val label: Int) {
    BAKERY("bakery", R.string.category_bakery),
    SUPERMARKET("supermarket", R.string.category_supermarket),
    RESTAURANT("restaurant", R.string.category_restaurant),
    CAFE("cafe", R.string.category_cafe),
    OTHER("other", R.string.category_other);

    companion object {
        // Zoekt de categorie bij een waarde uit Firestore, of null als die onbekend is.
        fun fromValue(value: String): StoreCategory? {
            return entries.find { it.value == value }
        }
    }
}
