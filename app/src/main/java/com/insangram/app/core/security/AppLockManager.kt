package com.insangram.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.insangram.app.core.datastore.InsangramPreferences
import com.insangram.app.domain.model.AppLockMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Biometric / PIN app lock.
 *
 * The PIN is never stored in plaintext: it is hashed with [PasswordHasher] and
 * the resulting hash is written to an EncryptedSharedPreferences file whose
 * master key lives in the Android Keystore.
 */
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: InsangramPreferences,
    private val passwordHasher: PasswordHasher,
) {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAt: Long = 0

    private val securePrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun canUseBiometrics(): Boolean =
        BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    suspend fun onAppBackgrounded() {
        backgroundedAt = System.currentTimeMillis()
        if (preferences.settings.first().appLockMode != AppLockMode.OFF) {
            // Locking happens on resume so the lock screen is not visible in
            // the recents thumbnail transition.
            _locked.value = _locked.value
        }
    }

    /** Re-locks if the configured idle timeout elapsed while backgrounded. */
    suspend fun onAppForegrounded() {
        val settings = preferences.settings.first()
        if (settings.appLockMode == AppLockMode.OFF) {
            _locked.value = false
            return
        }
        val elapsed = System.currentTimeMillis() - backgroundedAt
        if (backgroundedAt > 0 && elapsed >= settings.appLockTimeoutMs) _locked.value = true
    }

    fun unlock() {
        _locked.value = false
        backgroundedAt = 0
    }

    fun lockNow() {
        _locked.value = true
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }

    fun setPin(pin: String) {
        val hashed = passwordHasher.hash(pin.toCharArray())
        securePrefs.edit()
            .putString(KEY_PIN_HASH, hashed.hash)
            .putString(KEY_PIN_SALT, hashed.salt)
            .putInt(KEY_PIN_ITERATIONS, hashed.iterations)
            .apply()
    }

    fun hasPin(): Boolean = securePrefs.contains(KEY_PIN_HASH)

    fun verifyPin(pin: String): Boolean {
        val hash = securePrefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = securePrefs.getString(KEY_PIN_SALT, null) ?: return false
        val iterations = securePrefs.getInt(KEY_PIN_ITERATIONS, 0)
        val ok = passwordHasher.verify(pin.toCharArray(), HashedPassword(hash, salt, iterations))
        if (ok) unlock()
        return ok
    }

    fun clearPin() {
        securePrefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_ITERATIONS)
            .apply()
    }

    /** Ensures a Keystore-backed key exists for the encrypted preference file. */
    fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = javax.crypto.KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        generator.generateKey()
    }

    private companion object {
        const val SECURE_PREFS_NAME = "insangram_secure_prefs"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "insangram_app_lock"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_ITERATIONS = "pin_iterations"
        const val ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
