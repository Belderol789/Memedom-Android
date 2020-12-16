package com.kratsapps.memedom.firebaseutils

import android.content.Context
import android.util.Log
import com.facebook.internal.Mutable
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
import kotlin.reflect.KClass

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val POPULAR_POINTS = "PopularPoints"
    private val MEMES_PATH = "Memes"
    private val REPLIES_PATH = "Replies"
    private val COMMENTS_PATH = "Comments"
    private val USERS_PATH = "User"
    private val APP_SETTINGS = "AppSettings"
    private val DAY_LIMIT = "DayLimit"
    private val MEME_LIMIT = "memeLimit"
    private val MATCH_LIMIT = "matchLimit"
    private val MATCHED = "Matched"
    private val CHAT_PATH = "Chats"

    private val DESCENDING = Query.Direction.DESCENDING
    private val ASCENDING = Query.Direction.ASCENDING

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
            .set(hashMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written for $path $hashMap")
                success(null)
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
                success(e.message)
            }
    }

    fun sendUserCommentToFirestore(
        postID: String,
        commentID: String,
        newCount: Int,
        hashMap: HashMap<String, Any>
    ) {
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

    fun sendUserReplyToFirestore(
        comment: Comments,
        hashMap: HashMap<String, Any>
    ) {

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

    fun getCurrentNumberOfLikes(
        mainuserID: String,
        postUserID: String,
        matchType: String,
        completed: (currentLikes: Long) -> Unit
    ) {
        firestoreDB
            .collection(USERS_PATH)
            .document(mainuserID)
            .get()
            .addOnSuccessListener {
                val likedHashMap = it.get(matchType) as HashMap<String, Long>

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

    //Editing
    fun<T: Any> T.getClass(): KClass<T> {
        return javaClass.kotlin
    }

    fun updateLikeDatabase(mainuserID: String, postUserID: String, matchType: String, context: Context, plus: Long) {
        val mainUser = DatabaseManager(context).retrieveSavedUser()

        if (mainUser != null) {
            getCurrentNumberOfLikes(mainuserID, postUserID, matchType, {
                val updatedCount = it?.plus(plus)
                val updatedLike = hashMapOf(postUserID to updatedCount)
                val updateLiked: HashMap<String, Any> = hashMapOf(matchType to updatedLike)

                firestoreDB
                    .collection(USERS_PATH)
                    .document(mainuserID)
                    .set(updateLiked, SetOptions.merge())
            })
        }
        Log.d("Firestore-Liked", "Updating $postUserID to $mainuserID")
    }

    fun updateDatabaseObject(path: String,
                             document: String,
                             hashMap: HashMap<String, Any>) {
        firestoreDB
            .collection(path)
            .document(document)
            .update(hashMap)
    }

    fun updateArrayDatabaseObject(path: String,
                                  document: String,
                                  hashMap: HashMap<String, Any>) {
        firestoreDB
            .collection(path)
            .document(document)
            .update(hashMap)
    }

    fun updateCommentPoints(uid: String,
                            postID: String,
                            commentID: String) {

        var fieldValue: FieldValue = FieldValue.arrayUnion(uid)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .update("commentLikers", fieldValue)
    }

    fun removeCommentPoints(uid: String,
                            postID: String,
                            commentID: String) {
        var fieldValue: FieldValue = FieldValue.arrayRemove(uid)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .update("commentLikers", fieldValue)
    }

    //App Settings
    fun getAppSettings(done: (points: Long,
                              days: Long,
                              memeLimit: Long,
                              matchLimit: Long) -> Unit) {

        firestoreDB
            .collection(APP_SETTINGS)
            .document(APP_SETTINGS)
            .get()
            .addOnSuccessListener {

                val popularPoints = it.get(POPULAR_POINTS) as Long
                val dayLimit = it.get(DAY_LIMIT) as Long
                val memeLimit = it.get(MEME_LIMIT) as Long
                val matchLimit = it.get(MATCH_LIMIT) as Long

                done(popularPoints, dayLimit, memeLimit, matchLimit)
            }
            .addOnFailureListener {

                Log.d("AppSettings", "Error ${it.localizedMessage}")

                done(100, 20, 100, 5)
            }
    }

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
                        if (!mainUser.rejects.contains(newMeme.postUserUID)
                            && newMeme.userAge.toInt() >= minValue && newMeme.userAge.toInt() <= maxValue
                        ) {
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

    fun checkForComments(postID: String,
                         completed: (List<Comments>) -> Unit) {
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

    fun getUsersDataWith(uid: String,
                        completed: (MemeDomUser) -> Unit) {
        firestoreDB
            .collection(USERS_PATH)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val mainUser: MemeDomUser = document.toObject(MemeDomUser::class.java)!!
                completed(mainUser)
            }
    }

    //Matching
    fun checkMatchingStatus(context: Context,
                            uid: String,
                            popUpData: (MemeDomUser) -> Unit) {

        Log.d("Firestore-matching", "$uid")
        val matchLimit = DatabaseManager(context).retrievePrefsInt(MATCH_LIMIT, 5)
        Log.d("MatchLimit", "Limit $matchLimit")
        firestoreDB
            .collection(USERS_PATH)
            .document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore-matching", "listen failed $e")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val datingHashMap = snapshot.get("dating") as HashMap<String, Long>
                    val memeDomuser = DatabaseManager(context).retrieveSavedUser()
                    Log.d(
                        "Firestore-matching",
                        "Matches ${memeDomuser?.matches} Rejects ${memeDomuser?.rejects}"
                    )

                    for ((key, value) in datingHashMap) {

                        Log.d("Firestore-matching", "Current users $key and $value")

                        if (value == matchLimit.toLong() &&
                            memeDomuser != null &&
                            !memeDomuser.matches.contains(key)
                            && !memeDomuser.rejects.contains(key)
                        ) {
                            getUsersDataWith(key, {
                                popUpData(it)
                            })
                        }
                    }
                }
            }
    }

    fun unmatchUser(matchID: String) {
        firestoreDB
            .collection(MATCHED)
            .document(matchID)
            .delete()
    }

    fun rejectUser(matchUser: MemeDomUser,
                   context: Context) {
        val memeDomuser = DatabaseManager(context).retrieveSavedUser()
        if (memeDomuser != null && !memeDomuser.rejects.contains(matchUser.uid)) {
            memeDomuser.rejects += matchUser.uid
            DatabaseManager(context).convertUserObject(memeDomuser, "MainUser", {})
        }
    }

    fun getOnlineStatus(matches: MutableList<Matches>, completed: (Matches) -> Unit) {
        for (match in matches) {
            Log.d("MessagesFragment", "Getting online status of ${match.uid}")
            firestoreDB
                .collection("Online")
                .document(match.uid)
                .get()
                .addOnSuccessListener {
                    val onlineDate = it.get("onlineDate") as? Long
                    val onlineStatus = it.get("online") as? Boolean
                    if (onlineDate != null && onlineStatus != null) {
                        match.onlineDate = onlineDate
                        match.online = onlineStatus

                        Log.d("MessagesFragment", "Got Online Status of ${match.uid}")

                        completed(match)
                    }
                }
        }
    }

    fun checkNewMatch(context: Context,
                      completed: (MutableList<Matches>) -> Unit) {

        val mainUser = DatabaseManager(context).retrieveSavedUser()

        if (mainUser != null) {

            Log.d("MessagesFragment", "MainUser ${mainUser.uid}")

            firestoreDB
                .collection(MATCHED)
                .whereArrayContains("uids", mainUser.uid)
                .orderBy("chatDate", DESCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.w("Firestore-matching", "listen failed $error")
                        completed(mutableListOf())
                        return@addSnapshotListener
                    }
                    if (value?.documents != null) {

                        val matches = mutableListOf<Matches>()
                        Log.d("MessagesFragment", "Documents ${value.documents}")
                        for(document in value.documents) {
                            val documentData = document.data
                            if (documentData != null) {
                                val matchObject = Matches()
                                val matchData =
                                    documentData.get(mainUser.uid) as HashMap<String, Any>
                                //user date
                                matchObject.name = matchData.get("name") as String
                                matchObject.profilePhoto = matchData.get("profilePhoto") as String
                                matchObject.uid = matchData.get("uid") as String
                                //general data
                                matchObject.chatDate = documentData.get("chatDate") as Long
                                matchObject.matchText = documentData.get("matchText") as String
                                matchObject.matchStatus = documentData.get("matchStatus") as Boolean
                                matchObject.offered = documentData.get("offered") as String
                                Log.d("MessagesFragment", "Object to be added $matchObject")
                                matches.add(matchObject)
                            }
                        }
                        Log.d("MessagesFragment", "Completed $matches")
                        completed(matches)
                    }
                }
        }
    }

    fun sendToMatchUser(matchUser: MemeDomUser,
                        context: Context) {

        val mainUser = DatabaseManager(context).retrieveSavedUser()
        val today = System.currentTimeMillis()

        val userIDs = matchUser.uid + mainUser!!.uid
        val messageUniqueID = userIDs.toCharArray().sorted().joinToString("")

        if (mainUser != null) {

            val data: HashMap<String, Any> = hashMapOf(
                matchUser.uid to hashMapOf(
                    "name" to mainUser.name,
                    "profilePhoto" to mainUser.profilePhoto,
                    "uid" to mainUser.uid,
                    "online" to true,
                    "onlineDate" to today
                ),
                mainUser.uid to hashMapOf (
                    "name" to matchUser.name,
                    "profilePhoto" to matchUser.profilePhoto,
                    "uid" to matchUser.uid,
                    "online" to true,
                    "onlineDate" to today
                ),
                "onlineDate" to today,
                "chatDate" to today,
                "uids" to listOf(matchUser.uid, mainUser.uid),
                "matchText" to "New Match!",
                "matchStatus" to false,
                "offered" to matchUser.uid
            )

            firestoreDB
                .collection(MATCHED)
                .document(messageUniqueID)
                .set(data)
        }
        // Send to matching firebase
        updateUserLiked(matchUser.uid, context, {})
    }

    fun updateUserLiked(matchUserUID: String,
                        context: Context,
                        completed: () -> Unit) {

        val mainUser = DatabaseManager(context).retrieveSavedUser()
        if (mainUser != null && !mainUser.matches.contains(matchUserUID)) {

            var fieldValue: FieldValue = FieldValue.arrayUnion(matchUserUID)
            updateDatabaseObject(USERS_PATH, mainUser.uid, hashMapOf("matches" to fieldValue))
            mainUser.matches += matchUserUID
            DatabaseManager(context).convertUserObject(mainUser, "MainUser", {})
            updateLikeDatabase(mainUser.uid, matchUserUID, "dating", context, 1)

            completed()
        }
    }

    fun updateMatch(matchUseID: String, data: HashMap<String, Any>, context: Context, completed: () -> Unit) {
        val mainUser = DatabaseManager(context).retrieveSavedUser()
        val userIDs = matchUseID + mainUser!!.uid
        val messageUniqueID = userIDs.toCharArray().sorted().joinToString("")

        if (mainUser != null) {
            firestoreDB
                .collection(MATCHED)
                .document(messageUniqueID)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    completed()
                }
        }
    }

    fun getAllReplies(comment: Comments,
                      success: (List<Comments>) -> Unit) {
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

    // Chat
    fun sendUserChats(chatID: String,
                      chatUniqueID: String,
                      chatImageURL: String,
                      content: String,
                      type: Long,
                      userID: String) {
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
