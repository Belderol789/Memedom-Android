package com.kratsapps.memedom.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.kratsapps.memedom.CommentsActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.AndroidUtils
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FireStorageHandler
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.fragment_create.*
import java.util.*
import kotlin.collections.HashMap

class CreateFragment : Fragment() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val homeContext = container?.context
        rootView = inflater.inflate(R.layout.fragment_create, container, false)
        setupUI()
        return rootView
    }

    override fun onDestroy() {
        Log.d("CreateActivity", "Resetting")
        resetValues()
        super.onDestroy()
    }

    private fun setupUI() {
        val imageViewMeme = rootView.findViewById(R.id.imageViewMeme) as ImageView
        val buttonPost = rootView.findViewById(R.id.buttonPost) as Button
        val addImageButton = rootView.findViewById(R.id.addImageButton) as ImageButton

        resetValues()

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

        var progressOverlay: View = rootView.findViewById(R.id.progress_overlay)

        val title = editTextTitle.text.toString()
        val postID = generateRandomString()
        val today = System.currentTimeMillis()

        val savedUser = DatabaseManager(this.context!!).retrieveSavedUser()

        AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

        FireStorageHandler().uploadPhotoWith(postID, imageViewMeme.drawable, {
            val memeImageURL = it
            if (memeImageURL != null && savedUser != null) {

                val newPost: HashMap<String, Any> = hashMapOf(
                    "postID" to postID,
                    "postTitle" to title,
                    "postDate" to today,
                    "postImageURL" to memeImageURL,
                    "postComments" to 0,
                    "postReports" to 0,
                    "postShares" to 0,
                    "postLikers" to arrayListOf<String>(savedUser.uid),
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
                        resetValues()
                        val meme = Memes()
                        meme.postID = postID
                        meme.postTitle = title
                        meme.postDate = today
                        meme.postShares = 0
                        meme.postReports = 0
                        meme.postComments = 0
                        meme.postImageURL = memeImageURL
                        meme.postLikers = arrayListOf(savedUser.uid)
                        meme.postUsername = savedUser.name
                        meme.postProfileURL = savedUser.profilePhoto
                        meme.postUserUID = savedUser.uid
                        navigateToComments(meme)
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
                val postButton = rootView.findViewById(R.id.buttonPost) as Button
                postButton.alpha = 1.0f
                val imageData = data.data
                Glide.with(this)
                    .load(imageData)
                    .centerCrop()
                    .into(imageViewMeme)
                addImageButton.setImageResource(R.drawable.ic_action_delete)
            }
        }
    }

    private fun resetValues() {
        val imageViewMeme = rootView.findViewById(R.id.imageViewMeme) as ImageView
        val buttonPost = rootView.findViewById(R.id.buttonPost) as Button
        val postTitle = rootView.findViewById(R.id.editTextTitle) as EditText

        buttonPost.alpha = 0.25f
        imageViewMeme.setImageDrawable(null)
        postTitle.setText(null)
    }

    private fun setupAlertDialog(message: String?) {
        val builder = AlertDialog.Builder(this.context!!)
        builder.setTitle("Post Error")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            Toast.makeText(this.context, R.string.alert_ok, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(activity, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        startActivity(intent)
    }
}

