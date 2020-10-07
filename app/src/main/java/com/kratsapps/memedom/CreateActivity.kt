package com.kratsapps.memedom

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.kratsapps.memedom.utils.AndroidUtils
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FireStorageHandler
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.activity_create.*
import java.util.*
import kotlin.collections.HashMap

class CreateActivity : AppCompatActivity() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        Log.i("Navigation", "Navigated to Create")
        setupUI()

    }

    private fun setupUI() {
        imageViewMeme.setImageDrawable(null)

        addImageButton.setOnClickListener {

            val hasMeme: Boolean = imageViewMeme.drawable != null

            Log.d("Create", "ImageView has meme $hasMeme")
            if (hasMeme) {
                imageViewMeme.setImageDrawable(null)
                addImageButton.setImageResource(R.drawable.ic_action_create)
            } else {
                prepOpenImageGallery()
            }
        }

        buttonPost.setOnClickListener {
            if (imageViewMeme.drawable != null) {
                Log.d("Create", "Has Image from gallery")
                it.visibility = View.INVISIBLE
                sendPostToFirestore()
            } else {
                setupAlertDialog("Meme is missing!")
            }
        }
    }

    private fun sendPostToFirestore() {

        var progressOverlay: View = findViewById(R.id.progress_overlay)

        val title = editTextTitle.text.toString()
        val postID = generateRandomString()
        val today = GregorianCalendar().timeInMillis

        val savedUser = DatabaseManager(this).retrieveSavedUser("MainUser")

        AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

        FireStorageHandler().uploadPhotoWith(postID, imageViewMeme.drawable, {

            if (it != null && savedUser != null) {
                val newPost: HashMap<String, Any> = hashMapOf(
                    "postID" to postID,
                    "postTitle" to title,
                    "postDate" to today,
                    "postImageURL" to it,
                    "postComments" to 0,
                    "postReports" to 0,
                    "postLikers" to arrayListOf<String>(),
                    "postUsername" to savedUser.name,
                    "postProfileURL" to savedUser.profilePhoto,
                    "postUserUID" to savedUser.uid,
                    "postPoints" to 1
                )

                FirestoreHandler().addDataToFirestore("Memes", postID, newPost, {
                    progressOverlay.visibility = View.GONE
                    buttonPost.visibility = View.VISIBLE
                    if (it != null) {
                        setupAlertDialog(it)
                    } else {
                        onBackPressed()
                    }
                })
            } else {
                // show alert
                setupAlertDialog("Ooops, we failed to share your amazing meme :(")
            }
        })
    }

    private fun generateRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { charset.random() }
            .joinToString("")
    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {
                val imageData = data.data
                Glide.with(this)
                    .load(imageData)
                    .centerCrop()
                    .into(imageViewMeme)
                addImageButton.setImageResource(R.drawable.ic_action_delete)
            }
        }
    }

    private fun setupAlertDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Post Error")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            Toast.makeText(applicationContext, R.string.alert_ok, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun navigateToComments() {
        val intent: Intent = Intent(this, CommentsActivity::class.java)
        startActivity(intent)
    }

}

