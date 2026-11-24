# Uitleg H4: Foto bij een pakket (Base64)

Branch: `feature/foto-pakket` (niets gecommit of gepusht). `./gradlew assembleDebug` slaagt.

## 1. Overzicht

**Nieuw**
- `data/PhotoEncoder.kt`: zet een foto (Bitmap of Uri) om naar Base64-tekst: verkleinen tot max 800 px, JPEG 70. Kan Base64 ook terug omzetten naar een Bitmap om te tonen (later ook voor K3).
- `res/drawable/ic_camera.xml`: camera-icoon, zelfde stijl als `ic_clock.xml`.
- `res/drawable/ic_gallery.xml`: afbeelding-icoon (kader, bergen, zon).

**Gewijzigd**
- `ui/bag/CreateBagViewModel.kt`: state voor de foto (`photoBase64`, `isProcessingPhoto`, `photoError`), functies `onPhotoTaken`, `onPhotoPicked`, `onCameraUnavailable`, de foto is verplicht in `validate()`, en een `Factory` die de `ContentResolver` meegeeft.
- `ui/bag/CreateBagScreen.kt`: nieuw onderdeel `PhotoPicker` bovenaan het formulier (vak met stippellijn, knoppen Camera en Galerij), de ViewModel wordt gemaakt met de `Factory`.
- `res/values/strings.xml`: 5 teksten in het blok Pakket aanmaken, 3 foutmeldingen.

Niet aangeraakt: `ui/auth`, `ui/map`, `data/location`, `GeocodingRepository`, `AppNavHost.kt`, `Bag.kt`, `BagRepository.kt`, `AndroidManifest.xml`.

```
On branch feature/foto-pakket
Changes not staged for commit:
	modified:   app/src/main/java/com/example/mobiledevelopmentproject/ui/bag/CreateBagScreen.kt
	modified:   app/src/main/java/com/example/mobiledevelopmentproject/ui/bag/CreateBagViewModel.kt
	modified:   app/src/main/res/values/strings.xml

Untracked files:
	app/src/main/java/com/example/mobiledevelopmentproject/data/PhotoEncoder.kt
	app/src/main/res/drawable/ic_camera.xml
	app/src/main/res/drawable/ic_gallery.xml
```
(`UITLEG-H4.md` zelf staat er nu ook bij als untracked.)

```
 .../ui/bag/CreateBagScreen.kt                      | 183 ++++++++++++++++++++-
 .../ui/bag/CreateBagViewModel.kt                   |  81 ++++++++-
 app/src/main/res/values/strings.xml                |   8 +
 3 files changed, 263 insertions(+), 9 deletions(-)
```
(`git diff --stat` toont enkel gewijzigde bestanden, geen nieuwe.)

## 2. Hoe het samenwerkt

```
PhotoPicker (scherm)        tik op "Camera"                 tik op "Galerij"
        |                         |                               |
        v                         v                               v
launcher                  cameraLauncher.launch(null)     galleryLauncher.launch(ImageOnly)
        |                         |                               |
        v                         v                               v
andere app                camera-app neemt foto           fotokiezer van Android
        |                  -> Bitmap? terug                -> Uri? terug
        |                         |                               |
        v                         v                               v
CreateBagViewModel        onPhotoTaken(bitmap)            onPhotoPicked(uri)
        |                         \_______________  ______________/
        |                                         \/
        |                              processPhoto { ... }
        |                   isProcessingPhoto = true (scherm toont laden)
        |                   withContext(Dispatchers.Default) -> achtergrond
        v                                         |
PhotoEncoder              encodeBitmap / encodeUri: verkleinen, JPEG 70, Base64
        |                                         |
        v                                         v
CreateBagUiState          photoBase64 = "…lange tekst…"  (of photoError)
        |                 -> scherm: decode() -> Image in het vak
        v
save()                    validate(): foto leeg? -> "Voeg een foto toe."
        |                 Bag(photoBase64 = state.photoBase64, ...)
        v
BagRepository.addBag()    document.set(bag)
        v
Firestore                 bags/{id}.photoBase64 = "/9j/4AAQSk…"
```

## 3. Leesvolgorde

