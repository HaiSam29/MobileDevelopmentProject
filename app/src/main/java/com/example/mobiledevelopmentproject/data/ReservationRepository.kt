package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.model.FoodPackage
import com.example.mobiledevelopmentproject.data.model.OrderStatus
import com.example.mobiledevelopmentproject.data.model.Reservation
import com.example.mobiledevelopmentproject.data.model.Store
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val RESERVATIONS_COLLECTION = "reservations"
private const val PACKAGES_COLLECTION = "packages"
private const val STORES_COLLECTION = "stores"
private const val PICKUP_CODE_LENGTH = 6

// Zonder 0/O en 1/I, die lijken te veel op elkaar als je de code in de winkel toont.
private const val PICKUP_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

// Bij een fout gooit elke functie een FirebaseException, de ViewModel vangt die op.
class ReservationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val reservations = firestore.collection(RESERVATIONS_COLLECTION)

    // Verlaagt het aantal van het pakket en maakt de reservatie aan, allebei of geen van beide.
    // In een transactie, zodat twee klanten niet tegelijk het laatste pakket krijgen.
    // Een transactie werkt niet offline: zonder internet faalt ze met code UNAVAILABLE.
    suspend fun reserve(packageId: String, customerId: String, quantity: Int): Reservation {
        val packageRef = firestore.collection(PACKAGES_COLLECTION).document(packageId)
        val reservationRef = reservations.document()

        return firestore.runTransaction { transaction ->
            val foodPackage = transaction.get(packageRef).toObject(FoodPackage::class.java)
                ?: throw FirebaseFirestoreException(
                    "Package not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )

            if (foodPackage.quantityAvailable < quantity) {
                throw FirebaseFirestoreException(
                    "Not enough packages left",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }

            val storeRef = firestore.collection(STORES_COLLECTION).document(foodPackage.storeId)
            val store = transaction.get(storeRef).toObject(Store::class.java)

            val reservation = Reservation(
                packageId = packageId,
                storeId = foodPackage.storeId,
                customerId = customerId,
                packageTitle = foodPackage.title,
                storeName = store?.name.orEmpty(),
                quantity = quantity,
                pickupCode = generatePickupCode(),
                pickupFrom = foodPackage.pickupFrom,
                pickupUntil = foodPackage.pickupUntil,
                status = OrderStatus.RESERVED,
                createdAt = System.currentTimeMillis()
            )

            // In een transactie komen eerst alle get's, pas daarna de schrijfacties.
            transaction.update(packageRef, "quantityAvailable", foodPackage.quantityAvailable - quantity)
            transaction.set(reservationRef, reservation)

            reservation.copy(id = reservationRef.id)
        }.await()
    }

    // Nieuwste eerst, gesorteerd in Kotlin om een extra index in Firestore te vermijden.
    fun observeReservationsForCustomer(customerId: String): Flow<List<Reservation>> {
        return reservations
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Reservation::class.java).sortedByDescending { it.createdAt }
            }
    }

    // Niet gegarandeerd uniek, maar de winkel ziet de code altijd samen met het pakket.
    private fun generatePickupCode(): String {
        return (1..PICKUP_CODE_LENGTH)
            .map { PICKUP_CODE_CHARS.random() }
            .joinToString("")
    }
}
