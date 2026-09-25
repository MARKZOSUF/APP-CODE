package com.insangram.app.core.common

import android.database.sqlite.SQLiteException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single place where platform/SDK exceptions become [InsangramError].
 * Nothing else in the codebase inspects Firebase or SQLite exception types.
 */
@Singleton
class ErrorMapper @Inject constructor() {

    fun map(throwable: Throwable): InsangramError = when (throwable) {
        is InsangramException -> throwable.error

        // connectivity
        is FirebaseNetworkException -> InsangramError.Offline(throwable)
        is SocketTimeoutException, is TimeoutException -> InsangramError.Timeout(throwable)
        is IOException -> InsangramError.Offline(throwable)

        // auth
        is FirebaseAuthWeakPasswordException -> InsangramError.WeakPassword
        is FirebaseAuthInvalidCredentialsException -> InsangramError.InvalidCredentials
        is FirebaseAuthUserCollisionException -> InsangramError.EmailAlreadyInUse
        is FirebaseAuthRecentLoginRequiredException -> InsangramError.ReauthenticationRequired
        is FirebaseAuthInvalidUserException -> InsangramError.AccountDisabled
        is FirebaseTooManyRequestsException -> InsangramError.TooManyAttempts

        // firestore
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> InsangramError.PermissionDenied
            FirebaseFirestoreException.Code.NOT_FOUND -> InsangramError.NotFound()
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> InsangramError.NotAuthenticated
            FirebaseFirestoreException.Code.UNAVAILABLE -> InsangramError.Offline(throwable)
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> InsangramError.Timeout(throwable)
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> InsangramError.RateLimited()
            else -> InsangramError.RemoteFailure(throwable)
        }

        // storage
        is StorageException -> when (throwable.errorCode) {
            StorageException.ERROR_NOT_AUTHORIZED -> InsangramError.PermissionDenied
            StorageException.ERROR_OBJECT_NOT_FOUND -> InsangramError.MediaUnavailable
            StorageException.ERROR_QUOTA_EXCEEDED -> InsangramError.RateLimited()
            StorageException.ERROR_CANCELED -> InsangramError.UploadFailed("Upload cancelled.")
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> InsangramError.UploadFailed()
            else -> InsangramError.StorageFailure(throwable)
        }

        is SQLiteException -> InsangramError.DatabaseFailure(throwable)
        is IllegalStateException ->
            if (throwable.message?.contains("FirebaseApp") == true) {
                InsangramError.FirebaseNotConfigured
            } else {
                InsangramError.Unknown(throwable)
            }

        else -> InsangramError.Unknown(throwable)
    }
}

/** Thrown internally when a layer needs to signal an already-mapped error. */
class InsangramException(val error: InsangramError) : Exception(error.userMessage, error.cause)
