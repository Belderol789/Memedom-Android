package com.kratsapps.memedom.firebaseutils

import android.content.Context
import android.graphics.Color.convert
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.*
import com.kratsapps.memedom.utils.DatabaseManager
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
    private val MATCHES = "Matched"
    private val CHAT_PATH = "Chats"

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
            .addOnFailureListener {
                Log.d("Comment", "Comment Error $it")
            }

        firestoreDB
            .collection(MEMES_PATH)
            .document(postID)
            .update("postComments", newCount)
    }

    fun sendUserReplyToFirestore(comment: Comments, replyID: String, replyCount: Int, hashMap: HashMap<String, Any>) {

        var fieldValue: FieldValue = FieldValue.arrayUnion(hashMap)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(comment.postID)
            .collection(COMMENTS_PATH)
            .document(comment.commentID)
            .update("replies", fieldValue)
            .addOnFailureListener {
                Log.d("Comment", "Comment Error $it")
            }
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
    fun checkForFreshMemes(context: Context, mainUser: MemeDomUser?, dayLimit: Long, completed: (MutableList<Memes>) -> Unit) {

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

        val minValue = DatabaseManager(context).retrievePrefsInt("minAge", 18)
        val maxValue = DatabaseManager(context).retrievePrefsInt("maxAge", 65)

        var findGender: String = "Female"
        if(mainUser?.gender.equals("Other")) {
            findGender = "Other"
        } else if (mainUser?.gender.equals("Female")) {
            findGender = "Male"
        }

        firestoreDB
            .collection(MEMES_PATH)
            .whereEqualTo("userGender", findGender)
            .whereGreaterThanOrEqualTo("postDate", tomorrow)
            .orderBy("postDate", DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { documents ->

                Log.d("Filtering", "Found memes ${documents.count()} for gender $findGender")

                var memes: MutableList<Memes> = arrayListOf()
                for (document in documents) {

                    val newMeme: Memes = document.toObject(Memes::class.java)

                    Log.d("Filtering-Memes", "MemeAge ${newMeme.userAge} min $minValue max $maxValue")

                    if (mainUser != null) {
                        if (!mainUser.rejects.contains(newMeme.postUserUID)
                            && !mainUser.rejectedMemes.contains(newMeme.postID)
                            && newMeme.userAge.toInt() >= minValue && newMeme.userAge.toInt() <= maxValue) {
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
                    val memeDomuser = DatabaseManager(context).retrieveSavedUser()
                    Log.d(
                        "Firestore-matching",
                        "Matches ${memeDomuser?.matches} Rejects ${memeDomuser?.rejects}"
                    )

                    for ((key, value) in likedHashMap) {

                        Log.d("Firestore-matching", "Current users $key and $value")

                        if (value == 10L &&
                            memeDomuser != null &&
                            !memeDomuser.matches.contains(key)
                            && !memeDomuser.rejects.contains(key)
                        ) {
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
        if (memeDomuser != null && !memeDomuser.rejects.contains(matchUser.uid)) {
            memeDomuser.rejects += matchUser.uid
            DatabaseManager(context).convertUserObject(memeDomuser, "MainUser")
            updateLikedDatabase(memeDomuser.uid!!, matchUser.uid!!, 1)
        }
    }

    fun checkNewMatches(context: Context, completed: (MutableList<Matches>) -> Unit) {
        val mainUser = DatabaseManager(context).retrieveSavedUser()
        if (mainUser != null) {
            firestoreDB
                .collection(MATCHES)
                .document(mainUser.uid)
                .collection(MATCHES)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.w("Firestore-matching", "listen failed $error")
                        return@addSnapshotListener
                    }
                    if (value?.documents != null) {

                        var newMatches: MutableList<Matches> = mutableListOf()

                        for (match in value?.documents) {
                            val matchingUser = match.toObject(Matches::class.java)

                            Log.d("Firestore-matching", "Got matched user ${matchingUser?.uid}")

                            if (matchingUser != null) {
                                newMatches.add(matchingUser)
                            }
                        }

                        completed(newMatches)

                    }
                }
        }
    }

    fun sendMatchToUser(matchUserUID: String, context: Context) {

        val mainUser = DatabaseManager(context).retrieveSavedUser()
        val today = System.currentTimeMillis()

        if (mainUser != null) {
            Log.d(
                "Matching-Sending",
                "Sending Match from ${mainUser.uid} to MatchUser ${matchUserUID}"
            )

            val data: HashMap<String, Any> = hashMapOf(
                "name" to mainUser.name,
                "profilePhoto" to mainUser.profilePhoto,
                "uid" to mainUser.uid,
                "matchDate" to today,
                "matchText" to ""
            )

            firestoreDB
                .collection(MATCHES)
                .document(matchUserUID)
                .collection(MATCHES)
                .document(mainUser.uid)
                .set(data)
        }
        // Send to matching firebase
        matchWithUser(matchUserUID, context)
    }

    fun matchWithUser(matchUserUID: String, context: Context) {
        val memeDomuser = DatabaseManager(context).retrieveSavedUser()
        if (memeDomuser != null && !memeDomuser.matches.contains(matchUserUID)) {
            var fieldValue: FieldValue = FieldValue.arrayUnion(matchUserUID)
            updateDatabaseObject(USERS_PATH, memeDomuser.uid, hashMapOf("matches" to fieldValue))

            memeDomuser.matches += matchUserUID

            DatabaseManager(context).convertUserObject(memeDomuser, "MainUser")
            updateLikedDatabase(memeDomuser.uid!!, matchUserUID, 1)
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

                for (reply in documents) {
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
            .orderBy("postPoints", DESCENDING)
            .limit(50)
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

    // Chat
    fun sendUserChat(chatID: String, chatUniqueID: String, chatImageURL: String, content: String, type: Long, userID: String) {
        val today = System.currentTimeMillis()

        val chatPayload = hashMapOf<String, Any>(
            "chatID" to chatID,
            "chatUserID" to userID,
            "chatType" to type,
            "chatDate" to today,
            "chatContent" to content,
            "chatImageURL" to chatImageURL
        )

        val chatData = hashMapOf<String, Any>(
            chatID to chatPayload
        )

        firestoreDB
            .collection(CHAT_PATH)
            .document(chatUniqueID)
            .set(chatData, SetOptions.merge())
    }

    fun retrieveChats(chatUniqueID: String, contents: (Chat) -> Unit) {
        firestoreDB
            .collection(CHAT_PATH)
            .document(chatUniqueID)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore-matching", "listen failed $e")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val snapshotData = snapshot.data
                    if (snapshotData != null) {
                        for (data in snapshotData) {
                            Log.d("CurrentChat", "Chat Data ${data.value}")
                            val dataValue = data.value as HashMap<String, Any>
                            val chat = Chat()
                            chat.chatContent = dataValue.get("chatContent") as String
                            chat.chatDate = dataValue.get("chatDate") as Long
                            chat.chatType = dataValue.get("chatType") as Long
                            chat.chatID = dataValue.get("chatID") as String
                            chat.chatUserID = dataValue.get("chatUserID") as String
                            chat.chatImageURL = dataValue.get("chatImageURL") as String
                            contents(chat)
                        }
                    }
                }
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
