package com.fps.calculadora.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fps.calculadora.core.ProfileBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.prefsDataStore by preferencesDataStore(name = "fps_prefs")
private val PROFILES_KEY = stringPreferencesKey("profiles")
private val json = Json { ignoreUnknownKeys = true }

/**
 * O que o app lembra entre aberturas fora do histórico: os perfis de PC — e
 * portanto a build atual, que mora no perfil ativo. Substitui `fps:profiles:v1`
 * e `fps:state:v2` do `localStorage` do `index.html`.
 *
 * WebView e app nativo não compartilham disco, então não há migração a fazer:
 * quem usava a versão antiga recomeça com o perfil inicial.
 */
data class StoredPreferences(
    /** `null` quando nunca se gravou nada — primeira abertura, ou dado ilegível. */
    val profiles: ProfileBook?,
)

class PreferencesStore(private val context: Context) {

    val preferences: Flow<StoredPreferences> = context.prefsDataStore.data.map { prefs ->
        StoredPreferences(
            profiles = prefs[PROFILES_KEY]?.let(::decodeProfiles),
        )
    }

    suspend fun saveProfiles(book: ProfileBook) {
        context.prefsDataStore.edit { prefs ->
            prefs[PROFILES_KEY] = json.encodeToString(book)
        }
    }

    /**
     * Dado corrompido vira "nunca gravou" em vez de derrubar a abertura do app.
     * Perde-se o perfil salvo — mas o app abre, e o próximo `saveProfiles`
     * regrava por cima.
     */
    private fun decodeProfiles(raw: String): ProfileBook? = try {
        json.decodeFromString<ProfileBook>(raw)
    } catch (e: Exception) {
        null
    }
}
