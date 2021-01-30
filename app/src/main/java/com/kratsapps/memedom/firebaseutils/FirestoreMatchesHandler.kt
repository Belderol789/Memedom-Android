package com.kratsapps.memedom.firebaseutils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager

class FirestoreMatchesHandler {

    private val firestoreDB = Firebase.firestore
    private val MATCHED = "Matched"
    private val MATCH_LIMIT = "matchLimit"
    private val USERS_PATH = "User"
    private val DESCENDING = Query.Direction.DESCENDING

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

                        if (value >= matchLimit.toLong() &&
                            memeDomuser != null &&
                            !memeDomuser.matches.contains(key)
                            && !memeDomuser.rejects.contains(key)
                            && !memeDomuser.pendingMatches.contains(key)
                        ) {
                            FirestoreHandler().getUsersDataWith(key, {
                                if (it != null) {
                                    popUpData(it)
                                }
                            })
                        }
                    }
                }
            }
    }

    fun unmatchUserWithID(matchUserID: String, mainuserID: String, completed: () -> Unit) {
        val userIDs = matchUserID + mainuserID
        val messageUniqueID = userIDs.toCharArray().sorted().joinToString("")

        firestoreDB
            .collection(MATCHED)
            .document(messageUniqueID)
            .delete()
            .addOnSuccessListener {
                completed()
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
            if (memeDomuser.matches.contains(matchUser.uid)) {
                memeDomuser.matches -= matchUser.uid
            }
            DatabaseManager(context).convertUserObject(memeDomuser, {})
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
                        context: Context
    ) {

        val mainUser = DatabaseManager(context).retrieveSavedUser()
        val today = System.currentTimeMillis()

        val userIDs = matchUser.uid + mainUser!!.uid
        val messageUniqueID = userIDs.toCharArray().sorted().joinToString("")
        //What if na like na
        if (mainUser != null) {
            mainUser.pendingMatches += matchUser.uid

            DatabaseManager(context).convertUserObject(mainUser, {})

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
        if (mainUser != null) {
            var fieldValue: FieldValue = FieldValue.arrayUnion(matchUserUID)
            updateLikeDatabase(mainUser.uid, matchUserUID, context, 1, {
                FirestoreHandler().updateDatabaseObject(USERS_PATH, mainUser.uid, hashMapOf("matches" to fieldValue))
                completed()
            })
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


    fun updateLikeDatabase(mainuserID: String, postUserID: String, context: Context, plus: Long, completed: () -> Unit) {
        val mainUser = DatabaseManager(context).retrieveSavedUser()

        if (mainUser != null) {
            getCurrentNumberOfLikes(mainuserID, postUserID, "dating", {
                val updatedCount = it?.plus(plus)
                val updatedLike = hashMapOf(postUserID to updatedCount)
                val updateLiked: HashMap<String, Any> = hashMapOf("dating" to updatedLike)

                firestoreDB
                    .collection(USERS_PATH)
                    .document(mainuserID)
                    .set(updateLiked, SetOptions.merge())
                    .addOnSuccessListener {
                        completed()
                    }
            })
        }
        Log.d("Firestore-Liked", "Updating $postUserID to $mainuserID")
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

}