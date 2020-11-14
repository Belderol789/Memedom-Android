package com.kratsapps.memedom.utils

import android.content.Context
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.kratsapps.memedom.models.MemeDomUser
import java.lang.reflect.Type


class DatabaseManager(context: Context) {

    private val POST_IDS = "PostIDs"
    private val MAIN_USER = "MainUser"
    private val dbContext = context
    private val gson = Gson()

    fun savePostID(id: String) {
        var savedPostIDs = retrieveSavedPostIDs()
        savedPostIDs.add(id)
        var jsonString = gson.toJson(savedPostIDs)

        Log.d("Database", "Saved Json $jsonString")

        val sharedPreference = dbContext.getSharedPreferences(POST_IDS, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putString(POST_IDS, jsonString)
        editor.apply()
    }

    fun retrieveSavedPostIDs(): MutableList<String> {
        val savedJson = dbContext.getSharedPreferences(POST_IDS, Context.MODE_PRIVATE).getString(
            POST_IDS,
            null
        )

        Log.d("Database", "Loaded Json savedPostIDs $savedJson")

        if (savedJson != null) {
            val type: Type = object : TypeToken<List<String?>?>() {}.getType()
            var savedPostIDs = gson.fromJson<MutableList<String>>(savedJson, type)
            return savedPostIDs
        }
        return mutableListOf<String>()
    }


    fun saveToPrefsInt(key: String, value: Int) {
        val sharedPreference = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun retrievePrefsInt(key: String, defValue: Int): Int {
        val savedInt = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE).getInt(
            key,
            defValue
        )
        return savedInt
    }

    fun convertUserObject(user: MemeDomUser, key: String) {
        var jsonString = gson.toJson(user)

        Log.d("Database", "Saved Json $jsonString")

        val sharedPreference = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putString(key, jsonString)
        editor.apply()
    }

    fun retrieveSavedUser(): MemeDomUser? {
        val savedJson = dbContext.getSharedPreferences(MAIN_USER, Context.MODE_PRIVATE).getString(
            MAIN_USER,
            null
        )

        Log.d("Database", "Loaded Json $savedJson")

        if (savedJson != null) {
            var memeDomUser = gson.fromJson(savedJson, MemeDomUser::class.java)
            return memeDomUser
        }
        return null
    }

    fun getMainUserID(): String? {
        val savedJson = dbContext.getSharedPreferences(MAIN_USER, Context.MODE_PRIVATE).getString(
            MAIN_USER,
            null
        )
        if(savedJson != null) {
            val memeDomUser = gson.fromJson(savedJson, MemeDomUser::class.java)
            return memeDomUser.uid
        }
        return null
    }
}