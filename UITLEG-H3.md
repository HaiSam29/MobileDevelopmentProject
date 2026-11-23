# Uitleg H3: Pakket aanmaken

Branch: `feature/pakket-aanmaken` (niets gecommit of gepusht).

## 1. Overzicht

**Nieuw**
- `ui/bag/CreateBagViewModel.kt`: houdt het formulier bij (`CreateBagUiState`), laadt de winkel van de handelaar, controleert de velden en bewaart het pakket via `BagRepository`.
- `ui/bag/CreateBagScreen.kt`: het scherm "Nieuw pakket". Toont laden, een fout, "eerst een winkel maken" of het formulier, met twee uurvakken die een klok (`TimePicker`) openen.
- `res/drawable/ic_clock.xml`: klokicoon voor de uurvakken, zelfde stijl als `ic_back.xml`.

**Gewijzigd**
- `res/values/strings.xml`: blok `<!-- Pakket aanmaken (H3) -->` en 9 nieuwe foutmeldingen. `button_retry`, `button_my_store` en `action_back` worden hergebruikt.
- `ui/navigation/AppNavHost.kt`: route `CREATE_BAG`, de knop "Pakket aanmaken" navigeert er nu naartoe, en een `composable` voor het nieuwe scherm.

Niet aangeraakt: `ui/auth`, `ui/map`, `data/location`, `GeocodingRepository`, `BagRepository`, `Bag.kt`, `Store.kt`.

```
On branch feature/pakket-aanmaken
Changes not staged for commit:
	modified:   app/src/main/java/com/example/mobiledevelopmentproject/ui/navigation/AppNavHost.kt
	modified:   app/src/main/res/values/strings.xml

Untracked files:
	app/src/main/java/com/example/mobiledevelopmentproject/ui/bag/
	app/src/main/res/drawable/ic_clock.xml
```
(`UITLEG-H3.md` zelf staat er nu ook bij als untracked.)

## 2. Hoe het samenwerkt

```
MerchantHomeScreen        tik op "Pakket aanmaken" -> onCreateBagClick()
        |
        v
AppNavHost                navController.navigate(Routes.CREATE_BAG)
        |
        v
CreateBagScreen           viewModel() maakt CreateBagViewModel
        |                 toont uiState, elke toetsaanslag -> viewModel.onXxxChange()
        |                 tik op "Pakket plaatsen" -> viewModel.save()
        v
CreateBagViewModel        init: loadStore() -> StoreRepository.getStore(uid)
        |                 save(): validate() -> Bag maken -> isSaving = true
        v
BagRepository.addBag(bag) document.set(bag) in collectie "bags"
        |
        v
Firestore                 nieuw document met willekeurig id
        |
        v (terug)
CreateBagViewModel        isSaved = true
CreateBagScreen           LaunchedEffect ziet isSaved -> Toast + onBagCreated()
AppNavHost                navigateUp() -> terug naar het startscherm van de handelaar
```

## 3. Leesvolgorde

1. **`AppNavHost.kt`** (kort)
   - r. 28: de route `CREATE_BAG = "create_bag"`.
   - r. 80: de knop op het startscherm navigeert naar die route.
   - r. 90-102: het scherm in de navigatie. Na opslaan en bij Terug: `navigateUp()`. "Mijn winkel" haalt dit scherm eerst van de backstack (`popUpTo ... inclusive = true`).

2. **`CreateBagViewModel.kt`** (het belangrijkste)
   - r. 22-50: `CreateBagUiState`, alles wat het scherm toont: de velden als tekst, de uren als minuten na middernacht, laden/opslaan en de foutmeldingen (`@StringRes Int?`).
   - r. 64-90: `init { loadStore() }` en `loadStore()`: zoekt de winkel met de uid van de ingelogde handelaar. Geen winkel -> `hasNoStore = true`.
   - r. 101-127: `onPriceChange` (wist beide prijsfouten) en `onPickupStartChange` (`hour * 60 + minute`).
   - r. 129-181: `save()`: controleren, een `Bag` maken met de gegevens van de winkel, dan in `viewModelScope.launch` `addBag()` oproepen.
   - r. 186-200: `parseAmount()` (aanvaardt `4,50` en `4.50`) en `todayAt()` (maakt een `Timestamp` van vandaag op het gekozen uur).
   - r. 202-237: `validate()` en `hasFieldErrors()`: per veld een foutmelding of `null`.

3. **`CreateBagScreen.kt`**
   - r. 77: `collectAsStateWithLifecycle()`: het scherm volgt de state van de ViewModel.
   - r. 83-88: `LaunchedEffect(uiState.isSaved)`: na opslaan een Toast en terug.
   - r. 101-126: `when`: laden / laadfout + "Opnieuw proberen" / geen winkel + "Mijn winkel" / formulier.
   - r. 184-300: `CreateBagForm`: de velden van boven naar onder. Bovenaan (r. 197) staat de plek waar in H4 het fotovak komt.
   - r. 342-418: `TimeField`: het uurvak en de `AlertDialog` met `TimePicker`. `showPicker` staat in `rememberSaveable` (r. 351).

4. **`strings.xml`** en **`ic_clock.xml`**: enkel teksten en het icoon.

## 4. Begrippen

