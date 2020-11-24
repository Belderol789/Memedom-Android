package com.kratsapps.memedom.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.kratsapps.memedom.CommentsActivity
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.AndroidUtils
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.fragment_create.*


class CreateFragment : Fragment() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    lateinit var rootView: View
    lateinit var imageViewMeme: ImageView
    lateinit var buttonPost: Button
    lateinit var addImageButton: ImageButton
    lateinit var createContext: Context

    var imageHeight: Int = 0
    var imageWidth: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_create, container, false)
        setupUI()
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        createContext = context
        Log.d("OnCreateView", "Called Attached")
    }

    override fun onDestroy() {
        Log.d("CreateActivity", "Resetting")
        resetValues()
        super.onDestroy()
    }

    private fun setupUI() {
        imageViewMeme = rootView.findViewById(R.id.imageViewMeme) as ImageView
        buttonPost = rootView.findViewById(R.id.buttonPost) as Button
        addImageButton = rootView.findViewById(R.id.addImageButton) as ImageButton

        resetValues()

        imageViewMeme.setImageDrawable(null)
        addImageButton.setOnClickListener {

            val hasMeme: Boolean = imageViewMeme.drawable != null

            Log.d("Create", "ImageView has meme $hasMeme")
            if (hasMeme) {
                imageViewMeme.setImageDrawable(null)
                buttonPost.isEnabled = false
                addImageButton.setImageResource(R.drawable.ic_action_create)
            } else {
                prepOpenImageGallery()
            }
        }

        buttonPost.setOnClickListener {
            val savedUser = DatabaseManager(createContext).retrieveSavedUser()
            if (imageViewMeme.drawable != null && savedUser != null) {
                Log.d("Create", "Has Image from gallery")
                it.visibility = View.INVISIBLE
                sendPostToFirestore(savedUser)
            } else {
                setupAlertDialog("Meme is missing!")
            }
        }
    }

    private fun sendPostToFirestore(savedUser: MemeDomUser) {

        val mainActivity = activity as MainActivity

        val title = editTextTitle.text.toString()
        val postID = generateRandomString()
        val today = System.currentTimeMillis()

        mainActivity.activateNavBottom(false)

        Log.d("ProfilePhoto", "ProfilePhotoItem Create ${savedUser.profilePhoto}")

        FireStorageHandler().uploadMemePhotoWith(postID, imageViewMeme.drawable, createContext, {
            mainActivity.activateNavBottom(true)
            val memeImageURL = it
            if (memeImageURL != null && savedUser != null) {

                savedUser.memes += it.toString()
                Log.d("Saving New Meme", "Saving ${it.toString()}")
                DatabaseManager(createContext).convertUserObject(savedUser, "MainUser")

                val newPost: HashMap<String, Any> = hashMapOf(
                    "userAge" to savedUser.getUserAge().toLong(),
                    "userGender" to savedUser.gender,
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
                    "postPoints" to 1,
                    "postHeight" to imageHeight.toLong()
                )

                FirestoreHandler().addDataToFirestore("Memes", postID, newPost, {
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
            val imageData = data?.data
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && imageData!= null) {
                val postButton = rootView.findViewById(R.id.buttonPost) as Button
                postButton.alpha = 1.0f
                getImageDimension(imageData)
                Glide.with(this)
                    .load(imageData)
                    .into(imageViewMeme)
                addImageButton.setImageResource(R.drawable.ic_action_delete)
            }
        }
    }

    private fun resetValues() {
        val imageViewMeme = rootView.findViewById(R.id.imageViewMeme) as ImageView
        val buttonPost = rootView.findViewById(R.id.buttonPost) as Button
        val postTitle = rootView.findViewById(R.id.editTextTitle) as EditText

        addImageButton.setImageResource(R.drawable.ic_action_create)
        buttonPost.alpha = 0.25f
        imageViewMeme.setImageDrawable(null)
        postTitle.setText(null)
    }

    private fun setupAlertDialog(message: String?) {
        val builder = AlertDialog.Builder(createContext)
        builder.setTitle("Post Error")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->}
        builder.show()
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(activity, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        startActivity(intent)
    }

    fun getImageDimension(uri: Uri) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val inputStream = createContext.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream, null, options)

        imageHeight = options.outHeight
    }
}

