package com.kratsapps.memedom.firebaseutils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import java.io.ByteArrayOutputStream

class FireStorageHandler {
    val storageRef = Firebase.storage.reference
    val profilePhotoRef = storageRef.child("ProfilePhotos")
    val galleryPhotoRef = storageRef.child("GalleryPhotos")
    val chatPhotoRef = storageRef.child("ChatPhotos")

    fun uploadMemePhotoWith(id: String, image: Drawable?, context: Context, success: (String) -> Unit) {
        val mainUser = DatabaseManager(context).retrieveSavedUser()
        val bitmap = (image as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val photoRef = profilePhotoRef.child(id)
        var uploadTask = photoRef.putBytes(data)

        uploadTask.addOnFailureListener{ e ->
            Log.d("Storage", "Image not saved", e)
        }.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener {
                val downloadURI = it.toString()
                Log.d("Storage", "Image saved ${it.toString()}")
                if (it != null) {
                    success(it.toString())

                    if (mainUser != null) {
                        mainUser.memes += it.toString()
                        DatabaseManager(context).convertUserObject(mainUser, "MainUser")
                    }
                }
            }
        }
    }

    fun uploadPhotoWith(id: String, image: Drawable?, success: (String) -> Unit) {

        val bitmap = (image as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val photoRef = profilePhotoRef.child(id)
        var uploadTask = photoRef.putBytes(data)

        uploadTask.addOnFailureListener{ e ->
            Log.d("Storage", "Image not saved", e)
        }.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener {
                val downloadURI = it.toString()
                Log.d("Storage", "Image saved ${it.toString()}")
                if (it != null) {
                    success(it.toString())
                }
            }
        }
    }

    fun uploadChatMeme(chatID: String, chatUniqueID: String, imageUri: Uri, type: Long, userID: String, context: Context) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uniquePhotoID = generateRandomString()

        val photoRef = chatPhotoRef.child(chatUniqueID).child(uniquePhotoID)
        var uploadTask = photoRef.putBytes(data)

        val urlTask = uploadTask.addOnFailureListener{ e ->
            Log.d("Storage", "Galery not saved", e)
        }.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener {
                val downloadURI = it.toString()
                Log.d("Storage", "Chat Image saved ${it.toString()}")
                FirestoreHandler().sendUserChat(chatID, chatUniqueID, downloadURI, "", type, userID)
            }
        }
    }

    fun uploadGallery(mainUserID: String, imageUri: Uri, context: Context, success: (String) -> Unit) {

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uniquePhotoID = generateRandomString()

        val photoRef = galleryPhotoRef.child(mainUserID).child(uniquePhotoID)
        var uploadTask = photoRef.putBytes(data)

        val urlTask = uploadTask.addOnFailureListener{ e ->
            Log.d("Storage", "Galery not saved", e)
        }.addOnSuccessListener { taskSnapshot ->
            photoRef.downloadUrl.addOnSuccessListener {
                val downloadURI = it.toString()
                Log.d("Storage", "Gallery saved ${it.toString()}")
                success(downloadURI)
            }
        }
    }

    private fun generateRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { charset.random() }
            .joinToString("")
    }

}