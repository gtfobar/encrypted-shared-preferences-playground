package com.example.sharedprefs
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class Cryptography {
    lateinit var activity : MainActivity
    private lateinit var biometricPrompt : BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var data : ByteArray? = null
    private var mode : String? = null

    fun getMode() : String? {
        return mode
    }

    fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore") // 1

        val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder("SharedPrefs",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keyGenParameterSpecBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            keyGenParameterSpecBuilder.setUnlockedDeviceRequired(true)
        }
        val keyGenParameterSpec = keyGenParameterSpecBuilder.build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun reset() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry("SharedPrefs")

        generateKey()
    }

    fun init(activity : MainActivity) {
        this.activity = activity
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            keyStore.getEntry("SharedPrefs", null) as KeyStore.SecretKeyEntry
        } catch (e: Exception) {
            generateKey()
        }

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("unlock key to encrypt/decrypt shared prefs")
            .setSubtitle("log in using biometrics")
            .setNegativeButtonText("or gtfo")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt = BiometricPrompt(activity, BiometricCallback(activity, this))
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKeyEntry =
            keyStore.getEntry("SharedPrefs", null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    fun encrypt(dataToEncrypt : String) {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        data = dataToEncrypt.toByteArray()
        mode = "ENCRYPT"

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun encryptOnAuthSucceeded(cipher: Cipher) {
        val encryptedBytes = cipher.doFinal(data)
        val result = Base64.encodeToString(cipher.iv + encryptedBytes, Base64.DEFAULT)
        activity.saveManuallyEncryptedOnAuthSucceeded(result)
        data = null
        mode = null
    }

    fun decrypt(dataToDecrypt : String) {
        val decodedData = Base64.decode(dataToDecrypt, Base64.DEFAULT)
        val ivBytes = decodedData.copyOfRange(0, 12)
        val encryptedBytes = decodedData.copyOfRange(12, decodedData.size)

        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivBytes))

        data = encryptedBytes
        mode = "DECRYPT"

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun decryptOnAuthSucceeded(cipher : Cipher) {
        val result = String(cipher.doFinal(data))
        activity.readManuallyEncryptedOnAuthSucceeded(result)
        data = null
        mode = null
    }
}