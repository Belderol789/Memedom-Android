package com.kratsapps.memedom.models

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MemeDomUser: Serializable {
    var bio: String = "" //8
    var birthday: String = "" //4
    var dateJoined: Long = 0
    var dating: HashMap<String, Any> = hashMapOf() // 16
    var email: String = "" //7
    var gallery: List<String> = listOf() //11
    var gender: String = "Male" //2
    var lookingFor: String = "Male" //3
    var matches: List<String> = listOf() //14
    var maxAge: Int = 65 //10
    var memes: List<String> = listOf()
    var minAge: Int = 16 //9
    var name: String = "" //1
    var profilePhoto: String = "" //5
    var rejects: List<String> = listOf() //13
    var seenOldMemes: List<String> = listOf()
    var uid: String = "" //6

    var pendingMatches: List<String> = listOf()

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

