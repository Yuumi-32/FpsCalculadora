package com.fps.calculadora.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fps.calculadora.core.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.historyDataStore by preferencesDataStore(name = "fps_history")
private val HISTORY_KEY = stringPreferencesKey("entries")
private val json = Json { ignoreUnknownKeys = true }

/**
 * Histórico local de builds salvos. Substitui o `H_KEY` do `localStorage` do
 * `index.html` (:2513) por `DataStore` — são armazenamentos independentes
 * (WebView e app nativo não compartilham disco), então não há migração a
 * fazer, só uma persistência nova pro lado Compose.
 */
class HistoryStore(private val context: Context) {

    val entries: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        val raw = prefs[HISTORY_KEY] ?: return@map emptyList()
        try {
            json.decodeFromString<List<HistoryEntry>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(entries: List<HistoryEntry>) {
        context.historyDataStore.edit { prefs ->
            prefs[HISTORY_KEY] = json.encodeToString(entries)
        }
    }
}
