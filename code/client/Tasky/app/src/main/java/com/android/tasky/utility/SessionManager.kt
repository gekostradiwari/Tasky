package com.android.tasky.utility

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SessionManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        sharedPreferences = EncryptedSharedPreferences.create(
            "auth_prefs", // Nome del file
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Funzione per salvare il token
    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString(AUTH_TOKEN_KEY, token)
            .apply()
    }

    // Funzione per leggere il token
    fun getAuthToken(): String? {
        return sharedPreferences.getString(AUTH_TOKEN_KEY, null)
    }

    // Funzione per cancellare il token
    fun clearAuthToken() {
        sharedPreferences.edit()
            .remove(AUTH_TOKEN_KEY)
            .apply()
    }

    companion object {
        private const val AUTH_TOKEN_KEY = "AUTH_TOKEN"

        @Volatile
        private var INSTANCE: SessionManager? = null

        // Funzione per ottenere l'unica istanza del SessionManager
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}