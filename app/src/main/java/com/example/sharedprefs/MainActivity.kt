package com.example.sharedprefs

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


class MainActivity : AppCompatActivity() {
    private val crypto = Cryptography()
    private lateinit var encrypted_sp : SharedPreferences
    private lateinit var valueToSaveEditText: EditText
    private lateinit var keyToSaveEditText: EditText
    private lateinit var keyToReadEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configureButtons()
        configureEditTexts()

        encrypted_sp = EncryptedSharedPreferences.create(
            this,
            getString(R.string.api_encrypted_shared_prefs),
            MasterKey.Builder(this, getString(R.string.api_encryption_alias))
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        crypto.init(this)
    }

    private fun configureEditTexts() {
        valueToSaveEditText = findViewById(R.id.edit_save_value)
        keyToSaveEditText = findViewById(R.id.edit_save_key)
        keyToReadEditText = findViewById(R.id.edit_read_key)
    }

    private fun configureButtons() {
        val savePlainTextButton: Button = findViewById(R.id.save_plain_text_button)
        savePlainTextButton.setOnClickListener { savePlainText() }

        val saveManuallyEncryptedButton: Button =
            findViewById(R.id.save_manually_encrypted_button)
        saveManuallyEncryptedButton.setOnClickListener { saveManuallyEncrypted() }

        val saveAPIEncryptedButton: Button = findViewById(R.id.save_api_encrypted_button)
        saveAPIEncryptedButton.setOnClickListener { saveAPIEncrypted() }

        val readPlainTextButton: Button = findViewById(R.id.read_plain_text_button)
        readPlainTextButton.setOnClickListener { readPlainText() }

        val readManuallyEncryptedButton: Button =
            findViewById(R.id.read_manually_encrypted_button)
        readManuallyEncryptedButton.setOnClickListener { readManuallyEncrypted() }

        val readAPIEncryptedButton: Button = findViewById(R.id.read_api_encrypted_button)
        readAPIEncryptedButton.setOnClickListener { readAPIEncrypted() }

        val resetButton: Button = findViewById(R.id.reset_button)
        resetButton.setOnClickListener { reset() }
    }

    private fun show(message: String?) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
    }

    private fun savePlainText() {
        val key = keyToSaveEditText.text.toString()
        val value = valueToSaveEditText.text.toString()

        val sp = this.getSharedPreferences(
            getString(R.string.plain_text_shared_prefs),
            Context.MODE_PRIVATE
        ) ?: return

        with(sp.edit()) {
            putString(key, value)
            apply()
        }

        show(key + " : " + value + "successfully saved")
        keyToSaveEditText.setText("")
        valueToSaveEditText.setText("")
    }

    private fun saveManuallyEncrypted() {
        val value = valueToSaveEditText.text.toString()
        crypto.encrypt(value)
    }

    fun saveManuallyEncryptedOnAuthSucceeded(encryptedValue : String) {
        val key = keyToSaveEditText.text.toString()
        val value = valueToSaveEditText.text.toString()

        val sp = this.getSharedPreferences(
            getString(R.string.manually_encrypted_shared_prefs),
            Context.MODE_PRIVATE
        ) ?: return

        with(sp.edit()) {
            putString(key, encryptedValue)
            apply()
        }

        show("$key : $value saved")
        keyToSaveEditText.setText("")
        valueToSaveEditText.setText("")
    }

    private fun saveAPIEncrypted() {
        val key = keyToSaveEditText.text.toString()
        val value = valueToSaveEditText.text.toString()

        with(encrypted_sp.edit()) {
            putString(key, value)
            apply()
        }

        show("$key : $value successfully saved")
        keyToSaveEditText.setText("")
        valueToSaveEditText.setText("")
    }

    private fun readPlainText() {
        val key = keyToReadEditText.text.toString()

        val sp = this.getSharedPreferences(
            getString(R.string.plain_text_shared_prefs),
            Context.MODE_PRIVATE
        ) ?: return

        val value = sp.getString(key, null)
        if (value != null) {
            show("$key : $value")
            keyToReadEditText.setText("")
        } else {
            show(getString(R.string.key_not_found, key))
        }
    }

    private fun readManuallyEncrypted() {
        val key = keyToReadEditText.text.toString()

        val sp = this.getSharedPreferences(
            getString(R.string.manually_encrypted_shared_prefs),
            Context.MODE_PRIVATE
        ) ?: return

        val value = sp.getString(key, null)
        if (value != null) {
            crypto.decrypt(value)
        } else {
            show(getString(R.string.key_not_found, key))
        }
    }

    fun readManuallyEncryptedOnAuthSucceeded(decryptedValue : String) {
        val key = keyToReadEditText.text.toString()

        show("$key : $decryptedValue")
        keyToReadEditText.setText("")
    }

    private fun readAPIEncrypted() {
        val key = keyToReadEditText.text.toString()

        val value = encrypted_sp.getString(key, null)
        if (value != null) {
            show("$key : $value")
            keyToReadEditText.setText("")
        } else {
            show(getString(R.string.key_not_found, key))
        }
    }

    private fun reset() {
        val manual_sp = this.getSharedPreferences(
            getString(R.string.manually_encrypted_shared_prefs),
            Context.MODE_PRIVATE
        ) ?: return
        with(manual_sp.edit()) {
            clear()
            apply()
        }

        val plain_sp = this.getSharedPreferences(
            getString(R.string.plain_text_shared_prefs),
            Context.MODE_PRIVATE
        ) ?: return
        with(plain_sp.edit()) {
            clear()
            apply()
        }

        with(encrypted_sp.edit()) {
            clear()
            apply()
        }

        crypto.reset()
    }
}