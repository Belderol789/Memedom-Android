package com.kratsapps.memedom.firebaseutils

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.models.Comments

class FirestoreCommentsHandler {

    private val firestoreDB = Firebase.firestore
    private val COMMENTS_PATH = "Comments"
    private val MEMES_PATH = "Memes"
    private val DESCENDING = Query.Direction.DESCENDING

    fun sendUserCommentToFirestore(
        postID: String,
        commentID: String,
        newCount: Int,
        hashMap: HashMap<String, Any>
    ) {
        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .set(hashMap)
            .addOnFailureListener {
                Log.d("Comment", "Comment Error $it")
            }

        firestoreDB
            .collection(MEMES_PATH)
            .document(postID)
            .update("postComments", newCount)
    }

    fun sendUserReplyToFirestore(
        comment: Comments,
        hashMap: HashMap<String, Any>
    ) {

        var fieldValue: FieldValue = FieldValue.arrayUnion(hashMap)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(comment.postID)
            .collection(COMMENTS_PATH)
            .document(comment.commentID)
            .update("replies", fieldValue)
            .addOnFailureListener {
                Log.d("Comment", "Comment Error $it")
            }
    }

    fun removeCommentPoints(uid: String,
                            postID: String,
                            commentID: String) {
        var fieldValue: FieldValue = FieldValue.arrayRemove(uid)

        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .document(commentID)
            .update("commentLikers", fieldValue)
    }

    fun checkForComments(postID: String,
                         completed: (List<Comments>) -> Unit) {
        firestoreDB
            .collection(COMMENTS_PATH)
            .document(postID)
            .collection(COMMENTS_PATH)
            .orderBy("commentDate", DESCENDING)
            .get()
            .addOnSuccessListener {
                var comments: List<Comments> = listOf()
                for (document in it) {
                    val comment = document.toObject(Comments::class.java)
                    comments += comment
                }
                completed(comments)
            }
    }

}