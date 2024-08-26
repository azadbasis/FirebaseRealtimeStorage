package com.firebase.storages

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException



// Extension property to create DataStore instance
val Context.dataStore by preferencesDataStore("app_preferences")

class DatastoreUtil private constructor(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        @Volatile
        private var INSTANCE: DatastoreUtil? = null

        // Predefined keys for common data types
        // Predefined keys for name and email
        val NAME_KEY = stringPreferencesKey("name_key")
        val EMAIL_KEY = stringPreferencesKey("email_key")
        val PHONE_KEY = stringPreferencesKey("phone_key")
        val EXAMPLE_INT_KEY = intPreferencesKey("example_int")
        val EXAMPLE_BOOLEAN_KEY = booleanPreferencesKey("example_boolean")

        fun getInstance(context: Context): DatastoreUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatastoreUtil(context).also { INSTANCE = it }
            }
        }
    }

    suspend fun <T> saveData(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun <T> getData(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }

    suspend fun clearData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

