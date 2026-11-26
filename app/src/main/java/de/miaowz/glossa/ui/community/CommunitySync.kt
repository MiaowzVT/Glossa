package de.miaowz.glossa.ui.community

import androidx.annotation.StringRes
import de.miaowz.glossa.R
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Why syncing with the backend failed; the screen shows [message]. */
enum class SyncError(@get:StringRes val message: Int) {
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

/**
 * Logs a sync failure of [what] (e.g. "Syncing courses"). Being offline is expected and handled
 * (warning); a failing backend or data the app cannot read needs a fix (error).
 */
fun SyncError.log(error: Throwable, what: String) {
    if (this == SyncError.Network) Timber.w(error, "%s failed (%s)", what, this)
    else Timber.e(error, "%s failed (%s)", what, this)
}
