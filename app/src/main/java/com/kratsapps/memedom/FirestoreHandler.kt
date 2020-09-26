package com.kratsapps.memedom

import android.util.Log
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.ArrayList

class FirestoreHandler {

    private val firestoreDB = Firebase.firestore
    private val MEMES_PATH = "Memes"

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
    fun updateDatabaseObject(path: String, document: String, hashMap: HashMap<String, Any>) {
        firestoreDB
            .collection(path)
            .document(document)
            .update(hashMap)
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

    fun checkForNewMemes(completed: (List<Memes>) -> Unit) {
        firestoreDB
            .collection(MEMES_PATH)
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
                completed(memes)
            }
    }
}