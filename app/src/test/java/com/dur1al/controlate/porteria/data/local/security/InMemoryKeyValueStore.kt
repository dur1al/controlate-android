package com.dur1al.controlate.porteria.data.local.security

/** Implementación en memoria de [KeyValueStore] para tests JVM. */
class InMemoryKeyValueStore : KeyValueStore {
    val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String
    override fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default
    override fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) { values[key] = value }
            override fun putInt(key: String, value: Int) { values[key] = value }
            override fun putLong(key: String, value: Long) { values[key] = value }
        }.block()
    }
}
