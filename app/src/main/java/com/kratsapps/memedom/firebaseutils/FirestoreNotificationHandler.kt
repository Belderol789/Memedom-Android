package com.kratsapps.memedom.firebaseutils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.models.Notification
import com.kratsapps.memedom.utils.DatabaseManager

class FirestoreNotificationHandler {

    private val firestoreDB = Firebase.firestore
    private val NOTIF_PATH = "Notifications"
    private val MEMES_PATH = "Memes"

    fun getAllUserNotifications(savedNotifIDs: MutableSet<String>, userID: String, notif: (Notification) -> Unit) {
        if (savedNotifIDs.isEmpty()) {
            firestoreDB
                .collection(NOTIF_PATH)
                .document(userID)
                .collection(NOTIF_PATH)
                .limit(10)
                .get()
                .addOnSuccessListener {
                    for(document in it.documents) {
                        val notification = document.toObject(Notification::class.java)
                        if (notification != null) {
                            notif(notification)
                        }
                    }
                }
        } else {
            firestoreDB
                .collection(NOTIF_PATH)
                .document(userID)
                .collection(NOTIF_PATH)
                .whereNotIn("notifID", savedNotifIDs.toMutableList())
                .limit(10)
                .get()
                .addOnSuccessListener {
                    for(document in it.documents) {
                        val notification = document.toObject(Notification::class.java)
                        if (notification != null) {
                            notif(notification)
                        }
                    }
                }
        }
    }

    fun getMemeFromNotif(postID: String, completed: (Memes) -> Unit) {
        firestoreDB
            .collection(MEMES_PATH)
            .document(postID)
            .get()
            .addOnSuccessListener {
                val meme = it.toObject(Memes::class.java)
                if (meme != null) {
                    completed(meme)
                }
            }
    }

    fun updateUserNotification(context: Context, userID: String, contentID: String, isLike: Boolean, number: Int) {
        val mainUser = DatabaseManager(context).retrieveSavedUser()
        var notifTitle: String = ""
        var notifText: String = ""

        Log.d("Notification", "Notif Number $number isLike $isLike")

        if (mainUser != null && userID != mainUser.uid) {
            if (isLike) {
                if (number == 2) {
                    notifTitle = "${mainUser.name} has liked your meme!"
                    notifText = "Could this be the start of something new?"
                } else if (number % 10 == 0) {
                    notifTitle = "You're rich! (with internet points)"
                    notifText = "$number people have liked your meme!"
                }
            } else {
                if (number == 1) {
                    notifTitle = "${mainUser.name} commented on your meme"
                    notifText = "Remember to be nice and respectful"
                } else if (number % 10 == 0) {
                    notifTitle = "Grab some popcorn"
                    notifText = "$number people have commented on your meme"
                }
            }

            if ((number == 1 && !isLike) || (number == 2 && isLike) || (number % 10 == 0)) {

                Log.d("Notifications", "New Notif with number $number")

                val newNotif = Notification()
                newNotif.notifPhotoURL = mainUser.profilePhoto
                newNotif.notifTitle = notifTitle
                newNotif.notifText = notifText
                newNotif.notifContentID = contentID
                newNotif.notifDateLong = System.currentTimeMillis()

                val notifHash = hashMapOf<String, Any>(
                    "notifID" to newNotif.notifID,
                    "notifContentID" to newNotif.notifContentID,
                    "notifTitle" to newNotif.notifTitle,
                    "notifText" to newNotif.notifText,
                    "notifPhotoURL" to newNotif.notifPhotoURL,
                    "notifDateLong" to newNotif.notifDateLong,
                    "notifTapped" to newNotif.notifTapped
                )

                firestoreDB
                    .collection(NOTIF_PATH)
                    .document(userID)
                    .collection(NOTIF_PATH)
                    .document(newNotif.notifContentID)
                    .set(notifHash)
            }
        }
    }

}