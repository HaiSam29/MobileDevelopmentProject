package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.model.FoodPackage
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val PACKAGES_COLLECTION = "packages"
private const val SAVE_TIMEOUT_MS = 10_000L

// Bij een fout gooit elke functie een FirebaseException, de ViewModel vangt die op.
class PackageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val packages = firestore.collection(PACKAGES_COLLECTION)

    // Geeft het id terug dat Firestore aan het nieuwe pakket gaf.
    suspend fun addPackage(foodPackage: FoodPackage): String {
        // document() zonder naam maakt een nieuw document met een willekeurig id.
        val document = packages.document()

        // Zonder internet blijft set().await() wachten, zie AuthRepository.
        val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
            document.set(foodPackage).await()
            true
        }

        if (saved == null) {
            throw FirebaseNetworkException("Saving the package timed out")
        }
        return document.id
    }

    // Pakketten die nog niet uitverkocht zijn, voor de klant.
    // Sorteren op afstand gebeurt in de ViewModel, want daarvoor is de locatie nodig.
    fun observeAvailablePackages(): Flow<List<FoodPackage>> {
        return packages
            .whereGreaterThan("quantityAvailable", 0)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(FoodPackage::class.java) }
    }

    // Alle pakketten van één winkel, ook uitverkochte, voor de handelaar.
    // Gesorteerd in Kotlin, want orderBy op een ander veld dan de filter vraagt een extra index in Firestore.
    fun observePackagesForStore(storeId: String): Flow<List<FoodPackage>> {
        return packages
            .whereEqualTo("storeId", storeId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FoodPackage::class.java).sortedByDescending { it.createdAt }
            }
    }

    // Eén pakket, zodat "Nog 3 over" op het detailscherm meteen bijwerkt.
    // Geeft null als het pakket verwijderd is.
    fun observePackage(packageId: String): Flow<FoodPackage?> {
        return packages.document(packageId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(FoodPackage::class.java) }
    }
}
