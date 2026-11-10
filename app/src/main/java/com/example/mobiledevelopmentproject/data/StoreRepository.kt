package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.model.Store
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val STORES_COLLECTION = "stores"
private const val SAVE_TIMEOUT_MS = 10_000L

// Elke handelaar heeft één winkel. Het document-id is daarom de uid van de eigenaar,
// zo vind je de eigen winkel zonder te zoeken.
// Bij een fout gooit elke functie een FirebaseException, de ViewModel vangt die op.
class StoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val stores = firestore.collection(STORES_COLLECTION)

    // Maakt de winkel aan of overschrijft ze.
    suspend fun saveStore(store: Store) {
        // Zonder internet blijft set().await() wachten, zie AuthRepository.
        val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
            stores.document(store.ownerId).set(store).await()
            true
        }

        if (saved == null) {
            throw FirebaseNetworkException("Saving the store timed out")
        }
    }

    // Geeft null als de handelaar nog geen winkel heeft.
    suspend fun getStore(storeId: String): Store? {
        return stores.document(storeId).get().await().toObject(Store::class.java)
    }

    // Geeft een nieuwe lijst telkens er in Firestore een winkel bijkomt of verandert.
    fun observeStores(): Flow<List<Store>> {
        return stores.snapshots().map { snapshot ->
            snapshot.toObjects(Store::class.java)
        }
    }
}
