package com.example.calc

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * مدیریت ذخیره‌سازی و بازیابی تاریخچه محاسبات مهندسی در پروژه‌ها
 */
class CalculationHistoryRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _historyFlow = MutableStateFlow<List<CalculationHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<CalculationHistoryItem>> = _historyFlow.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val raw = prefs.getString(KEY_HISTORY, null)
        if (!raw.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<CalculationHistoryItem>>(raw)
                _historyFlow.value = list
            } catch (e: Exception) {
                _historyFlow.value = emptyList()
            }
        } else {
            _historyFlow.value = emptyList()
        }
    }

    fun saveHistoryItem(item: CalculationHistoryItem) {
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item) // جدیدترین در بالا
        val limited = current.take(MAX_HISTORY_ENTRIES)
        _historyFlow.value = limited
        persist(limited)
    }

    fun deleteHistoryItem(id: String) {
        val updated = _historyFlow.value.filterNot { it.id == id }
        _historyFlow.value = updated
        persist(updated)
    }

    fun clearAllHistory() {
        _historyFlow.value = emptyList()
        persist(emptyList())
    }

    fun getItemsForProject(projectId: String?): List<CalculationHistoryItem> {
        if (projectId.isNullOrBlank()) return _historyFlow.value
        return _historyFlow.value.filter { it.projectId == projectId }
    }

    private fun persist(list: List<CalculationHistoryItem>) {
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString(KEY_HISTORY, encoded).apply()
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    companion object {
        private const val PREF_NAME = "simyar_calc_history_pref"
        private const val KEY_HISTORY = "saved_calc_history_json"
        private const val MAX_HISTORY_ENTRIES = 100
    }
}
