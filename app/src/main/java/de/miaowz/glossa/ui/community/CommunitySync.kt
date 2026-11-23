package de.miaowz.glossa.ui.community

import androidx.annotation.StringRes
import de.miaowz.glossa.R
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException

/** Why syncing with the backend failed; the screen shows [message]. */
enum class SyncError(@StringRes val message: Int) {
    /** No connection, timeout, DNS, ... */
    Network(R.string.community_error_network),

    /** The backend answered with 4xx/5xx. */
    Server(R.string.community_error_server),

    /** The body could not be decoded. */
    InvalidData(R.string.community_error_invalid_data),
}

/** Maps a sync failure to a [SyncError]. Cancellation is not a failure and is rethrown. */
fun syncErrorOf(error: Throwable): SyncError = when (error) {
    is CancellationException -> throw error
    is ResponseException -> SyncError.Server
    // kotlinx.serialization.SerializationException is an IllegalArgumentException.
    is IllegalArgumentException -> SyncError.InvalidData
    else -> SyncError.Network
}
