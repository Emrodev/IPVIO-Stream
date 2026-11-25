package com.ipvio.stream

import com.ipvio.stream.SecretsLogic.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Test

/** #112: a Keystore key is never replaced while its alias still exists (pure, no Android). */
class SecretsLogicTest {

    @Test
    fun readableKey_isUsed() {
        assertEquals(KeyAction.USE, SecretsLogic.keyAction(keyReadable = true, aliasExists = true))
    }

    @Test
    fun missingAlias_generatesNewKey() {
        assertEquals(KeyAction.GENERATE, SecretsLogic.keyAction(keyReadable = false, aliasExists = false))
    }

    @Test
    fun existingAliasWithUnreadableKey_isNeverReplaced() {
        assertEquals(KeyAction.UNAVAILABLE, SecretsLogic.keyAction(keyReadable = false, aliasExists = true))
    }
}