**1. `data/PhotoEncoder.kt`** (los van de rest, begin hier)
- r. 15-23: commentaar over Base64 en de 1 MB-limiet.
- r. 26-27: `MAX_SIDE_PX = 800`, `JPEG_QUALITY = 70`.
- r. 30 `encodeBitmap`: `scaleDown`, dan `compress(JPEG, 70)` in een `ByteArrayOutputStream`, dan `Base64.encodeToString(..., NO_WRAP)`.
- r. 40 `encodeUri`: kiest `ImageDecoder` (Android 9+) of `BitmapFactory` (ouder), vangt fouten op en geeft dan `null`.
- r. 61 `decode`: Base64 -> bytes -> `Bitmap`, `null` bij lege/kapotte tekst.
- r. 74 `decodeWithImageDecoder`: `ALLOCATOR_SOFTWARE` en `setTargetSize` als de foto groter is dan 800 px.
- r. 91 `decodeWithBitmapFactory`: eerst `inJustDecodeBounds` (enkel afmetingen), dan `inSampleSize` (1, 2, 4, 8 …).
- r. 110 `scaleDown`: `createScaledBitmap` met dezelfde verhouding.

**2. `ui/bag/CreateBagViewModel.kt`**
- r. 41-46 en 63: nieuwe velden in `CreateBagUiState`.
- r. 70: de constructor krijgt `contentResolver` als eerste parameter.
- r. 148 `onPhotoTaken`, r. 153 `onPhotoPicked`, r. 157 `onCameraUnavailable`.
- r. 162 `processPhoto`: laden aan, r. 169 `withContext(Dispatchers.Default)`, daarna foto of fout in de state.
- r. 181 `save()`: stopt als `isProcessingPhoto`; r. 219 de foto gaat mee in de `Bag`.
- r. 235-247: `companion object` met de `Factory`.
- r. 298: `photoError` in `validate()`, en `photoError` in `hasFieldErrors()` (r. 302).

**3. `ui/bag/CreateBagScreen.kt`**
- r. 95: `viewModel(factory = CreateBagViewModel.Factory)`.
- r. 217-219: label "Foto" + `PhotoPicker` bovenaan het formulier.
- r. 318: "Pakket plaatsen" toont laden tijdens `isProcessingPhoto`.
- r. 338 `PhotoPicker`:
  - r. 343: camera-launcher (`TakePicturePreview`), met de uitleg over de toestemming.
  - r. 352: galerij-launcher (`PickVisualMedia`).
  - r. 362-363: `launch(null)` in `try/catch (ActivityNotFoundException)`.
  - r. 373: `remember(uiState.photoBase64) { decode(...) }`.
  - r. 393-405: de stippellijn met `drawBehind` en `dashPathEffect`.
  - r. 416: de foto met `ContentScale.Crop`.
- r. 455 `PhotoButtons` en r. 476 `PhotoButton`: de twee omlijnde knoppen met icoon.

## 4. Begrippen

- **Base64**: een foto is een rij bytes. Firestore bewaart hier tekst, dus schrijven we de bytes als gewone tekens (A-Z, a-z, 0-9, + en /). Elke 3 bytes worden 4 tekens, de tekst is dus ± 33% groter dan de JPEG.
- **800 px en JPEG 70**: een Firestore-document mag max 1 MB zijn. Een gsm-foto is vaak 3-5 MB. Na verkleinen en comprimeren is ze 50-150 KB, als Base64 ± 70-200 KB: ruim onder de limiet, met plaats voor de andere velden. Op een gsm-scherm zie je het verschil niet.
- **Activity Result API / `rememberLauncherForActivityResult`**: de manier om een ander scherm (camera-app, fotokiezer) te openen en een resultaat terug te krijgen. Je kiest een "contract" (`TakePicturePreview`, `PickVisualMedia`) dat zegt wat je meegeeft en wat je terugkrijgt. `remember...` zorgt dat de launcher een rotatie overleeft, zodat het resultaat toch aankomt.
- **`Bitmap` vs `Uri`**: een `Bitmap` is de foto zelf (pixels in het geheugen). Een `Uri` is enkel een adres naar de foto (zoals een link); we lezen ze zelf in met de `ContentResolver`.
- **Geen CAMERA-toestemming**: niet onze app gebruikt de camera, maar de camera-app van het toestel. Opgelet: als je `CAMERA` wél in de manifest zet, verplicht Android ze ook voor deze manier. Daarom staat ze er niet in. De fotokiezer is ook van het systeem: we krijgen enkel de ene gekozen foto, dus ook daar geen toestemming.
- **`Dispatchers.Default` en `withContext`**: de main thread tekent het scherm. Zwaar rekenwerk (foto verkleinen, comprimeren) daarop laten lopen doet het scherm haperen. `withContext(Dispatchers.Default) { ... }` voert dat blok uit op een achtergrondthread voor rekenwerk en gaat daarna terug naar de main thread.
- **`Factory` en `ContentResolver`**: om een `Uri` te lezen is een `ContentResolver` nodig, en die krijg je via een Context. Een ViewModel mag geen Activity bijhouden: bij een rotatie wordt de Activity vernietigd, maar de ViewModel blijft, en houdt dan de oude Activity vast (geheugenlek). De Application leeft zo lang als de app, dus haar `ContentResolver` is veilig. Een ViewModel met een constructorparameter kan Android niet zelf maken, daarom de `Factory` (zelfde patroon als `MyStoreViewModel`).

