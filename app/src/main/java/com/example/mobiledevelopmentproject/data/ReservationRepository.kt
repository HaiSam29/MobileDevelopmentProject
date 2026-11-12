package com.example.mobiledevelopmentproject.data

import com.example.mobiledevelopmentproject.data.model.Bag
import com.example.mobiledevelopmentproject.data.model.OrderStatus
import com.example.mobiledevelopmentproject.data.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val RESERVATIONS_COLLECTION = "reservations"
private const val BAGS_COLLECTION = "bags"
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
    suspend fun reserve(bagId: String, customerId: String, quantity: Int): Reservation {
        val bagRef = firestore.collection(BAGS_COLLECTION).document(bagId)
        val reservationRef = reservations.document()

        return firestore.runTransaction { transaction ->
            val bag = transaction.get(bagRef).toObject(Bag::class.java)
                ?: throw FirebaseFirestoreException(
                    "Bag not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )

            if (!bag.active || bag.quantityLeft < quantity) {
                throw FirebaseFirestoreException(
                    "Not enough bags left",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }

            // storeName staat al in het pakket, de winkel apart lezen is niet nodig.
            val reservation = Reservation(
                bagId = bagId,
                storeId = bag.storeId,
                customerId = customerId,
                quantity = quantity,
                pickupCode = generatePickupCode(),
                status = OrderStatus.RESERVED.value,
                createdAt = System.currentTimeMillis(),
                bagTitle = bag.title,
                storeName = bag.storeName,
                pickupStart = bag.pickupStart,
                pickupEnd = bag.pickupEnd
            )

            // In een transactie komen eerst alle get's, pas daarna de schrijfacties.
            transaction.update(bagRef, "quantityLeft", bag.quantityLeft - quantity)
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
