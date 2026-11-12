package com.example.mobiledevelopmentproject.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Zorgt dat er minstens intervalMs tussen het begin van twee aanvragen zit.
// Komen er meerdere tegelijk, dan wachten ze netjes op hun beurt.
// now is de klok in milliseconden. Een test kan een eigen klok meegeven.
class RateLimiter(
    private val intervalMs: Long,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 }
) {

    // Een Mutex laat maar één coroutine tegelijk binnen, de rest wacht zonder de thread te blokkeren.
    private val mutex = Mutex()

    // null zolang er nog geen aanvraag geweest is, dan hoeft de eerste niet te wachten.
    private var lastRequestAt: Long? = null

    // Voert block uit zodra het mag. withLock geeft het slot ook vrij als block een fout gooit.
    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock {
        val last = lastRequestAt
        if (last != null) {
            val waitMs = intervalMs - (now() - last)
            if (waitMs > 0) {
                delay(waitMs)
            }
        }
        lastRequestAt = now()
        block()
    }
}
