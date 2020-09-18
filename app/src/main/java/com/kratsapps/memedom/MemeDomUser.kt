package com.kratsapps.memedom

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MemeDomUser: Serializable {
    var name: String = "Anonymous"
    var gender: String = ""
    var birthday: String = ""
    var profilePhoto: String = ""
    var uid: String = ""
    var email: String = ""

    fun getUserAge(): Int {
        return 0
    }
}

