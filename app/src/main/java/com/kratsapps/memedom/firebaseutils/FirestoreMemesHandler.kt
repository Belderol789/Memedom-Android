package com.kratsapps.memedom.firebaseutils

import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import java.text.SimpleDateFormat
import java.util.*

class FirestoreMemesHandler {

    private val firestoreDB = Firebase.firestore
    private val DESCENDING = Query.Direction.DESCENDING
    private val MEMES_PATH = "Memes"

    //Getting
    fun getAllMemes(
        mainUser: MemeDomUser?,
        dayLimit: Long,
        memeLimit: Long,
        completed: (MutableList<Memes>) -> Unit) {

        val todayCalendar: Calendar = GregorianCalendar()
        todayCalendar[Calendar.HOUR_OF_DAY] = 0
        todayCalendar[Calendar.MINUTE] = 0
        todayCalendar[Calendar.SECOND] = 0
        todayCalendar[Calendar.MILLISECOND] = 0
        val today = todayCalendar.timeInMillis

        val tomorrowCalendar: Calendar = GregorianCalendar()
        tomorrowCalendar[Calendar.HOUR_OF_DAY] = 0
        tomorrowCalendar[Calendar.MINUTE] = 0
        tomorrowCalendar[Calendar.SECOND] = 0
        tomorrowCalendar[Calendar.MILLISECOND] = 0
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, dayLimit.toInt() * -1)
        val tomorrow = tomorrowCalendar.timeInMillis

        Log.d(
            "DayLimit",
            "Today ${convertLongToTime(today)} Days after ${convertLongToTime(tomorrow)} with day limit $dayLimit"
        )

        val minValue = if (mainUser != null) mainUser?.minAge else 16
        val maxValue = if (mainUser != null) mainUser?.maxAge else 65

        firestoreDB
            .collection(MEMES_PATH)
            .whereGreaterThanOrEqualTo("postDate", tomorrow)
            .orderBy("postDate", DESCENDING)
            .limit(memeLimit)
            .get()
            .addOnSuccessListener { documents ->

                Log.d("Filtering", "Found memes ${documents.count()}")

                var memes: MutableList<Memes> = arrayListOf()
                for (document in documents) {

                    val newMeme: Memes = document.toObject(Memes::class.java)

                    Log.d(
                        "Filtering-Memes",
                        "MemeAge ${newMeme.userAge} min $minValue max $maxValue"
                    )
                    //!mainUser.seenOldMemes.contains(newMeme.postID)
                    if (mainUser != null) {
                        if (!mainUser.rejects.contains(newMeme.postUserUID)) {
                            Log.d("Memes", "User is not null")
                            memes.add(newMeme)
                        }
                    } else {
                        Log.d("Memes", "User is null")
                        memes.add(newMeme)
                    }
                }

                Log.d("Memes", "Completed getting memes $memes")
                completed(memes)
            }
    }

    fun getAllMemesOfMainUser(uid: String,
                              memes: (Memes?) -> Unit) {

        Log.d("UserMemes", "Getting Memes of $uid")

        firestoreDB
            .collection(MEMES_PATH)
            .whereEqualTo("postUserUID", uid)
            .orderBy("postPoints", DESCENDING)
            .limit(10)
            .addSnapshotListener {snapshot, e ->
                if (e != null) {
                    Log.w("Profile-memes", "listen failed $e")
                    memes(null)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (snapshot in snapshot!!.documents) {
                        val userMeme = snapshot.toObject(Memes::class.java)
                        if (userMeme != null) {
                            memes(userMeme)
                        }
                    }
                } else {
                    memes(null)
                }
            }
    }

    // Extras
    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
        return format.format(date)
    }

}