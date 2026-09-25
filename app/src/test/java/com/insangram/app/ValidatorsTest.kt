package com.insangram.app

import com.insangram.app.core.common.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Registration validation and username normalization rules. */
class ValidatorsTest {

    @Test
    fun `usernames are normalized to lowercase and stripped of illegal characters`() {
        assertEquals("aarav_dev", Validators.normalizeUsername("  Aarav_Dev "))
        assertEquals("meera.k", Validators.normalizeUsername("Meera.K!"))
        assertEquals("kabir99", Validators.normalizeUsername("Kabir 99"))
    }

    @Test
    fun `valid emails are accepted and malformed ones rejected`() {
        assertTrue(Validators.isValidEmail("aarav@insangram.example"))
        assertFalse(Validators.isValidEmail("aarav@"))
        assertFalse(Validators.isValidEmail("not-an-email"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun `registration requires matching passwords`() {
        val errors = Validators.validateRegistration(
            fullName = "Aarav Sharma",
            username = "aarav",
            email = "aarav@insangram.example",
            password = "Demo@12345",
            confirmPassword = "Demo@54321",
            dateOfBirthMillis = birthday(2000),
            termsAccepted = true,
        )
        assertTrue(errors.containsKey("confirmPassword"))
    }

    @Test
    fun `registration requires accepted terms and a plausible age`() {
        val underage = Validators.validateRegistration(
            fullName = "Test User",
            username = "testuser",
            email = "test@insangram.example",
            password = "Demo@12345",
            confirmPassword = "Demo@12345",
            dateOfBirthMillis = birthday(2020),
            termsAccepted = true,
        )
        assertTrue(underage.containsKey("dateOfBirth"))

        val noTerms = Validators.validateRegistration(
            fullName = "Test User",
            username = "testuser",
            email = "test@insangram.example",
            password = "Demo@12345",
            confirmPassword = "Demo@12345",
            dateOfBirthMillis = birthday(2000),
            termsAccepted = false,
        )
        assertTrue(noTerms.containsKey("terms"))
    }

    @Test
    fun `a fully valid registration produces no errors`() {
        val errors = Validators.validateRegistration(
            fullName = "Aarav Sharma",
            username = "aarav_dev",
            email = "aarav@insangram.example",
            password = "Demo@12345",
            confirmPassword = "Demo@12345",
            dateOfBirthMillis = birthday(1999),
            termsAccepted = true,
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `hashtags and mentions are extracted from captions`() {
        val caption = "Sunrise at #Munnar with @meera.k and @kabir #travel #Travel"
        assertEquals(listOf("munnar", "travel"), Validators.extractHashtags(caption))
        assertEquals(listOf("meera.k", "kabir"), Validators.extractMentions(caption))
    }

    private fun birthday(year: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
        return calendar.timeInMillis
    }
}
