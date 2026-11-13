package com.dur1al.controlate.porteria.data.local.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Almacén clave-valor mínimo; permite probar [AdminPinManager] sin Android. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun edit(block: Editor.() -> Unit)

    interface Editor {
        fun putString(key: String, value: String)
        fun putInt(key: String, value: Int)
        fun putLong(key: String, value: Long)
    }
}

/**
 * [KeyValueStore] respaldado por `EncryptedSharedPreferences` (claves AES256-SIV, valores
 * AES256-GCM, llave maestra en Android Keystore). El archivo está excluido de los respaldos
 * en `backup_rules.xml` y `data_extraction_rules.xml`.
 */
class EncryptedKeyValueStore(context: Context) : KeyValueStore {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    @SuppressLint("ApplySharedPref") // Intencional: ver comentario sobre commit síncrono.
    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        val editor = prefs.edit()
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) { editor.putString(key, value) }
            override fun putInt(key: String, value: Int) { editor.putInt(key, value) }
            override fun putLong(key: String, value: Long) { editor.putLong(key, value) }
        }.block()
        // commit síncrono: el contador de intentos fallidos no debe perderse si el proceso muere.
        editor.commit()
    }

    private companion object {
        const val FILE_NAME = "controlate_secure_prefs"
    }
}
