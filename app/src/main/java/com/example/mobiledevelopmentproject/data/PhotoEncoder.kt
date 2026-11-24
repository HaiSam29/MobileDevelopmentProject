package com.example.mobiledevelopmentproject.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

// Zet een foto om naar Base64-tekst en terug.
//
// Base64: een foto bestaat uit bytes (getallen van 0 tot 255), en die kan je niet zomaar
// als tekst in Firestore zetten. Base64 schrijft dezelfde bytes als gewone letters,
// cijfers, "+" en "/". De tekst is ongeveer 4/3 keer zo groot als de bytes.
//
// Waarom verkleinen: een Firestore-document mag maximaal 1 MB zijn. Een foto van een
// gsm-camera is vaak meerdere MB. Met max 800 px en JPEG-kwaliteit 70 blijft een foto
// rond 50 à 150 KB, ruim onder de limiet.
object PhotoEncoder {

    const val MAX_SIDE_PX = 800
    const val JPEG_QUALITY = 70

    // Verkleint de foto als dat nodig is, comprimeert als JPEG en geeft Base64-tekst.
    fun encodeBitmap(bitmap: Bitmap): String {
        val scaled = scaleDown(bitmap)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        // NO_WRAP: alles op één regel, zonder regeleinden in de tekst.
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    // Leest een foto uit de galerij (een Uri) en zet ze om naar Base64-tekst.
    // Geeft null als de foto niet gelezen kan worden.
    fun encodeUri(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(contentResolver, uri)
            } else {
                decodeWithBitmapFactory(contentResolver, uri)
            }
            bitmap?.let { encodeBitmap(it) }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            // Geen toegang meer tot de foto.
            null
        } catch (e: IllegalArgumentException) {
            // Geen geldige afbeelding.
            null
        }
    }

    // Van Base64-tekst terug naar een Bitmap, om de foto te tonen.
    // Geeft null bij een lege of kapotte tekst.
    fun decode(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            // Geeft zelf null als de bytes geen afbeelding zijn.
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            // Geen geldige Base64-tekst.
            null
        }
    }

    // Android 9 en hoger. ImageDecoder draait de foto ook automatisch rechtop.
    private fun decodeWithImageDecoder(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Anders krijg je een hardware-bitmap, die kan je niet goed verkleinen of comprimeren.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            // Meteen kleiner inlezen, zo vult een grote foto het geheugen niet.
            val width = info.size.width
            val height = info.size.height
            val longest = max(width, height)
            if (longest > MAX_SIDE_PX) {
                val factor = MAX_SIDE_PX.toFloat() / longest
                decoder.setTargetSize((width * factor).roundToInt(), (height * factor).roundToInt())
            }
        }
    }

    // Oudere toestellen (Android 7 en 8).
    private fun decodeWithBitmapFactory(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        // Eerst enkel de afmetingen lezen, zonder de foto zelf in het geheugen te laden.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // inSampleSize 2 leest elke 2e pixel (4x minder geheugen), 4 elke 4e, ...
        // We blijven verdubbelen zolang de foto daarna nog groter is dan 800 px.
        val longest = max(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (longest / (sampleSize * 2) >= MAX_SIDE_PX) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    // Verkleint zodat de langste zijde max 800 px is. De verhouding blijft dezelfde.
    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_SIDE_PX) return bitmap

        val factor = MAX_SIDE_PX.toFloat() / longest
        val width = (bitmap.width * factor).roundToInt()
        val height = (bitmap.height * factor).roundToInt()
        // filter = true geeft een vloeiender resultaat bij het verkleinen.
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
