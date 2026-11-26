package de.miaowz.glossa.util

import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Epoch millis of a backend timestamp. mysql2 serializes `TIMESTAMP` columns as ISO-8601 UTC
 * (`2026-11-17T14:09:42.000Z`); a plain SQL `2026-11-17 14:09:42` is read as UTC too.
 * Missing or unparsable values become 0 instead of failing the whole sync.
 */
internal fun parseBackendTimestamp(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching {
            LocalDateTime.parse(value.trim().replace(' ', 'T')).toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        .getOrElse {
            Timber.w("Unparsable backend timestamp '%s', using 0", value)
            0L
        }
}
