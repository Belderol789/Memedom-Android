package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.internal.Mutable
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.adapters.FeedAdapter
import com.kratsapps.memedom.adapters.ImageAdapter
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.*


class ProfileFragment : Fragment() {

    lateinit var profileContext: Context
    lateinit var profileView: CardView
    lateinit var profileRecyclerView: RecyclerView
    lateinit var progressCardView: CardView
    lateinit var username: TextView
    lateinit var gender: TextView
    lateinit var profilePhoto: ImageButton
    lateinit var bioText: EditText
    lateinit var photosScrollView: HorizontalScrollView

    lateinit var rootView: View
    lateinit var galleryAdapter: ImageAdapter
    lateinit var galleryRecyclerView: RecyclerView

    lateinit var mainActivity: MainActivity

    private var mainUser: MemeDomUser? = null
    private var mainUserID: String? = null

    var galleryItems: MutableList<String> = mutableListOf()
    var profilePhotoSelected: Boolean = true
    var profileEdited: Boolean = false

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        profileContext = context
        Log.d("OnCreateView", "Called Attached")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mainActivity = this.activity as MainActivity
        mainUser = mainActivity.mainUser
        mainUserID = mainUser?.uid
        rootView = inflater.inflate(R.layout.fragment_profile, container, false)
        setupUI()
        setupActionUI()
        getAllUserMemes()
        setupGallery()
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bioText.text.toString() != mainUser?.bio) {
            mainUser?.bio = bioText.text.toString()
            DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser", {})
            mainActivity.saveProfileEdits(
                hashMapOf(
                    "bio" to bioText.text.toString()
                )
            )
        }
        Log.d("Destroying", "ProfileFragment getting destroyed")
    }

    private fun getAllUserMemes() {
        val crownCount = rootView.findViewById<TextView>(R.id.crownsCount)
        val postCount = rootView.findViewById<TextView>(R.id.postCount)
        val matchCount = rootView.findViewById<TextView>(R.id.matchCount)
        Log.d("UserMemes", "We getting the memes of $mainUser inside $mainActivity")

        if (mainActivity != null && mainUser != null && mainActivity.profileMemes.isEmpty()) {
            mainActivity.setupProfileFragment() { profileMemes ->
                Log.d("UserMemes", "User Profile Memes ${profileMemes.count()}")

                var crown: Int = 0
                for (meme in profileMemes) {
                    crown += meme.getPostLikeCount()
                }

                matchCount.setText("${mainUser!!.matches.count()}")
                postCount.setText("${profileMemes.count()}")
                crownCount.setText("$crown")

                var userMemes = mutableListOf<String>()
                for (meme in profileMemes) {
                    userMemes.add(meme.postImageURL)
                }

                val feedAdapter = ImageAdapter(userMemes, profileMemes, this.activity!!, this, true)
                val galleryManager: GridLayoutManager =
                    GridLayoutManager(mainActivity, 3, GridLayoutManager.VERTICAL, false)
                profileRecyclerView.adapter = feedAdapter
                profileRecyclerView.layoutManager = galleryManager
                profileRecyclerView.itemAnimator?.removeDuration
            }
        }
    }

    private fun setupUI() {

        username = rootView.findViewById<TextView>(R.id.username)
        gender = rootView.findViewById<TextView>(R.id.gender)
        profilePhoto = rootView.findViewById<ImageButton>(R.id.profilePhoto)
        bioText = rootView.findViewById<EditText>(R.id.bioText)
        photosScrollView = rootView.findViewById<HorizontalScrollView>(R.id.photosScrollView)

        profileRecyclerView = rootView.findViewById(R.id.profileRecycler)
        galleryRecyclerView = rootView.findViewById(R.id.galleryRecycler)

        profileView = rootView.findViewById<CardView>(R.id.profile_cardView)
        progressCardView = rootView.findViewById<CardView>(R.id.progressCardView)
        progressCardView.visibility = View.INVISIBLE
        val loadingImageView = rootView.findViewById(R.id.loadingImageView) as ImageView
        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)

        AndroidUtils().getScreenWidthAndHeight(mainActivity, { width, height ->
            profileRecyclerView.layoutParams.width = width
            profileRecyclerView.layoutParams.height = height

            galleryRecyclerView.layoutParams.width = width
            galleryRecyclerView.layoutParams.height = height
        })

        if (mainUser != null) {
            username.setText(mainUser!!.name)
            gender.setText(mainUser!!.gender)
            if (!mainUser!!.bio.equals("")) {
                bioText.setText(mainUser!!.bio)
            } else {
                bioText.setHint("Write About Yourself")
            }

            Log.d("ProfileURL", "ProfilePhotoItem ${mainUser!!.profilePhoto}")

            Glide.with(mainActivity)
                .load(mainUser!!.profilePhoto)
                .error(ContextCompat.getDrawable(this.context!!, R.drawable.ic_action_name))
                .circleCrop()
                .into(profilePhoto)
        }

        profilePhoto.setOnClickListener {
            profilePhotoSelected = true
            openImageGallery()
        }
    }

    fun openImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    private fun setupActionUI() {
        profilePhoto.setOnClickListener {
            profilePhotoSelected = true
            prepOpenImageGallery()
        }

        val memeSegment = rootView.findViewById<AppCompatRadioButton>(R.id.memeSegment)
        val gallerySegment = rootView.findViewById<AppCompatRadioButton>(R.id.gallerySegment)
        val screenWidth = ScreenSize().getScreenWidth()

        memeSegment.setOnClickListener {
            memeSegment.isChecked = true
            gallerySegment.isChecked = false

            memeSegment.setTextColor(Color.WHITE)
            gallerySegment.setTextColor(Color.parseColor("#58BADC"))
            photosScrollView.smoothScrollTo(0, 0)
        }

        gallerySegment.setOnClickListener {
            gallerySegment.isChecked = true
            memeSegment.isChecked = false

            gallerySegment.setTextColor(Color.WHITE)
            memeSegment.setTextColor(Color.parseColor("#58BADC"))
            photosScrollView.smoothScrollTo(screenWidth, 0)
        }
    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    private fun setupGallery() {
        val context = this.context
        val activity = this.activity

        var savedImages = DatabaseManager(profileContext).retrieveSavedUser()?.gallery
        var images = listOf<String>("https://firebasestorage.googleapis.com/v0/b/memedom-fb37b.appspot.com/o/AppSettings%2Fplus.png?alt=media&token=c4e6af5c-d0ea-4570-ab19-655997bfac29")
        if (savedImages != null) {
            images += savedImages
        }
        val galleryManager: GridLayoutManager = GridLayoutManager(activity, 3, GridLayoutManager.VERTICAL, false)

        if (context != null && activity != null && images != null) {

            Log.d("UserGallery", "User photos $images")
            galleryItems = images.toMutableList()
            galleryAdapter = ImageAdapter(galleryItems, null, activity, this, false)
            galleryRecyclerView.adapter = galleryAdapter
            galleryRecyclerView.layoutManager = galleryManager
            galleryRecyclerView.itemAnimator?.removeDuration
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val profilePhoto = rootView.findViewById<ImageView>(R.id.profilePhoto)
        val userID = DatabaseManager(this.context!!).getMainUserID()

        if (resultCode == Activity.RESULT_OK && mainUser != null) {
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {

                profileEdited = true

                val imageData = data.data
                progressCardView.visibility = View.VISIBLE
                if (profilePhotoSelected && imageData != null) {
                    FireStorageHandler().uploadPhotoData(userID!!, imageData, profileContext, {
                        progressCardView.visibility = View.INVISIBLE
                        mainUser?.profilePhoto = it
                        Glide.with(this)
                            .load(it)
                            .circleCrop()
                            .into(profilePhoto)
                        mainActivity.saveProfileEdits(
                            hashMapOf(
                                "profilePhoto" to it
                            )
                        )
                        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser", {})
                    })
                } else if (userID != null && imageData != null) {
                    FireStorageHandler().uploadGallery(userID, imageData, this.context!!, {
                        progressCardView.visibility = View.INVISIBLE
                        galleryItems.add(it)
                        galleryRecyclerView.adapter?.notifyDataSetChanged()

                        mainUser!!.gallery += it
                        mainActivity.saveProfileEdits(
                            hashMapOf(
                                "gallery" to mainUser!!.gallery
                            )
                        )
                        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser", {})
                    })
                }
            }
        }
    }
}