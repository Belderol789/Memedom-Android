package com.kratsapps.memedom

import android.util.Log
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val USER_PATH = "Users"

    fun addDataToFirestore(path: String, document: String, hashMap: HashMap<String, Any>, success: (String?) -> Unit) {
        firestoreDB.collection(path).document(document).set(hashMap).addOnSuccessListener {
            Log.d("Firestore", "DocumentSnapshot successfully written for $path")
            success(null)
        }.addOnFailureListener { e ->
            Log.w("Firestore", "Error writing document", e)
            success(e.message)
        }
    }

    fun updateDatabaseObject(path: String, document: String, hashMap: HashMap<String, Any>) {
        firestoreDB.collection(path).document(document).update(hashMap)
    }

}