## 5. Keuzes die niet in de opgave stonden

- **Foto verplicht**: de klant ziet in de lijst en op het detailscherm een foto. Zonder foto geeft `validate()` "Voeg een foto toe." en wordt de stippellijn rood.
- **Knoppen in het vak of eronder**: zonder foto staan tekst en knoppen ín het vak, zoals in het ontwerp. Met een foto vult de foto het hele vak en staan dezelfde knoppen eronder, zodat ze de foto niet bedekken. Ze vervangen dan de foto.
- **`ImageDecoder` (Android 9+) en `BitmapFactory` (ouder)**: `ImageDecoder` kan meteen op de juiste grootte inlezen en draait de foto rechtop. Op Android 7-8 bestaat het niet (minSdk is 24), daarom `BitmapFactory` met `inSampleSize`. Die draait de foto niet rechtop; op oude toestellen kan een galerijfoto dus soms gedraaid staan (aanvaardbaar voor een kleine groep toestellen).
- **Mislukte nieuwe foto**: de vorige foto blijft staan, met de foutmelding eronder.
- **Fouten opvangen in `encodeUri`**: `IOException` (bestand niet leesbaar, geen geldige afbeelding), `SecurityException` (geen toegang) en `IllegalArgumentException`. Andere fouten vangen we niet, zoals in de rest van het project.
- **Stippellijn**: getekend ná de achtergrond en een halve lijndikte naar binnen, anders valt de helft van de lijn buiten het vak of onder de achtergrond.
- **`TakePicturePreview` geeft een kleine foto** (vaak ± 200-400 px breed). Dat is genoeg voor een lijstje en kan zonder bestand of `FileProvider`. Voor een scherpere foto zou je `TakePicture` met een `FileProvider` gebruiken, maar dat vraagt veel meer code.

## 6. Testen in de emulator

1. **Een foto in de galerij krijgen**: open in de emulator de app *Camera* en neem een foto (de emulator toont een virtuele kamer). Of sleep een `.jpg` van je pc naar het emulatorscherm, dan komt die in *Downloads* en in de fotokiezer.
2. **Zonder foto**: log in als handelaar met een winkel -> "Pakket aanmaken" -> vul alles in behalve de foto -> "Pakket plaatsen". Je ziet "Voeg een foto toe." en de stippellijn wordt rood.
3. **Camera**: tik "Camera" -> neem een foto -> tik op het vinkje. Even een laadcirkel, daarna staat de foto in het vak en verdwijnt de fout.
4. **Galerij**: tik "Galerij" (nu onder het vak) -> kies een andere foto. De foto wordt vervangen.
5. **Rotatie**: draai het scherm (knop in de zijbalk van de emulator). De foto en de andere velden blijven staan.
6. **Opslaan**: "Pakket plaatsen" -> melding "Je pakket is geplaatst." -> Firebase console -> Firestore -> `bags` -> het nieuwste document. `photoBase64` is een lange tekst die begint met `/9j/` (zo begint elke JPEG in Base64).
7. **Grootte controleren**: Base64 heeft ± 4 tekens per 3 bytes, dus `aantal tekens × 0,75 = bytes`. 100.000 tekens ≈ 75 KB. Aantal tekens tellen: in Android Studio een breakpoint in `save()` en `state.photoBase64.length` bekijken, of tijdelijk `Log.d("Foto", "${state.photoBase64.length}")` toevoegen en in Logcat kijken. Alles onder ± 1.300.000 tekens past (1 MB), normaal zit je onder 200.000.

## 7. Commit

Voorstel:

```
Voeg foto toe aan pakket als Base64 (H4)
```
