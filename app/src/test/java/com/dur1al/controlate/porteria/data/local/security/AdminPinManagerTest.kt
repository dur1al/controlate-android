package com.dur1al.controlate.porteria.data.local.security

import com.dur1al.controlate.porteria.data.local.security.AdminPinManager.VerifyResult
import com.dur1al.controlate.porteria.testing.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPinManagerTest {

    private val clock = FakeClock()
    private val store = InMemoryKeyValueStore()
    private val manager = AdminPinManager(store, clock)

    @Test
    fun `no pin is configured on first use`() {
        assertFalse(manager.isPinConfigured())
    }

    @Test
    fun `setPin rejects invalid formats and stores only a salted hash`() {
        assertFalse(manager.setPin("12a4"))
        assertTrue(manager.setPin("4829"))
        assertTrue(manager.isPinConfigured())
        assertFalse("PIN must never be stored in clear text", store.values.values.any { it == "4829" })
    }

    @Test
    fun `verify accepts the right pin and rejects the wrong one`() {
        manager.setPin("4829")
        assertEquals(VerifyResult.Granted, manager.verify("4829"))
        assertEquals(VerifyResult.Denied(remainingAttempts = 4), manager.verify("0000"))
    }

    @Test
    fun `five failures lock for thirty seconds even with the right pin`() {
        manager.setPin("4829")
        repeat(4) { manager.verify("0000") }
        assertEquals(VerifyResult.LockedOut(30), manager.verify("0000"))
        assertEquals(VerifyResult.LockedOut(30), manager.verify("4829"))

        clock.nowMs += 30_000
        assertEquals(VerifyResult.Granted, manager.verify("4829"))
    }

    @Test
    fun `a success resets the failure counter`() {
        manager.setPin("4829")
        repeat(3) { manager.verify("0000") }
        manager.verify("4829")
        assertEquals(VerifyResult.Denied(remainingAttempts = 4), manager.verify("0000"))
    }

    @Test
    fun `changePin requires the current pin`() {
        manager.setPin("4829")
        assertEquals(AdminPinManager.ChangeResult.WrongCurrentPin, manager.changePin("1111", "5555"))
        assertEquals(AdminPinManager.ChangeResult.InvalidNewPin, manager.changePin("4829", "55"))
        assertEquals(AdminPinManager.ChangeResult.Changed, manager.changePin("4829", "5555"))
        assertEquals(VerifyResult.Granted, manager.verify("5555"))
    }
}
