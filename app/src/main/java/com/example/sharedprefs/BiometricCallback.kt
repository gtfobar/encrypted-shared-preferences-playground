package com.example.sharedprefs

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

class BiometricCallback(private val activity: FragmentActivity, private val crypto : Cryptography) : BiometricPrompt.AuthenticationCallback() {

    fun show(string : CharSequence) {
        Toast.makeText(
            activity, string, Toast.LENGTH_SHORT
        ).show()
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        show("Authentication error: $errString")
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        val cipher = result.cryptoObject?.cipher ?: return
        show("Authentication succeeded")
        val mode = crypto.getMode() ?: return
        when (mode) {
            "ENCRYPT" -> crypto.encryptOnAuthSucceeded(cipher)
            "DECRYPT" -> crypto.decryptOnAuthSucceeded(cipher)
        }
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        show("Authentication failed")
    }
}