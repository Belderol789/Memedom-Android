package com.kratsapps.memedom

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson

class DatabaseManager {

    val gson = Gson()

    fun convertUserObject(context: Context, user: MemeDomUser, key: String) {
        var jsonString = gson.toJson(user)

        Log.d("Database", "Saved Json $jsonString")

        val sharedPreference = context.getSharedPreferences(key, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putString(key, jsonString)
        editor.apply()
    }

    fun retrieveSavedUser(context: Context, key: String): MemeDomUser? {
        val sharedPreference = context.getSharedPreferences(key, Context.MODE_PRIVATE)
        var savedJson = sharedPreference.getString(key, null)

        Log.d("Database", "Loaded Json $savedJson")

        if (savedJson != null) {
            var memeDomUser = gson.fromJson(savedJson, MemeDomUser::class.java)
            return memeDomUser
        }
        return null
    }
}