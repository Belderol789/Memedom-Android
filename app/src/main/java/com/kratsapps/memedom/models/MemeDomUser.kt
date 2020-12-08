package com.kratsapps.memedom.models

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MemeDomUser: Serializable {
    var name: String = "" //1
    var gender: String = "Male" //2
    var lookingFor: String = "Female" //3
    var birthday: String = "" //4
    var profilePhoto: String = "" //5
    var uid: String = "" //6
    var email: String = "" //7
    var bio: String = "" //8
    var minAge: Int = 16 //9
    var maxAge: Int = 65 //10
    var gallery: List<String> = listOf() //11
    var memes: List<String> = listOf() //12

    var rejects: List<String> = listOf() //13
    var matches: List<String> = listOf() //14
    var liked: HashMap<String, Any> = hashMapOf() //15
    var dating: HashMap<String, Any> = hashMapOf() // 16

    var seenOldMemes: List<String> = listOf()

    fun getUserAge(): Int {
        val sdf = SimpleDateFormat("MM/dd/yyyy")
        if (birthday.isEmpty()) {
            return 0
        }
        val date = sdf.parse(birthday)
        val currentDate = Date()
        val userAge = (currentDate.time - date.time) / 86400000 / 365
        return userAge.toInt()
    }
}

