package com.kratsapps.memedom

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson

class DatabaseManager(context: Context) {

    private val MAIN_USER = "MainUser"
    private val dbContext = context
    private val gson = Gson()

    fun convertUserObject(user: MemeDomUser, key: String) {
        var jsonString = gson.toJson(user)

        Log.d("Database", "Saved Json $jsonString")

        val sharedPreference = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putString(key, jsonString)
        editor.apply()
    }

    fun retrieveSavedUser(key: String): MemeDomUser? {
        val savedJson = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE).getString(key, null)

        Log.d("Database", "Loaded Json $savedJson")

        if (savedJson != null) {
            var memeDomUser = gson.fromJson(savedJson, MemeDomUser::class.java)
            return memeDomUser
        }
        return null
    }

    fun getMainUserID(): String? {
        val savedJson = dbContext.getSharedPreferences(MAIN_USER, Context.MODE_PRIVATE).getString(MAIN_USER, null)
        if(savedJson != null) {
            val memeDomUser = gson.fromJson(savedJson, MemeDomUser::class.java)
            return memeDomUser.uid
        }
        return null
    }
}