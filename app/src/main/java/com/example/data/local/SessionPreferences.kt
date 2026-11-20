package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.mailVaultDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "mailvault_session"
)

data class StoredSession(
  val accountId: String,
  val address: String,
  val password: String,
  val token: String,
)

class SessionPreferences(private val context: Context) {

  private object Keys {
    val ACCOUNT_ID = stringPreferencesKey("account_id")
    val ADDRESS = stringPreferencesKey("address")
    val PASSWORD = stringPreferencesKey("password")
    val TOKEN = stringPreferencesKey("token")
  }

  suspend fun loadSession(): StoredSession? {
    return context.mailVaultDataStore.data.map { prefs ->
      val id = prefs[Keys.ACCOUNT_ID].orEmpty()
      val address = prefs[Keys.ADDRESS].orEmpty()
      val password = prefs[Keys.PASSWORD].orEmpty()
      val token = prefs[Keys.TOKEN].orEmpty()
      if (id.isNotBlank() && address.isNotBlank() && token.isNotBlank()) {
        StoredSession(id, address, password, token)
      } else {
        null
      }
    }.first()
  }

  suspend fun saveSession(session: StoredSession) {
    context.mailVaultDataStore.edit { prefs ->
      prefs[Keys.ACCOUNT_ID] = session.accountId
      prefs[Keys.ADDRESS] = session.address
      prefs[Keys.PASSWORD] = session.password
      prefs[Keys.TOKEN] = session.token
    }
  }

  suspend fun clearSession() {
    context.mailVaultDataStore.edit { it.clear() }
  }
}
