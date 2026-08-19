package com.pico.swan.colorstrike.data

import android.content.Context
import com.pico.swan.colorstrike.domain.model.SessionSummary

interface ColorStrikeRepository {
    suspend fun save(summary: SessionSummary)
    suspend fun recentCount(): Int
}

class SharedPreferencesColorStrikeRepository(context: Context) : ColorStrikeRepository {
    private val preferences = context.getSharedPreferences("color-strike", Context.MODE_PRIVATE)
    override suspend fun save(summary: SessionSummary) {
        preferences.edit().putInt("recent_count", preferences.getInt("recent_count", 0) + 1).apply()
    }
    override suspend fun recentCount(): Int = preferences.getInt("recent_count", 0)
}
