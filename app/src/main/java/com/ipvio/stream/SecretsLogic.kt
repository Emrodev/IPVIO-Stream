package com.ipvio.stream

/**
 * #112: decides what [Secrets] does with its Keystore key (pure, no Android).
 *
 * A key is only created when the alias does not exist at all. If the alias exists but the key cannot be read right now
 * (seen on some boxes after a restart), a new key under the same alias would replace the old one for good, and every
 * saved login would become unreadable even when the old key works again later.
 */
object SecretsLogic {
    enum class KeyAction { USE, GENERATE, UNAVAILABLE }

    fun keyAction(keyReadable: Boolean, aliasExists: Boolean): KeyAction = when {
        keyReadable -> KeyAction.USE
        aliasExists -> KeyAction.UNAVAILABLE
        else -> KeyAction.GENERATE
    }
}
