package com.kratsapps.memedom.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.round

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val POPULAR_POINTS = "PopularPoints"
    private val MEMES_PATH = "Memes"
    private val REPLIES_PATH = "Replies"
    private val COMMENTS_PATH = "Comments"
    private val USERS_PATH = "User"
    private val APP_SETTINGS = "AppSettings"
    private val DAY_LIMIT = "DayLimit"

    private val DESCENDING = Query.Direction.DESCENDING
    private val ASCENDING = Query.Direction.ASCENDING

    //Adding
    fun addDataToFirestore(path: String, document: String, hashMap: HashMap<String, Any>, success: (String?) -> Unit) {
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

    fun sendUserCommentToFirestore(postID: String, commentID: String, newCount: Int, hashMap: HashMap<String, Any>) {
        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .set(hashMap)
            .addOnFailureListener{
                Log.d("Comment", "Comment Error $it")
            }

            firestoreDB
                .collection(MEMES_PATH)
                .document(postID)
                .update("postComments", newCount)
    }

    fun sendUserReplyToFirestore(comment: Comments, replyID: String, replyCount: Int, hashMap: HashMap<String, Any>) {

        firestoreDB
            .collection(REPLIES_PATH)
            .document(comment.commentID)
            .collection(REPLIES_PATH)
            .document(replyID)
            .set(hashMap)
            .addOnFailureListener{
                Log.d("Comment", "Comment Error $it")
            }

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(comment.postID)
            .collection(COMMENTS_PATH)
            .document(comment.commentID)
            .update("commentsRepliesCount", replyCount)
    }

    //Editing
    fun updateLikedDatabase(mainUserID: String, postUserID: String, plus: Long) {
        getCurrentNumberOfLikes(mainUserID, postUserID, {
            val updatedCount = it?.plus(plus)
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

    fun updateArrayDatabaseObject(path: String, document: String, hashMap: HashMap<String, Any>) {
        firestoreDB
            .collection(path)
            .document(document)
            .update(hashMap)
    }

    fun updateCommentPoints(uid: String, postID: String, commentID: String) {

        var fieldValue: FieldValue = FieldValue.arrayUnion(uid)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .update("commentLikers", fieldValue)
    }

    fun removeCommentPoints(uid: String, postID: String, commentID: String) {
        var fieldValue: FieldValue = FieldValue.arrayRemove(uid)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .update("commentLikers", fieldValue)
    }

    //App Settings
    fun getAppSettings(done: (points: Long, days: Long) -> Unit) {

        firestoreDB
            .collection(APP_SETTINGS)
            .document(APP_SETTINGS)
            .get()
            .addOnSuccessListener {

                val popularPoints = it.get(POPULAR_POINTS) as Long
                val dayLimit = it.get(DAY_LIMIT) as Long

                done(popularPoints, dayLimit)
            }
            .addOnFailureListener {
                done(100, 1)
            }
    }

    //Getting
    fun checkForFreshMemes(mainUser: MemeDomUser?, dayLimit: Long, completed: (MutableList<Memes>) -> Unit) {

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
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, dayLimit.toInt() * -1)
        val tomorrow = tomorrowCalendar.timeInMillis

        Log.d(
            "DayLimit",
            "Today ${convertLongToTime(today)} Days after ${convertLongToTime(tomorrow)}"
        )

        firestoreDB
            .collection(MEMES_PATH)
            .whereGreaterThan("postDate", tomorrow)
            .orderBy("postDate", DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { documents ->
                var memes: MutableList<Memes> = arrayListOf()
                for (document in documents) {
                    val newMeme: Memes = document.toObject(Memes::class.java)
                    if (mainUser != null) {
                        if(!mainUser.rejects.contains(newMeme.postUserUID) && !mainUser.rejectedMemes.contains(newMeme.postID)) {
                            memes.add(newMeme)
                        }
                    } else {
                        memes.add(newMeme)
                    }

                }
                completed(memes)
            }
    }

    fun checkForComments(postID: String, completed: (List<Comments>) -> Unit) {
        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .orderBy("commentRepliesCount", DESCENDING)
            .get()
            .addOnSuccessListener {
                var comments: List<Comments> = listOf()
                for (document in it) {
                    val comment = document.toObject(Comments::class.java)
                    comments += comment
                }
                completed(comments)
            }
    }

    fun getUserDataWith(uid: String, completed: (MemeDomUser) -> Unit) {
        firestoreDB
            .collection(USERS_PATH)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val mainUser: MemeDomUser = document.toObject(MemeDomUser::class.java)!!
                completed(mainUser)
            }
    }

    fun getCurrentNumberOfLikes(mainuserID: String, postUserID: String, completed: (currentLikes: Long?) -> Unit) {
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

    fun checkMatchingStatus(context: Context, uid: String, popUpData: (MemeDomUser) -> Unit) {

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
                        val memeDomuser = DatabaseManager(context).retrieveSavedUser()
                        if (value == 10L &&
                            memeDomuser != null &&
                            !memeDomuser.matches.contains(key) &&
                            !memeDomuser.rejects.contains(key))
                        {
                            getUserDataWith(key, {
                                popUpData(it)
                            })
                        }
                    }
                }
            }
    }

    fun rejectUser(matchUser: MemeDomUser, context: Context) {
        val memeDomuser = DatabaseManager(context).retrieveSavedUser()
        if(memeDomuser != null && !memeDomuser.rejects.contains(matchUser.uid)) {
            var fieldValue: FieldValue = FieldValue.arrayUnion(matchUser.uid)
            updateDatabaseObject(USERS_PATH, memeDomuser.uid, hashMapOf("rejects" to fieldValue))

            memeDomuser.rejects += matchUser.uid
            DatabaseManager(context).convertUserObject(memeDomuser, "MainUser")
        }
    }

    fun matchUser(matchUser: MemeDomUser, context: Context) {
        val memeDomuser = DatabaseManager(context).retrieveSavedUser()
        if(memeDomuser != null && !memeDomuser.matches.contains(matchUser.uid)) {
            var fieldValue: FieldValue = FieldValue.arrayUnion(matchUser.uid)
            updateDatabaseObject(USERS_PATH, memeDomuser.uid, hashMapOf("matches" to fieldValue))

            memeDomuser.matches += matchUser.uid
            DatabaseManager(context).convertUserObject(memeDomuser, "MainUser")
        }
    }

    fun getAllReplies(comment: Comments, success: (List<Comments>) -> Unit) {
        firestoreDB
            .collection(REPLIES_PATH)
            .document(comment.commentID)
            .collection(REPLIES_PATH)
            .get()
            .addOnSuccessListener { documents ->

                var replies: List<Comments> = listOf()

                for(reply in documents) {
                    val newReply = reply.toObject(Comments::class.java)
                    Log.d("Replies", "Reply ${newReply.showActions}")
                    replies += newReply
                }
                success(replies)
            }
    }

    fun getAllMemesOfMainUser(uid: String, memes: (MutableList<Memes>) -> Unit) {

        Log.d("UserMemes", "Getting Memes of $uid")

        firestoreDB
            .collection(MEMES_PATH)
            .whereEqualTo("postUserUID", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                var userMemes: MutableList<Memes> = mutableListOf()
                for (document in snapshot) {
                    val userMeme = document.toObject(Memes::class.java)
                    userMemes.add(userMeme)
                }

                Log.d("UserMemes", "Got memes $userMemes")

                memes(userMemes)
            }
            .addOnFailureListener {
                Log.d("UserNames", "Failed to get memes ${it.localizedMessage}")
            }
    }

    // Extras

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

}

/*


//    fun checkForPopularMemes(context: Context, popularPoints: Long, completed: (List<Memes>) -> Unit) {
//        firestoreDB
//            .collection(MEMES_PATH)
//            .whereGreaterThanOrEqualTo("postPoints", popularPoints)
//            .get()
//            .addOnSuccessListener { documents ->
//
//                val memes = ArrayList<Memes>()
//
//                for (document in documents) {
//                    val newMeme: Memes = document.toObject(Memes::class.java)
//                    memes += newMeme
//                }
//
//                val shuffledMemes = memes.shuffled()
//                completed(shuffledMemes)
//            }
//    }

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


/*

    fun checkForMemeChanges(dayLimit: Long, uid: String, completed: (Memes) -> Unit) {
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
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, dayLimit.toInt() * -1)
        val tomorrow = tomorrowCalendar.timeInMillis

        Log.d(
            "DayLimit",
            "Today ${convertLongToTime(today)} Days after ${convertLongToTime(tomorrow)}"
        )

        firestoreDB
            .collection(MEMES_PATH)
            .whereArrayContains("postLikers", uid)
            .addSnapshotListener { value, e ->
                Log.d("Firestore", "Got Updated Posts ${value?.count()}")
            }
    }
 */