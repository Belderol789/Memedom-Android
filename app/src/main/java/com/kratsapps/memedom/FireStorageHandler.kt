package com.kratsapps.memedom

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_signup.*
import java.io.ByteArrayOutputStream

class FireStorageHandler {
    val storageRef = Firebase.storage.reference
    val profilePhotoRef = storageRef.child("ProfilePhotos")

    fun uploadPhotoWith(id: String, image: Drawable?, success: (String?) -> Unit) {

        val bitmap = (image as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val photoRef = profilePhotoRef.child(id)
        var uploadTask = photoRef.putBytes(data)

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