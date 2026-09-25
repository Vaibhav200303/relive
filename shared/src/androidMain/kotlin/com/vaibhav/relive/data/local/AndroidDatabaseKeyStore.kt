package com.vaibhav.relive.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps the random SQLCipher passphrase encrypted by a non-exportable Android Keystore key. */
internal class AndroidDatabaseKeyStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getOrCreatePassphrase(): ByteArray = synchronized(keyCreationLock) {
        val stored = preferences.getString(WRAPPED_PASSPHRASE, null)
        if (stored != null) return@synchronized unwrap(stored)

        val passphrase = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        val wrapped = wrap(passphrase)
        check(preferences.edit().putString(WRAPPED_PASSPHRASE, wrapped).commit()) {
            "Unable to persist the protected database key."
        }
        passphrase
    }

    private fun wrap(passphrase: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        val ciphertext = cipher.doFinal(passphrase)
        return listOf(cipher.iv, ciphertext)
            .joinToString(SEPARATOR) { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    private fun unwrap(encoded: String): ByteArray {
        val pieces = encoded.split(SEPARATOR, limit = 2)
        check(pieces.size == 2) { "The protected database key is invalid." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(pieces[0], Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, getWrappingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(Base64.decode(pieces[1], Base64.NO_WRAP))
            .also { check(it.size == PASSPHRASE_BYTES) { "The protected database key has an invalid size." } }
    }

    private fun getOrCreateWrappingKey(): SecretKey =
        (keyStore().getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
            }
            .generateKey()

    private fun getWrappingKey(): SecretKey =
        keyStore().getKey(KEY_ALIAS, null) as? SecretKey
            ?: error("The device database key is unavailable. Relive will not replace the archive.")

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "relive.database.wrapping.v1"
        const val PREFERENCES_NAME = "relive_database_security"
        const val WRAPPED_PASSPHRASE = "wrapped_passphrase_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "."
        const val PASSPHRASE_BYTES = 32
        const val GCM_TAG_BITS = 128
        val keyCreationLock = Any()
    }
}
