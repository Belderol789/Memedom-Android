package com.kratsapps.memedom

import android.util.Log
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreHandler {

val firestoreDB = Firebase.firestore

    fun addUserToDatabase(user: MemeDomUser) {

        Log.d("Firestore", "Adding new user ${user.profilePhoto} ${user.birthday}")

        val newUser = hashMapOf(
            "name" to user.name,
            "birthday" to user.birthday,
            "profilePhoto" to user.profilePhoto,
            "uid" to user.uid,
            "email" to user.email
        )

        firestoreDB.collection("Users")
            .add(newUser).addOnSuccessListener { documentReference ->
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener{ e ->
                Log.w("Firestore", "Error with adding document", e)
            }

    }

}