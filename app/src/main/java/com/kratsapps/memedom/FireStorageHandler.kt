package com.kratsapps.memedom

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class FireStorageHandler {
    val storageRef = Firebase.storage.reference
    val profilePhotoRef = storageRef.child("ProfilePhotos")

    fun uploadPhotoWith(id: String, byteArray: ByteArray, success: (String?) -> Unit) {
        val photoRef = profilePhotoRef.child(id)
        var uploadTask = photoRef.putBytes(byteArray)

        val urlTask = uploadTask.addOnFailureListener{ e ->
            success(null)
            Log.d("Storage", "Image not saved", e)
        }.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener {
                val downloadURI = it.toString()
                Log.d("Storage", "Image saved ${it.toString()}")
                success(downloadURI)
            }
        }
    }
}