package io.github.tatooinoyo.star.badge

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import io.github.tatooinoyo.star.badge.data.AppDatabase
import io.github.tatooinoyo.star.badge.data.Badge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BadgeRestoreTest {
    private lateinit var database: AppDatabase
    private val original = Badge("original", "old", "", orderIndex = 1)

    @Before fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java,
        ).build()
        runBlocking { database.badgeDao().insertBadge(original) }
    }

    @After fun closeDatabase() { database.close() }

    @Test fun failedReplacementRollsBackDeleteAndEarlierInserts() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_bad BEFORE INSERT ON badges WHEN NEW.id = 'bad' BEGIN SELECT RAISE(ABORT, 'test failure'); END",
        )
        val dao = database.badgeDao()
        try {
            dao.replaceAll(listOf(original.copy(id = "first"), original.copy(id = "bad")))
            fail("Expected insertion failure")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(listOf(original), dao.getAllBadges4export())
    }

    @Test fun cancellationRollsBackTransaction() = runBlocking {
        val deleted = CompletableDeferred<Unit>()
        val job = launch {
            database.withTransaction {
                database.badgeDao().deleteAll()
                deleted.complete(Unit)
                awaitCancellation()
            }
        }
        deleted.await()
        job.cancelAndJoin()
        assertEquals(listOf(original), database.badgeDao().getAllBadges4export())
    }

    @Test fun replacementAndAppendAreCompleteAndOrdered() = runBlocking {
        val dao = database.badgeDao()
        val replacement = listOf(original.copy(id = "a"), original.copy(id = "b", orderIndex = 2))
        dao.replaceAll(replacement)
        assertEquals(replacement.toSet(), dao.getAllBadges4export().toSet())
        dao.appendCopies(replacement)
        val result = dao.getAllBadges4export()
        assertEquals(4, result.size)
        assertEquals(4, result.map { it.id }.toSet().size)
        assertEquals(listOf(1, 2, 3, 4), result.map { it.orderIndex }.sorted())
    }
}
