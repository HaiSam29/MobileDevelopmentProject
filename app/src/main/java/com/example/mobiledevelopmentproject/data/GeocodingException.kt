package com.example.mobiledevelopmentproject.data

// Fout bij het opzoeken van een adres. De ViewModel kiest de tekst op basis van reason.
class GeocodingException(
    val reason: Reason,
    cause: Throwable? = null
) : Exception(reason.name, cause) {

    enum class Reason {
        // Geen verbinding of de aanvraag duurde te lang.
        NO_INTERNET,
        // Nominatim gaf een fout terug (bv. te veel aanvragen) of een onleesbaar antwoord.
        SERVER_ERROR,
        // Geen adres of gemeente op deze plek, bv. midden in zee.
        NOT_FOUND
    }
}
