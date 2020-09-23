package com.kratsapps.memedom

import android.util.Log
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val USER_PATH = "Users"

    fun addUserToDatabase(user: MemeDomUser, success: (String?) -> Unit) {

        Log.d("Firestore", "Adding new user ${user.profilePhoto} ${user.birthday}")

        val newUser = hashMapOf(
            "name" to user.name,
            "birthday" to user.birthday,
            "profilePhoto" to user.profilePhoto,
            "uid" to user.uid,
            "email" to user.email
        )

        firestoreDB.collection(USER_PATH).document(user.uid).set(newUser).addOnSuccessListener {
            Log.d("Firestore", "DocumentSnapshot successfully written!")
            success(null)
        }.addOnFailureListener { e ->
            Log.w("Firestore", "Error writing document", e)
            success(e.message)
        }

    }

    fun updateUserDatabase(uid: String, key: String, value: Any) {
        val updatedData = hashMapOf(
            key to value
        )
        firestoreDB.collection(USER_PATH).document(uid).update(updatedData)
    }
}