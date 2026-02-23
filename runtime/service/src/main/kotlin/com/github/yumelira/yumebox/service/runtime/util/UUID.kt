package com.github.yumelira.yumebox.service.runtime.util

import android.content.Intent
import android.os.Bundle
import java.util.UUID

/**
 * UUID utilities for Intent and Bundle
 */

/**
 * Get UUID from Bundle
 */
fun Bundle.getUUID(key: String): UUID? {
    return getString(key)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}

/**
 * Put UUID into Bundle
 */
fun Bundle.putUUID(key: String, uuid: UUID) {
    putString(key, uuid.toString())
}

/**
 * Get UUID from Intent extras
 */
fun Intent.getUUID(key: String): UUID? {
    return getStringExtra(key)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}

/**
 * Put UUID into Intent extras
 */
fun Intent.putUUID(key: String, uuid: UUID): Intent {
    putExtra(key, uuid.toString())
    return this
}
