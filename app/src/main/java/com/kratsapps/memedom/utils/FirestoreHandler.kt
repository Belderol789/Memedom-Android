package com.kratsapps.memedom.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val MEMES_PATH = "Memes"
    private val USERS_PATH = "User"
    private val APP_SETTINGS = "AppSettings"
    private val POPULAR_POINTS = "PopularPoints"
    private val DAY_LIMIT = "DayLimit"

    //Adding
    fun addDataToFirestore(
        path: String,
        document: String,
        hashMap: HashMap<String, Any>,
        success: (String?) -> Unit
    ) {
        firestoreDB
            .collection(path)
            .document(document)
            .set(hashMap)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written for $path")
                success(null)
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
                success(e.message)
            }
    }

    //Editing
    fun updateLikedDatabase(mainUserID: String, postUserID: String) {
        getCurrentNumberOfLikes(mainUserID, postUserID, {
            val updatedCount = it?.plus(1)
            val updatedLike = hashMapOf(postUserID to updatedCount)
            val updateLiked: HashMap<String, Any> = hashMapOf(
                "liked" to updatedLike
            )
            firestoreDB
                .collection(USERS_PATH)
                .document(mainUserID)
                .set(updateLiked, SetOptions.merge())

        })
        Log.d("Firestore-Liked", "Updating $postUserID to $mainUserID")
    }

    fun updateDatabaseObject(path: String, document: String, hashMap: HashMap<String, Any>) {
        firestoreDB
            .collection(path)
            .document(document)
            .update(hashMap)
    }

    fun updateArrayDatabaseObject(path: String, document: String, value: String, points: Long) {
        val fieldValue = FieldValue.arrayUnion(value)
        firestoreDB
            .collection(path)
            .document(document)
            .update(
                "postLikers", fieldValue,
                "postPoints", points
            )
    }

    //App Settings
    fun getAppSettings(context: Context, done: (points: Long, days: Long) -> Unit) {

        Log.d("App Settings", "Getting app settings")

        val sharedPreference = context.getSharedPreferences(POPULAR_POINTS, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()

        firestoreDB
            .collection(APP_SETTINGS)
            .document(APP_SETTINGS)
            .get()
            .addOnSuccessListener {

                val popularPoints = it.get(POPULAR_POINTS) as Long
                val dayLimit = it.get(DAY_LIMIT) as Long

                done(popularPoints, dayLimit)
            }
            .addOnFailureListener{
                done(100, 1)
            }
    }

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
        return format.format(date)
    }

    fun Long.round(decimals: Int): Long {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (round(this * multiplier) / multiplier).toLong()
    }

    //Getting
    fun checkForFreshMemes(dayLimit: Long, completed: (List<Memes>) -> Unit) {

        Log.d("DayLimit", "Current day limit $dayLimit")

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
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, -3)
        val tomorrow = tomorrowCalendar.timeInMillis

        Log.d("DayLimit", "Today ${convertLongToTime(today)} Days after ${convertLongToTime(tomorrow)}")

        firestoreDB
            .collection(MEMES_PATH)
            .whereGreaterThan("postDate", tomorrow)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val memes = ArrayList<Memes>()
                for (doc in value!!) {

                    val memeId = doc["postID"]
                    val memePoints = doc["postPoints"]

                    Log.d("Firestore", "$memeId with points $memePoints")

                    val newMeme = doc.toObject(Memes::class.java)
                    memes += newMeme
                }
                completed(memes)
            }

    }

    fun checkForPopularMemes(
        context: Context,
        popularPoints: Long,
        completed: (List<Memes>) -> Unit
    ) {
        firestoreDB
            .collection(MEMES_PATH)
            .whereGreaterThanOrEqualTo("postPoints", popularPoints)
            .get()
            .addOnSuccessListener { documents ->

                val memes = ArrayList<Memes>()

                for (document in documents) {
                    val newMeme: Memes = document.toObject(Memes::class.java)
                    memes += newMeme
                }

                val shuffledMemes = memes.shuffled()
                completed(shuffledMemes)
            }
    }

    fun getUserDataWith(uid: String, completed: (MemeDomUser) -> Unit) {
        firestoreDB
            .collection(USERS_PATH)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                Log.d("Firestore", "${document.id} => ${document.data}")
                val mainUser: MemeDomUser = document.toObject(MemeDomUser::class.java)!!
                completed(mainUser)
            }
    }

    fun getCurrentNumberOfLikes(
        mainuserID: String,
        postUserID: String,
        completed: (currentLikes: Long?) -> Unit
    ) {
        firestoreDB
            .collection(USERS_PATH)
            .document(mainuserID)
            .get()
            .addOnSuccessListener {
                val likedHashMap = it.get("liked") as HashMap<String, Long>

                Log.d("Firestore-Liked", "Got like hashmap $likedHashMap with uid as $postUserID")

                val userLiked = likedHashMap.get(postUserID)
                if (userLiked != null) {
                    Log.d("Firestore-Liked", "Current number $userLiked")
                    completed(userLiked)
                } else {
                    completed(0)
                }
            }
            .addOnFailureListener {
                completed(0)
            }
    }

    fun checkMatchingStatus(uid: String) {

        Log.d("Firestore-matching", "$uid")

        firestoreDB
            .collection(USERS_PATH)
            .document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore-matching", "listen failed $e")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val likedHashMap = snapshot.get("liked") as HashMap<String, Long>
                    for ((key, value) in likedHashMap) {
                        Log.d("Firestore-matching", "Current users $key and $value")
                        if (value >= 10) {
                            getUserDataWith(key, {
                                Log.d(
                                    "Firestore-matching",
                                    "Got user data ${it.name} ${it.profilePhoto}"
                                )
                            })
                            // display popup
                        }
                    }
                }
            }
    }
}

/*
.get()
            .addOnSuccessListener { documents ->

                val memes = ArrayList<Memes>()

                Log.d("Timestamps", "Got memes ${documents.size()}")

                for (document in documents) {
                    val newMeme: Memes = document.toObject(Memes::class.java)
                    memes += newMeme
                }

                val shuffledMemes = memes.shuffled()
                completed(shuffledMemes)
            }
 */


/*
fun getAllMemeObjects(completed: (List<Memes>) -> Unit) {

    var memes: List<Memes> = listOf<Memes>()

    firestoreDB
        .collection(MEMES_PATH)
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                Log.d("Firestore", "${document.id} => ${document.data}")
                val newMeme: Memes = document.toObject(Memes::class.java)
                memes += newMeme
            }
            Log.d("Firestore", "Getting all memes ${memes.size}")
            completed(memes)
        }
        .addOnFailureListener { exception ->
            Log.w("Firestore", "Error getting documents: ", exception)
            completed(memes)
        }
}
*/