- **`StateFlow` en `collectAsStateWithLifecycle`**: de ViewModel heeft een `MutableStateFlow` (enkel zij mag die wijzigen) en geeft een `StateFlow` naar buiten. Het scherm "abonneert" zich met `collectAsStateWithLifecycle()`: telkens de state verandert, tekent Compose het scherm opnieuw. "WithLifecycle" betekent dat het stopt met luisteren als de app op de achtergrond staat.
- **Velden in de ViewModel (rotatie, G2)**: bij een rotatie wordt de Activity en dus het scherm opnieuw gemaakt, maar de ViewModel blijft bestaan. Omdat alle ingevulde waarden in `CreateBagUiState` staan, blijven ze staan. Enkel "staat de klok open" is iets van het scherm; dat staat in `rememberSaveable`, dat ook een rotatie overleeft.
- **`@StringRes` foutmeldingen**: de ViewModel kent geen `Context` en dus geen teksten. Hij bewaart het id van de tekst (`R.string.error_price_invalid`). Het scherm zet dat id om met `stringResource()`. `@StringRes` laat Android Studio controleren dat het echt een string-id is.
- **`viewModelScope.launch`**: start een coroutine voor werk dat even duurt (Firestore). De app blijft intussen reageren. Wordt de ViewModel opgeruimd (scherm definitief weg), dan stopt de coroutine vanzelf.
- **`Timestamp` van vandaag**: `Calendar.getInstance()` is "nu". We zetten het uur en de minuten op het gekozen uur en seconden/milliseconden op 0. `Timestamp(calendar.time)` is het type dat Firestore als datum+tijd bewaart.
- **`storeName` en `location` kopiëren**: Firestore heeft geen "join" zoals SQL. Als de klant een lijst pakketten ziet, wil je de winkelnaam en de afstand tonen zonder voor elk pakket nog de winkel op te vragen. Daarom staan die al in het pakket zelf (staat ook zo in de commentaar van `Bag.kt`).

## 5. Keuzes die niet letterlijk in de opgave staan

- **Prijsfout pas als de originele waarde geldig is**: "prijs moet lager zijn" wordt enkel getoond als beide getallen geldig zijn, anders zou je twee verwarrende fouten krijgen.
- **Het huidige uur wordt in `save()` bepaald** en aan `validate(nowMinutes)` meegegeven, zodat `validate()` zelf geen klok leest (eenvoudiger te testen).
- **Toast in plaats van Snackbar**: na opslaan gaat het scherm meteen weg, een Snackbar zou mee verdwijnen.
- **Klok opent op het eerder gekozen uur**, of 17:00 als er nog niets gekozen is.
- **Eén foutmelding onder beide uurvakken**, en bij een fout krijgen beide vakken een rode rand, want de fout gaat over het venster als geheel.
- **`popUpTo(CREATE_BAG)` bij "Mijn winkel"**: na het maken van de winkel brengt Terug je naar het startscherm, niet naar het lege "geen winkel"-scherm. Daarna tik je gewoon opnieuw op "Pakket aanmaken".
- **`hasNoStore`-melding is gewone tekst** (geen rode foutmelding), want het is geen fout maar een stap die nog moet gebeuren.
- **Vangnet in `save()`** als `store` toch `null` zou zijn: algemene foutmelding in plaats van een crash.
- **`ImeAction.Next`** bij de velden, zodat je met het toetsenbord naar het volgende veld springt; bij "Aantal" `Done`.

## 6. Testen in de emulator

1. Start de app en log in als **handelaar die al een winkel heeft**.
2. Tik op **"Pakket aanmaken"**. Je ziet kort een laadcirkel, dan het formulier "Nieuw pakket".
3. Tik meteen op **"Pakket plaatsen"**. Onder elk veld verschijnt een rode melding, ook "Kies een begin- en einduur."
4. Vul **prijs 14** en **originele waarde 12** in en tik op "Pakket plaatsen": onder Prijs staat "De prijs moet lager zijn dan de originele waarde."
5. Typ iets in Prijs: de melding verdwijnt meteen.
6. Kies **Van 18:00** en **Tot 17:00**: "Het einduur moet na het beginuur liggen." Kies een einduur dat al voorbij is: "Het einduur is vandaag al voorbij."
7. Vul alles correct in, bv. titel "Verrassingspakket brood", een beschrijving, prijs `4,50`, waarde `12,00`, aantal `5`, en een venster dat nog moet komen.
8. **Draai het scherm** (rotatieknop in de emulator) terwijl je invult, en ook terwijl de klok open staat: alles blijft staan.
9. Tik op **"Pakket plaatsen"**: de knop toont een laadcirkel, dan de Toast "Je pakket is geplaatst." en je bent terug op het startscherm.
10. Open de **Firebase console** -> Firestore -> collectie **`bags`**. Het nieuwe document heeft:
    - `storeId` = uid van de handelaar (zelfde als het id van zijn document in `stores`),
    - `storeName` en `location` gelijk aan de winkel,
    - `price` 4.5, `originalValue` 12, `quantityTotal` en `quantityLeft` 5,
    - `pickupStart` en `pickupEnd` op de datum van vandaag met de gekozen uren,
    - `active` true, `photoBase64` leeg.
11. Log in als **handelaar zonder winkel** (of maak een nieuwe handelaar) en tik op "Pakket aanmaken": je ziet "Maak eerst je winkel aan, dan kan je een pakket plaatsen." en de knop **"Mijn winkel"**, die naar het winkelscherm gaat.
12. Extra: zet de emulator in vliegtuigmodus en tik op "Pakket plaatsen": na ongeveer 10 seconden "Geen internetverbinding. Probeer het opnieuw."

## 7. Commit

Voorstel:

```
Voeg scherm Pakket aanmaken toe (H3)
```
