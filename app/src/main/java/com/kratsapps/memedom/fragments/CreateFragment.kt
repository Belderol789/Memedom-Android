package com.kratsapps.memedom.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
    lateinit var removeImageBtn: ImageButton
    lateinit var createContext: Context
    lateinit var createLoadingView: CardView

    lateinit var mainActivity: MainActivity

    var postIsMeme: Boolean = true
    var postNSFW: Boolean = false
    var postType: String = "Friends"
    var imageHeight: Int = 0

    var imageData: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = this.activity as MainActivity
        rootView = inflater.inflate(R.layout.fragment_create, container, false)
        setupUI()
        return rootView
    }

    override fun onAttach(context: Context) {
        Log.d("OnCreateView", "Called Attached")
        super.onAttach(context)
        createContext = context
    }

    override fun onDestroy() {
        Log.d("CreateActivity", "Resetting")
        resetValues()
        super.onDestroy()
    }

    private fun setupUI() {
        createLoadingView = rootView.findViewById(R.id.createLoadingView) as CardView
        val loadingImageView = rootView.findViewById(R.id.loadingImageView) as ImageView
        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)

        imageViewMeme = rootView.findViewById(R.id.imageViewMeme) as ImageView
        buttonPost = rootView.findViewById(R.id.buttonPost) as Button
        addImageButton = rootView.findViewById(R.id.addImageButton) as ImageButton
        removeImageBtn = rootView.findViewById(R.id.removeImageBtn) as ImageButton

        addImageButton.visibility = View.VISIBLE
        removeImageBtn.visibility = View.INVISIBLE

        resetValues()
        restartCreateView()

        imageViewMeme.setImageDrawable(null)

        removeImageBtn.setOnClickListener {
            addImageButton.visibility = View.VISIBLE
            removeImageBtn.visibility = View.INVISIBLE
            imageViewMeme.setImageDrawable(null)
        }

        addImageButton.setOnClickListener {
            prepOpenImageGallery()
        }

        buttonPost.setOnClickListener {
            val savedUser = DatabaseManager(createContext).retrieveSavedUser()
            if (imageViewMeme.drawable != null && savedUser != null) {
                Log.d("Create", "Has Image from gallery")
                createLoadingView.visibility = View.VISIBLE
                it.visibility = View.INVISIBLE
                if (postIsMeme) {
                    sendPostToFirestore(savedUser)
                } else {
                    addPhotoToGallery(savedUser)
                }
            } else {
                setupAlertDialog("Post Error","Meme is missing!")
            }
        }

        val memesBtn = rootView.findViewById<Button>(R.id.memesBtn)
        val mesBtn = rootView.findViewById<Button>(R.id.mesBtn)

        memesBtn.setOnClickListener {
            postIsMeme = true
            optionsView.visibility = View.GONE
        }

        mesBtn.setOnClickListener {
            setupMes()
        }

        setupTypeSelection()
        setupNSFWSelection()
    }

    private fun addPhotoToGallery(savedUser: MemeDomUser) {
        if (imageData != null) {
            FireStorageHandler().uploadGallery(savedUser, imageData!!, createContext, {
                createLoadingView.visibility = View.INVISIBLE
                setupAlertDialog("Success!","Photo has been added to your profile")
                resetValues()
                restartCreateView()
            })
        }
    }

    private fun setupMes() {
        postIsMeme = false
        optionsView.visibility = View.GONE
        editTextTitle.visibility = View.INVISIBLE
        datingLayout.visibility = View.INVISIBLE
        nsfwLayout.visibility = View.INVISIBLE
        buttonPost.text = "Post to Profile Gallery"
        setButtonColor(R.color.specialColor)
    }

    private fun restartCreateView() {
        val optionsView = rootView.findViewById<CardView>(R.id.optionsView)
        val datingLayout = rootView.findViewById<LinearLayout>(R.id.datingLayout)
        val nsfwLayout = rootView.findViewById<LinearLayout>(R.id.nsfwLayout)
        val editTextTitle = rootView.findViewById<EditText>(R.id.editTextTitle)

        optionsView.visibility = View.VISIBLE
        datingLayout.visibility = View.VISIBLE
        editTextTitle.visibility = View.VISIBLE
        nsfwLayout.visibility = View.VISIBLE
        buttonPost.text = "Post Meme!"
        setButtonColor(R.color.appFGColor)
    }

    fun setupNSFWSelection() {
        val nsfw = rootView.findViewById<SwitchCompat>(R.id.nsfwSwitch)
        nsfw.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                postNSFW = true
            } else {
                postNSFW = false
            }
        }
    }

    fun setupTypeSelection() {
        val dating = rootView.findViewById<SwitchCompat>(R.id.datingSwitch)
        dating.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                postType = "Dating"
                setButtonColor(R.color.appDateFGColor)
            } else {
                postType = "Friends"
                setButtonColor(R.color.appFGColor)
            }
        }
    }

    fun setButtonColor(color: Int) {
        buttonPost.setBackgroundColor(ContextCompat.getColor(createContext, color))

        if (imageViewMeme.drawable == null) {
            buttonPost.alpha = 0.25f
        }
    }

    private fun sendPostToFirestore(savedUser: MemeDomUser) {

        val mainActivity = activity as MainActivity

        val title = editTextTitle.text.toString()
        val postID = generateRandomString()
        val today = System.currentTimeMillis()

        mainActivity.activateNavBottom(false)

        Log.d("ProfilePhoto", "ProfilePhotoItem Create ${savedUser.profilePhoto}")

        FireStorageHandler().uploadMemePhotoWith(
            postID,
            imageViewMeme.drawable,
            createContext,
            {

                mainActivity.activateNavBottom(true)
                val memeImageURL = it
                if (memeImageURL != null && savedUser != null) {

                    savedUser.memes += it
                    DatabaseManager(createContext).convertUserObject(savedUser, {})

                    Log.d("Saving New Meme", "Saving ${it.toString()}")

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
                        "postUserEmail" to savedUser.email,
                        "postPoints" to 1,
                        "postHeight" to imageHeight.toLong(),
                        "postType" to postType,
                        "postNSFW" to postNSFW
                    )

                    FirestoreHandler().addDataToFirestore("Memes", postID, newPost, {
                        buttonPost.visibility = View.VISIBLE
                        createLoadingView.visibility = View.INVISIBLE
                        if (it != null) {
                            setupAlertDialog("Post Error", it)
                        } else {

                            resetValues()
                            mainActivity.allMemes.clear()
                            mainActivity.datingMemes.clear()

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
                            meme.postType = postType
                            navigateToComments(meme)
                        }
                    })
                } else {
                    // show alert
                    setupAlertDialog("Post Error","Ooops, we failed to share your amazing meme :(")
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
            imageData = data?.data
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && imageData != null) {
                val postButton = rootView.findViewById(R.id.buttonPost) as Button
                postButton.alpha = 1.0f
                getImageDimension(imageData!!)
                Glide.with(this)
                    .load(imageData)
                    .into(imageViewMeme)
                addImageButton.visibility = View.INVISIBLE
                removeImageBtn.visibility = View.VISIBLE
            }
        }
    }

    private fun resetValues() {
        val imageViewMeme = rootView.findViewById(R.id.imageViewMeme) as ImageView
        val buttonPost = rootView.findViewById(R.id.buttonPost) as Button
        val postTitle = rootView.findViewById(R.id.editTextTitle) as EditText
        val switchDating = rootView.findViewById(R.id.datingSwitch) as SwitchCompat
        val switchNSFW = rootView.findViewById(R.id.nsfwSwitch) as SwitchCompat

        addImageButton.visibility = View.VISIBLE
        removeImageBtn.visibility = View.INVISIBLE

        buttonPost.alpha = 0.25f
        switchDating.isChecked = false
        switchNSFW.isChecked = false
        imageViewMeme.setImageDrawable(null)
        postTitle.setText(null)
    }

    private fun setupAlertDialog(title: String, message: String?) {
        createLoadingView.visibility = View.INVISIBLE
        val builder = AlertDialog.Builder(createContext)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which -> }
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

