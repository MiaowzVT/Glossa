package de.miaowz.glossa.ui.community

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class SyncErrorTest {

    @Test
    fun connectionProblemsAreNetworkErrors() {
        assertEquals(SyncError.Network, syncErrorOf(IOException("unreachable")))
        assertEquals(SyncError.Network, syncErrorOf(SocketTimeoutException("timeout")))
    }

    @Test
    fun undecodableBodiesAreInvalidData() {
        assertEquals(SyncError.InvalidData, syncErrorOf(SerializationException("bad json")))
        assertEquals(SyncError.InvalidData, syncErrorOf(IllegalArgumentException("Unexpected course JSON")))
    }

    @Test
    fun cancellationIsRethrown() {
        assertThrows(CancellationException::class.java) { syncErrorOf(CancellationException("left screen")) }
    }

    @Test
    fun everyErrorHasAMessage() {
        SyncError.entries.forEach { assert(it.message != 0) }
    }
}
