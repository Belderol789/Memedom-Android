package com.kratsapps.memedom.firebaseutils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
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
    private val COMMENTS_PATH = "Comments"
    private val USERS_PATH = "User"
    private val APP_SETTINGS = "AppSettings"
    private val DAY_LIMIT = "DayLimit"
    private val MEME_LIMIT = "memeLimit"
    private val MATCH_LIMIT = "matchLimit"
    private val CHAT_PATH = "Chats"

    //Setup
    fun setupFirestore() {
        val settings = firestoreSettings {
            isPersistenceEnabled = false
        }
        firestoreDB.firestoreSettings = settings
    }

    //Checking
    fun checkIfUserExist(uid: String, exist: (Boolean) -> Unit) {
        firestoreDB
            .collection(USERS_PATH)
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    exist(false)
                } else {
                    exist(true)
                }
            }
            .addOnFailureListener {
                exist(false)
            }
    }

    //Adding
    fun addDataToFirestore(
        path: String,
        document: String,
        hashMap: HashMap<String, Any>,
        success: (String?) -> Unit
    ) {
        firestoreDB
            .collection(path)
            .document("Test$document")
            .set(hashMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written for $path $hashMap")
                success(null)
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
                success(e.message)
            }
    }

    fun deleteDataFromFirestore(
        path: String,
        document: String,
        success: () -> Unit) {
        firestoreDB
            .collection(path)
            .document(document)
            .delete()
            .addOnSuccessListener {
                success()
            }
    }

    fun addArrayInFirestore(mainuserID: String, addString: String) {
        firestoreDB
            .collection(USERS_PATH)
            .document(mainuserID)
            .update("gallery", FieldValue.arrayUnion(addString))
    }

    fun checkUsernameAvailability(username: String, available: (Boolean) -> Unit) {
        firestoreDB
            .collection("Username")
            .whereEqualTo("Username", username)
            .get()
            .addOnSuccessListener {
                Log.d("Usernames", "Isername ${it.documents}")
                if (it.isEmpty) {
                    available(true)
                } else {
                    available(false)
                }
            }
            .addOnFailureListener {
                available(true)
            }
    }

    fun deleteArrayInFirestore(path: String, document: String, deleteString: String) {
        firestoreDB
            .collection(path)
            .document(document)
            .update("gallery", FieldValue.arrayRemove(deleteString))
    }

    //Editing
    fun<T: Any> T.getClass(): KClass<T> {
        return javaClass.kotlin
    }

    fun updateDatabaseObject(
        path: String,
        document: String,
        hashMap: HashMap<String, Any>
    ) {
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

    fun getUsersDataWith(uid: String,
                        completed: (MemeDomUser?) -> Unit) {
        firestoreDB
            .collection(USERS_PATH)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val mainUser: MemeDomUser? = document.toObject(MemeDomUser::class.java)
                if (mainUser != null) {
                    completed(mainUser)
                } else {
                    completed(null)
                }
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

    fun Long.round(decimals: Int): Long {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (round(this * multiplier) / multiplier).toLong()
    }

    private fun generateRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { charset.random() }
            .joinToString("")
    }

}
