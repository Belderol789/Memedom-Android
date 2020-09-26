package com.kratsapps.memedom

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class MemeDomUser: Serializable {
    var name: String = ""
    var gender: String = ""
    var birthday: String = ""
    var profilePhoto: String = ""
    var uid: String = ""
    var email: String = ""

    fun getUserAge(): Int {
        val sdf = SimpleDateFormat("dd MMMM yyyy")
        val date = sdf.parse(birthday)
        val currentDate = Date()
        val userAge = (currentDate.time - date.time) / 86400000 / 365
        return userAge.toInt()
    }
}

