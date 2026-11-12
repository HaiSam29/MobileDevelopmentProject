package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.model.Bag
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val BAGS_COLLECTION = "bags"
private const val SAVE_TIMEOUT_MS = 10_000L

// Bij een fout gooit elke functie een FirebaseException, de ViewModel vangt die op.
class BagRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bags = firestore.collection(BAGS_COLLECTION)

    // Geeft het id terug dat Firestore aan het nieuwe pakket gaf.
    suspend fun addBag(bag: Bag): String {
        // document() zonder naam maakt een nieuw document met een willekeurig id.
        val document = bags.document()

        // Zonder internet blijft set().await() wachten, zie AuthRepository.
        val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
            document.set(bag).await()
            true
        }

        if (saved == null) {
            throw FirebaseNetworkException("Saving the bag timed out")
        }
        return document.id
    }

    // Actieve pakketten die nog niet uitverkocht zijn, voor de klant.
    // active wordt in Kotlin gefilterd: een tweede filter in Firestore vraagt een extra index.
    // Sorteren op afstand gebeurt in de ViewModel, want daarvoor is de locatie nodig.
    fun observeAvailableBags(): Flow<List<Bag>> {
        return bags
            .whereGreaterThan("quantityLeft", 0)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Bag::class.java).filter { it.active } }
    }

    // Alle pakketten van één winkel, ook uitverkochte, voor de handelaar.
    // Gesorteerd in Kotlin, want orderBy op een ander veld dan de filter vraagt een extra index in Firestore.
    fun observeBagsForStore(storeId: String): Flow<List<Bag>> {
        return bags
            .whereEqualTo("storeId", storeId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Bag::class.java).sortedByDescending { it.createdAt }
            }
    }

    // Eén pakket, zodat "Nog 3 over" op het detailscherm meteen bijwerkt.
    // Geeft null als het pakket verwijderd is.
    fun observeBag(bagId: String): Flow<Bag?> {
        return bags.document(bagId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(Bag::class.java) }
    }
}
