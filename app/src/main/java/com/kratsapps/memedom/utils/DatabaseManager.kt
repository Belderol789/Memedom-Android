package com.kratsapps.memedom.utils

import android.content.Context
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import java.lang.reflect.Type


class DatabaseManager(context: Context) {

    private val POST_IDS = "PostIDs"
    private val MAIN_USER = "MainUser"
    private val dbContext = context
    private val gson = Gson()

    fun saveToPrefsInt(key: String, value: Int) {
        val sharedPreference = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun saveToPrefsBoolean(key: String, value: Boolean) {
        val sharedPreference = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun retrievePrefsBoolean(key: String, defValue: Boolean): Boolean{
        val savedBoolean = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE).getBoolean(
            key,
            defValue
        )
        return savedBoolean
    }

    fun retrievePrefsInt(key: String, defValue: Int): Int {
        val savedInt = dbContext.getSharedPreferences(key, Context.MODE_PRIVATE).getInt(
            key,
            defValue
        )
        return savedInt
    }

    fun converMemeObject(meme: Memes, completed: (String) -> Unit) {
        var jsonString = gson.toJson(meme)
        completed(jsonString)
    }

    fun retrieveMemeObject(json: String): Memes {
        var meme = gson.fromJson(json, Memes::class.java)
        return meme
    }

    fun convertUserObject(user: MemeDomUser?, completed: () -> Unit) {
        Log.d("Database", "Main Activity Saving Json $user")
        var jsonString = gson.toJson(user)
        val sharedPreference = dbContext.getSharedPreferences("MainUser", Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putString("MainUser", jsonString)
        editor.apply()
        Log.d("Database", "Main Activity Saved Json $jsonString")
        completed()
    }

    fun retrieveSavedUser(): MemeDomUser? {
        val savedJson = dbContext.getSharedPreferences(MAIN_USER, Context.MODE_PRIVATE).getString(
            MAIN_USER,
            null
        )

        Log.d("Saved JSON", "Main Activity $savedJson")

        if (savedJson != null) {
            var memeDomUser = gson.fromJson(savedJson, MemeDomUser::class.java)
            return memeDomUser
        }
        return null
    }
}