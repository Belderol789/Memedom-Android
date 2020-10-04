package com.kratsapps.memedom

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.ArrayList

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val MEMES_PATH = "Memes"
    private val USERS_PATH = "User"

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
            .update("postLikers", fieldValue,
            "postPoints", points)
    }

    //Getting
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

    fun checkForPopularMemes(completed: (List<Memes>) -> Unit) {
        firestoreDB
            .collection(MEMES_PATH)
            .whereGreaterThanOrEqualTo("postPoints", 200)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen faield", e)
                    return@addSnapshotListener
                }

                val memes = ArrayList<Memes>()

                for (document in value!!) {
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
                                Log.d("Firestore-matching", "Got user data ${it.name} ${it.profilePhoto}")
                            })
                            // display popup
                        }
                    }
                }
            }
    }
}

