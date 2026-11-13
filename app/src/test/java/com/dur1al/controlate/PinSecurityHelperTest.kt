package com.dur1al.controlate

import com.dur1al.controlate.porteria.data.local.security.PinSecurityHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityHelperTest {

    @Test
    fun `isValidPinFormat accepts 4 to 8 numeric digits`() {
        assertTrue(PinSecurityHelper.isValidPinFormat("4829"))
        assertTrue(PinSecurityHelper.isValidPinFormat("12345678"))
        assertFalse(PinSecurityHelper.isValidPinFormat("123"))
        assertFalse(PinSecurityHelper.isValidPinFormat("123456789"))
        assertFalse(PinSecurityHelper.isValidPinFormat("12a4"))
        assertFalse(PinSecurityHelper.isValidPinFormat(""))
    }

    @Test
    fun `hashPin generates unique salted hashes and verifies correctly`() {
        val pin = "5931"
        val hashA = PinSecurityHelper.hashPin(pin)
        val hashB = PinSecurityHelper.hashPin(pin)

        assertNotEquals(hashA, hashB)
        assertTrue(PinSecurityHelper.verifyPin(pin, hashA))
        assertTrue(PinSecurityHelper.verifyPin(pin, hashB))
        assertFalse(PinSecurityHelper.verifyPin("0000", hashA))
    }

    @Test
    fun `verifyPin rejects malformed or empty stored hashes`() {
        assertFalse(PinSecurityHelper.verifyPin("4829", null))
        assertFalse(PinSecurityHelper.verifyPin("4829", ""))
        assertFalse(PinSecurityHelper.verifyPin("4829", "invalid-format-without-colon"))
        assertFalse(PinSecurityHelper.verifyPin("", "ZmFrZQ==:ZmFrZQ=="))
    }
}
