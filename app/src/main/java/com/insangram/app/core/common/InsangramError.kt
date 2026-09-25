package com.insangram.app.core.common

/**
 * Central domain error taxonomy. Every layer maps its own exceptions into this
 * type so the UI has exactly one thing to render and exactly one place to add
 * user-facing copy.
 */
sealed class InsangramError(
    open val userMessage: String,
    open val recoverable: Boolean = true,
    open val cause: Throwable? = null,
) {
    /**
     * Log-safe description for workers, crash reporting and pending-action
     * bookkeeping. Never shown to the user - that is [userMessage].
     */
    open val technicalMessage: String
        get() = buildString {
            append(this@InsangramError::class.simpleName ?: "InsangramError")
            append(": ")
            append(userMessage)
            cause?.let { append(" <- ").append(it::class.simpleName).append(": ").append(it.message) }
        }

    // --- connectivity -------------------------------------------------------
    data class Offline(override val cause: Throwable? = null) : InsangramError(
        userMessage = "You are offline. Showing saved content.",
        cause = cause,
    )

    data class Timeout(override val cause: Throwable? = null) : InsangramError(
        userMessage = "That took too long. Check your connection and try again.",
        cause = cause,
    )

    // --- auth ---------------------------------------------------------------
    data object InvalidCredentials : InsangramError("Incorrect email or password.")
    data object EmailAlreadyInUse : InsangramError("That email is already registered.")
    data object UsernameTaken : InsangramError("That username is already taken.")
    data object WeakPassword : InsangramError("Choose a stronger password (8+ characters).")
    data object EmailNotVerified : InsangramError("Verify your email address to continue.")
    data object AccountDisabled : InsangramError("This account has been disabled.", recoverable = false)
    data object ReauthenticationRequired : InsangramError("Please confirm your password to continue.")
    data object NotAuthenticated : InsangramError("You need to be signed in to do that.")
    data object TooManyAttempts : InsangramError("Too many attempts. Wait a moment and try again.")

    // --- authorisation / visibility ----------------------------------------
    data object PermissionDenied : InsangramError("You do not have access to this content.")
    data object PrivateAccount : InsangramError("This account is private. Follow to see their posts.")
    data object BlockedByUser : InsangramError("This content is unavailable.", recoverable = false)

    // --- content ------------------------------------------------------------
    /**
     * Missing content. [what] names the entity so one error type can serve
     * accounts, posts, collections and uploads.
     */
    data class NotFound(val what: String = "content") : InsangramError(
        userMessage = "This $what is no longer available.",
        recoverable = false,
    )
    data object MediaUnavailable : InsangramError("This media could not be loaded.")
    data class UploadFailed(val reason: String? = null) : InsangramError(
        userMessage = reason ?: "Upload failed. Tap retry to try again.",
    )
    data object MediaTooLarge : InsangramError("That file is too large to upload.")
    data object UnsupportedMediaType : InsangramError("That file type is not supported.")
    data object DuplicateUpload : InsangramError("You already uploaded this media.")

    // --- infrastructure -----------------------------------------------------
    data class RateLimited(val retryAfterSeconds: Long? = null) : InsangramError(
        userMessage = "You are doing that too often. Please slow down.",
    )
    data class RemoteFailure(override val cause: Throwable? = null) : InsangramError(
        userMessage = "Something went wrong on our side. Please try again.",
        cause = cause,
    )
    data class StorageFailure(override val cause: Throwable? = null) : InsangramError(
        userMessage = "Media storage is unavailable right now.",
        cause = cause,
    )
    data class DatabaseFailure(override val cause: Throwable? = null) : InsangramError(
        userMessage = "Local data could not be read. Try clearing the cache.",
        cause = cause,
    )
    data object FirebaseNotConfigured : InsangramError(
        userMessage = "Online mode is not configured. Add google-services.json, or use the demo build.",
        recoverable = false,
    )
    data class Validation(val field: String, override val userMessage: String) :
        InsangramError(userMessage)
    data class Unknown(override val cause: Throwable? = null) : InsangramError(
        userMessage = "Something went wrong. Please try again.",
        cause = cause,
    )

    /** An action that only exists in online mode was invoked in demo mode. */
    data class Unsupported(override val userMessage: String) : InsangramError(
        userMessage = userMessage,
        recoverable = false,
    )
}
