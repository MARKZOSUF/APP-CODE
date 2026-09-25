package com.insangram.app.core.common

/**
 * Pure validation rules shared by the auth screens, the profile editor and the
 * Cloud Functions contract. Kept free of Android dependencies so it is unit
 * testable on the JVM.
 */
object Validators {

    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 24
    const val PASSWORD_MIN = 8
    const val BIO_MAX = 150
    const val CAPTION_MAX = 2_200
    const val COMMENT_MAX = 1_000
    const val NOTE_MAX = 60
    const val MIN_AGE_YEARS = 13

    private val USERNAME_REGEX = Regex("^[a-z0-9._]{$USERNAME_MIN,$USERNAME_MAX}$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val HASHTAG_REGEX = Regex("#([\\p{L}0-9_]{1,50})")
    private val MENTION_REGEX = Regex("@([a-z0-9._]{$USERNAME_MIN,$USERNAME_MAX})")

    /**
     * Canonical username form. Usernames are stored twice: the display value on
     * the user document, and this normalized value as the document id of
     * `usernames/{normalizedUsername}` which makes uniqueness a write
     * constraint rather than a query.
     */
    fun normalizeUsername(raw: String): String =
        raw.trim()
            .removePrefix("@")
            .lowercase()
            .replace(Regex("\\s+"), "")
            .filter { it.isLetterOrDigit() || it == '.' || it == '_' }

    fun validateUsername(raw: String): ValidationOutcome {
        val normalized = normalizeUsername(raw)
        return when {
            normalized.isEmpty() -> invalid("username", "Choose a username.")
            normalized.length < USERNAME_MIN ->
                invalid("username", "Usernames need at least $USERNAME_MIN characters.")
            normalized.length > USERNAME_MAX ->
                invalid("username", "Usernames can be at most $USERNAME_MAX characters.")
            !USERNAME_REGEX.matches(normalized) ->
                invalid("username", "Use letters, numbers, periods and underscores only.")
            normalized.startsWith(".") || normalized.endsWith(".") ->
                invalid("username", "Usernames cannot start or end with a period.")
            normalized.contains("..") ->
                invalid("username", "Usernames cannot contain two periods in a row.")
            normalized in RESERVED_USERNAMES ->
                invalid("username", "That username is reserved.")
            else -> ValidationOutcome.Valid
        }
    }

    fun validateEmail(raw: String): ValidationOutcome = when {
        raw.isBlank() -> invalid("email", "Enter your email address.")
        !EMAIL_REGEX.matches(raw.trim()) -> invalid("email", "That email address looks incorrect.")
        else -> ValidationOutcome.Valid
    }

    fun validatePassword(raw: String): ValidationOutcome = when {
        raw.length < PASSWORD_MIN ->
            invalid("password", "Use at least $PASSWORD_MIN characters.")
        raw.none { it.isDigit() } ->
            invalid("password", "Include at least one number.")
        raw.none { it.isLetter() } ->
            invalid("password", "Include at least one letter.")
        raw.lowercase() in WEAK_PASSWORDS ->
            invalid("password", "That password is too common.")
        else -> ValidationOutcome.Valid
    }

    fun validatePasswordConfirmation(password: String, confirmation: String): ValidationOutcome =
        if (password != confirmation) {
            invalid("confirmPassword", "Passwords do not match.")
        } else {
            ValidationOutcome.Valid
        }

    fun validateFullName(raw: String): ValidationOutcome = when {
        raw.trim().length < 2 -> invalid("fullName", "Enter your name.")
        raw.trim().length > 60 -> invalid("fullName", "That name is too long.")
        else -> ValidationOutcome.Valid
    }

    /** Date of birth is validated against a caller-supplied clock for testability. */
    fun validateDateOfBirth(birthEpochMillis: Long?, nowEpochMillis: Long): ValidationOutcome {
        if (birthEpochMillis == null) return invalid("dateOfBirth", "Enter your date of birth.")
        if (birthEpochMillis > nowEpochMillis) {
            return invalid("dateOfBirth", "That date is in the future.")
        }
        val years = (nowEpochMillis - birthEpochMillis) / 31_556_952_000L
        return if (years < MIN_AGE_YEARS) {
            invalid("dateOfBirth", "You must be at least $MIN_AGE_YEARS to sign up.")
        } else {
            ValidationOutcome.Valid
        }
    }

    fun validateCaption(raw: String): ValidationOutcome =
        if (raw.length > CAPTION_MAX) {
            invalid("caption", "Captions can be at most $CAPTION_MAX characters.")
        } else {
            ValidationOutcome.Valid
        }

    fun validateComment(raw: String): ValidationOutcome = when {
        raw.isBlank() -> invalid("comment", "Write something first.")
        raw.length > COMMENT_MAX -> invalid("comment", "Comments can be at most $COMMENT_MAX characters.")
        else -> ValidationOutcome.Valid
    }

    fun extractHashtags(text: String): List<String> =
        HASHTAG_REGEX.findAll(text)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .take(30)
            .toList()

    fun extractMentions(text: String): List<String> =
        MENTION_REGEX.findAll(text.lowercase())
            .map { it.groupValues[1] }
            .distinct()
            .take(20)
            .toList()

    /**
     * Bounded keyword tokens written alongside a post so Firestore can support
     * keyword search without a full-text index. See docs/FIRESTORE_SCHEMA.md.
     */
    fun captionKeywordTokens(caption: String, limit: Int = 25): List<String> =
        caption.lowercase()
            .split(Regex("[^\\p{L}0-9]+"))
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .distinct()
            .take(limit)
            .toList()

    /** Prefixes of a username, used for `whereIn` prefix search. */
    fun usernamePrefixes(username: String, maxPrefixLength: Int = 12): List<String> {
        val normalized = normalizeUsername(username)
        return (1..minOf(normalized.length, maxPrefixLength)).map { normalized.substring(0, it) }
    }


    private val WEBSITE_REGEX =
        Regex("^(https?://)?[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+(/\\S*)?$")

    /** Boolean form of [validateEmail] for call sites that only need a flag. */
    fun isValidEmail(raw: String): Boolean = EMAIL_REGEX.matches(raw.trim())

    /**
     * Field-keyed registration errors so the sign-up form can surface every
     * invalid input at once. Key names match the form field names.
     */
    fun validateRegistration(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        dateOfBirthMillis: Long?,
        termsAccepted: Boolean,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Map<String, String> {
        val errors = LinkedHashMap<String, String>()
        fun put(field: String, outcome: ValidationOutcome) {
            (outcome as? ValidationOutcome.Invalid)?.let { errors[field] = it.error.userMessage }
        }
        put("fullName", validateFullName(fullName))
        put("username", validateUsername(username))
        put("email", validateEmail(email))
        put("password", validatePassword(password))
        put("confirmPassword", validatePasswordConfirmation(password, confirmPassword))
        put("dateOfBirth", validateDateOfBirth(dateOfBirthMillis, nowEpochMillis))
        if (!termsAccepted) errors["terms"] = "Accept the terms to continue."
        return errors
    }

    /**
     * First-error form used by the repositories, which fail a registration as
     * soon as any field is invalid.
     */
    fun validateRegistration(
        fullName: String,
        username: String,
        email: String,
        password: String,
        dateOfBirthMillis: Long?,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): InsangramError.Validation? {
        val outcomes = listOf(
            validateFullName(fullName),
            validateUsername(username),
            validateEmail(email),
            validatePassword(password),
            validateDateOfBirth(dateOfBirthMillis, nowEpochMillis),
        )
        return outcomes.firstNotNullOfOrNull { (it as? ValidationOutcome.Invalid)?.error }
    }

    /** Profile editor rules. Returns null when the profile is acceptable. */
    fun validateProfile(
        fullName: String,
        username: String,
        website: String,
    ): InsangramError.Validation? {
        (validateFullName(fullName) as? ValidationOutcome.Invalid)?.let { return it.error }
        (validateUsername(username) as? ValidationOutcome.Invalid)?.let { return it.error }
        val trimmed = website.trim()
        if (trimmed.isNotEmpty() && !WEBSITE_REGEX.matches(trimmed)) {
            return InsangramError.Validation("website", "Enter a valid link.")
        }
        return null
    }

    private fun invalid(field: String, message: String) =
        ValidationOutcome.Invalid(InsangramError.Validation(field, message))

    private val RESERVED_USERNAMES = setOf(
        "insangram", "admin", "administrator", "support", "help", "root", "system",
        "moderator", "official", "security", "about", "settings", "explore", "reels",
    )

    private val WEAK_PASSWORDS = setOf(
        "password", "password1", "12345678", "qwerty123", "insangram", "letmein1",
        "11111111", "abc12345", "password123",
    )

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "this", "that", "from", "have", "was", "were",
        "are", "but", "you", "your", "его", "not", "all", "can", "had", "her", "his",
        "our", "out", "who", "get", "got", "just", "now", "new", "one", "day",
    )
}

sealed interface ValidationOutcome {
    data object Valid : ValidationOutcome
    data class Invalid(val error: InsangramError.Validation) : ValidationOutcome

    val errorMessage: String? get() = (this as? Invalid)?.error?.userMessage
    val isValid: Boolean get() = this is Valid
}

/** Returns the first invalid outcome, or [ValidationOutcome.Valid]. */
fun List<ValidationOutcome>.firstError(): ValidationOutcome =
    firstOrNull { it is ValidationOutcome.Invalid } ?: ValidationOutcome.Valid
