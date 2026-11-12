package com.example.mobiledevelopmentproject.data.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// runTest gebruikt virtuele tijd: delay(1000) wacht niet echt, de test is meteen klaar.
// De limiter krijgt dezelfde virtuele klok mee.
@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {

    private fun TestScope.newLimiter() = RateLimiter(intervalMs = 1_000, now = { currentTime })

    @Test
    fun requestsAtTheSameTimeAreOneSecondApart() = runTest {
        val limiter = newLimiter()
        val startTimes = mutableListOf<Long>()

        repeat(3) {
            launch { limiter.run { startTimes.add(currentTime) } }
        }
        advanceUntilIdle()

        assertEquals(listOf(0L, 1_000L, 2_000L), startTimes)
    }

    @Test
    fun noExtraWaitWhenLastRequestIsLongAgo() = runTest {
        val limiter = newLimiter()
        limiter.run { }

        advanceTimeBy(5_000)
        val start = currentTime
        limiter.run { }

        assertEquals(start, currentTime)
    }

    @Test
    fun errorInRequestDoesNotBlockNextRequest() = runTest {
        val limiter = newLimiter()

        try {
            limiter.run { throw IllegalStateException("mislukt") }
        } catch (e: IllegalStateException) {
            // Verwacht.
        }
        val result = limiter.run { "ok" }

        assertEquals("ok", result)
        assertEquals(1_000L, currentTime)
    }
}
