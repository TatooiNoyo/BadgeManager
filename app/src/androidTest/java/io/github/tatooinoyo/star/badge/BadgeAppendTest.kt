package io.github.tatooinoyo.star.badge

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.tatooinoyo.star.badge.data.AppDatabase
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BadgeAppendTest {
    private lateinit var database: AppDatabase

    @Before fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java,
        ).build()
    }

    @After fun closeDatabase() { database.close() }

    @Test fun appendAllocatesFromDatabaseAndPreservesContent() = runBlocking {
        val dao = database.badgeDao()
        val badge = Badge(
            title = "new", remark = "remark", link = "https://example.com/badge",
            channel = BadgeChannel.CUSTOM, orderIndex = -1, tags = listOf("tag"),
            channelPackageName = "example.game", channelActivityName = "example.game.Main",
            channelLabel = "Game",
        )
        dao.appendBadge(badge)
        assertEquals(listOf(badge.copy(orderIndex = 1)), dao.getAllBadges4export())

        dao.insertBadge(badge.copy(id = "highest", orderIndex = 20))
        val next = badge.copy(id = "next")
        dao.appendBadge(next)
        assertEquals(next.copy(orderIndex = 21), dao.getAllBadges4export().single { it.id == "next" })
    }

    @Test fun concurrentAppendsHaveUniqueContinuousOrder() = runBlocking {
        val dao = database.badgeDao()
        val start = CompletableDeferred<Unit>()
        val badges = List(64) { index ->
            Badge(id = "badge-$index", title = "Badge $index", remark = "", orderIndex = 0)
        }
        val jobs = badges.map { badge ->
            launch(Dispatchers.Default) {
                start.await()
                dao.appendBadge(badge)
            }
        }
        start.complete(Unit)
        jobs.joinAll()

        val result = dao.getAllBadges4export()
        assertEquals(badges.size, result.size)
        assertEquals(badges.map { it.id }.toSet(), result.map { it.id }.toSet())
        assertEquals((1..badges.size).toList(), result.map { it.orderIndex }.sorted())
    }

    @Test fun concurrentSingleAndBatchAppendsShareOrderSequence() = runBlocking {
        val dao = database.badgeDao()
        val original = Badge(id = "original", title = "Original", remark = "", orderIndex = 7)
        dao.insertBadge(original)
        val start = CompletableDeferred<Unit>()
        val jobs = (0 until 32).map { index ->
            launch(Dispatchers.Default) {
                start.await()
                if (index % 2 == 0) {
                    dao.appendBadge(original.copy(id = "single-$index", title = "Single $index"))
                } else {
                    dao.appendCopies(List(3) { original.copy(title = "Batch $index/$it") })
                }
            }
        }
        start.complete(Unit)
        jobs.joinAll()

        val result = dao.getAllBadges4export()
        assertEquals(65, result.size)
        assertEquals(65, result.map { it.id }.toSet().size)
        assertEquals((7..71).toList(), result.map { it.orderIndex }.sorted())
        assertEquals(original, result.single { it.id == original.id })
    }

    @Test fun exhaustedOrderDoesNotInsertOrOverflow() = runBlocking {
        val dao = database.badgeDao()
        val original = Badge(title = "Last", remark = "", orderIndex = Int.MAX_VALUE)
        dao.insertBadge(original)

        try {
            dao.appendBadge(original.copy(id = "new"))
            fail("Expected order capacity failure")
        } catch (error: IllegalArgumentException) {
            assertEquals("Badge order is full", error.message)
        }
        assertEquals(listOf(original), dao.getAllBadges4export())
    }
}
