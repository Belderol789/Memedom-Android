package com.kratsapps.memedom.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.*
import com.kratsapps.memedom.adapters.ImageAdapter
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.*
import kotlinx.android.synthetic.main.fragment_profile.*


class ProfileFragment : Fragment() {
    companion object {
        const val START_EDIT_REQUEST_CODE = 3001
    }

    lateinit var profileContext: Context
    lateinit var profileView: CardView
    lateinit var profileRecyclerView: RecyclerView
    lateinit var progressCardView: CardView
    lateinit var username: TextView
    lateinit var gender: TextView
    lateinit var profilePhoto: ImageButton
    lateinit var bioText: TextView
    lateinit var photosScrollView: HorizontalScrollView
    lateinit var editBtn: Button

    var feedAdapter: ImageAdapter? = null

    lateinit var rootView: View
    lateinit var galleryAdapter: ImageAdapter
    lateinit var galleryRecyclerView: RecyclerView

    lateinit var savedAdapter: ImageAdapter
    lateinit var savedRecyclerView: RecyclerView

    lateinit var mainActivity: MainActivity

    private var mainUser: MemeDomUser? = null
    private var mainUserID: String? = null

    var memeItemIDs: MutableList<String> = mutableListOf()
    var profileMemes: MutableList<Memes> = mutableListOf()
    var savedMemes: MutableList<Memes> = mutableListOf()

    var galleryItems: MutableList<String> = mutableListOf()

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
        getAllSavedMemes()
        return rootView
    }

    private fun getAllUserMemes() {
        val crownCount = rootView.findViewById<TextView>(R.id.crownsCount)
        val postCount = rootView.findViewById<TextView>(R.id.postCount)
        val matchCount = rootView.findViewById<TextView>(R.id.matchCount)

        if (mainActivity != null && mainUser != null) {
            mainActivity.setupProfileFragment() { profileMeme ->

                Log.d("ProfileFragment", "MemeIDs $memeItemIDs, incoming meme ${profileMeme.postID}")

                if (!memeItemIDs.contains(profileMeme.postID)) {
                    memeItemIDs.add(profileMeme.postID)
                    profileMemes.add(profileMeme)
                }

                var crown: Int = 0
                for (meme in profileMemes) {
                    crown += meme.getPostLikeCount()
                }

                matchCount.setText("${mainUser!!.matches.count()}")
                postCount.setText("${profileMemes.count()}")
                crownCount.setText("$crown")

                val profileMemeUrls = profileMemes.map { it.postImageURL }.toMutableList()
                feedAdapter = ImageAdapter(profileMemeUrls, profileMemes, mainActivity, this, true)
                val galleryManager: GridLayoutManager =
                    GridLayoutManager(mainActivity, 3, GridLayoutManager.VERTICAL, false)
                profileRecyclerView.adapter = feedAdapter
                profileRecyclerView.layoutManager = galleryManager
                profileRecyclerView.itemAnimator?.removeDuration
            }
        }
    }

    private fun getAllSavedMemes() {
        val userSavedMemes = DatabaseManager(profileContext).retrieveSavedUser()?.seenOldMemes
        savedMemes.clear()
        Log.d("SavedMemes", "User saved memes ${userSavedMemes?.count()}")
        if (userSavedMemes != null) {
            for (savedMeme in userSavedMemes) {
               val meme = DatabaseManager(profileContext).retrieveMemeObject(savedMeme)
               savedMemes.add(meme)
            }
            val savedMemeUrls = savedMemes.map { it.postImageURL }.toMutableList()
            savedAdapter = ImageAdapter(savedMemeUrls, savedMemes, mainActivity, this, true)
            val galleryManager: GridLayoutManager =
                GridLayoutManager(mainActivity, 3, GridLayoutManager.VERTICAL, false)
            savedRecyclerView.adapter = savedAdapter
            savedRecyclerView.layoutManager = galleryManager
            savedRecyclerView.itemAnimator?.removeDuration
        }
    }

    private fun setupUI() {

        username = rootView.findViewById<TextView>(R.id.username)
        gender = rootView.findViewById<TextView>(R.id.genderText)
        profilePhoto = rootView.findViewById<ImageButton>(R.id.profilePhoto)
        bioText = rootView.findViewById<TextView>(R.id.bioText)
        photosScrollView = rootView.findViewById<HorizontalScrollView>(R.id.photosScrollView)

        editBtn = rootView.findViewById(R.id.editBtn)

        profileRecyclerView = rootView.findViewById(R.id.profileRecycler)
        galleryRecyclerView = rootView.findViewById(R.id.galleryRecycler)
        savedRecyclerView = rootView.findViewById(R.id.savedRecycler)

        profileView = rootView.findViewById<CardView>(R.id.profile_cardView)
        progressCardView = rootView.findViewById<CardView>(R.id.profileLoadingView)
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

            savedRecyclerView.layoutParams.width = width
            savedRecyclerView.layoutParams.height = height
        })

        if (mainUser != null) {
            username.setText(mainUser!!.name)
            gender.setText(mainUser!!.gender)
            if (!mainUser!!.bio.equals("")) {
                bioText.setText(mainUser!!.bio)
            } else {
                bioText.setHint("No Bio.")
            }

            Log.d("ProfileURL", "ProfilePhotoItem ${mainUser!!.profilePhoto}")

            Glide.with(mainActivity)
                .load(mainUser!!.profilePhoto)
                .error(ContextCompat.getDrawable(this.context!!, R.drawable.ic_action_name))
                .circleCrop()
                .into(profilePhoto)
        }

        profilePhoto.setOnClickListener {
            navigateToEditPage()
        }

        editBtn.setOnClickListener {
            navigateToEditPage()
        }
    }

    fun reloadProfile() {
        val mainUser = DatabaseManager(profileContext).retrieveSavedUser()
        bioText.setText(mainUser!!.bio)
        gender.setText(mainUser!!.gender)
        Glide.with(mainActivity)
            .load(mainUser!!.profilePhoto)
            .error(ContextCompat.getDrawable(this.context!!, R.drawable.ic_action_name))
            .circleCrop()
            .into(profilePhoto)
    }

    private fun navigateToEditPage() {
        val intent: Intent = Intent(mainActivity, EditActivity::class.java)
        mainActivity.startActivityForResult(intent, ProfileFragment.START_EDIT_REQUEST_CODE)
        mainActivity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    private fun setupActionUI() {
        val memeSegment = rootView.findViewById<AppCompatRadioButton>(R.id.memeSegment)
        val gallerySegment = rootView.findViewById<AppCompatRadioButton>(R.id.gallerySegment)
        val savedSegment = rootView.findViewById<AppCompatRadioButton>(R.id.savedSegment)
        val screenWidth = ScreenSize().getScreenWidth()

        memeSegment.setOnClickListener {
            memeSegment.isChecked = true
            gallerySegment.isChecked = false
            savedSegment.isChecked = false

            memeSegment.setTextColor(Color.WHITE)
            gallerySegment.setTextColor(Color.parseColor("#58BADC"))
            savedSegment.setTextColor(Color.parseColor("#58BADC"))
            photosScrollView.smoothScrollTo(0, 0)
        }

        gallerySegment.setOnClickListener {
            gallerySegment.isChecked = true
            memeSegment.isChecked = false
            savedSegment.isChecked = false

            gallerySegment.setTextColor(Color.WHITE)
            memeSegment.setTextColor(Color.parseColor("#58BADC"))
            savedSegment.setTextColor(Color.parseColor("#58BADC"))
            photosScrollView.smoothScrollTo(screenWidth, 0)
        }

        savedSegment.setOnClickListener {
            savedSegment.isChecked = true
            gallerySegment.isChecked = false
            memeSegment.isChecked = false

            savedSegment.setTextColor(Color.WHITE)
            memeSegment.setTextColor(Color.parseColor("#58BADC"))
            gallerySegment.setTextColor(Color.parseColor("#58BADC"))
            photosScrollView.smoothScrollTo(screenWidth * 2, 0)
        }

    }

    private fun setupGallery() {
        val context = this.context
        val activity = this.activity
        galleryItems.clear()

        var savedImages = DatabaseManager(profileContext).retrieveSavedUser()?.gallery
        var images = mutableListOf<String>()
        if (savedImages != null) {
            images.addAll(savedImages)
        }
        val galleryManager: GridLayoutManager = GridLayoutManager(activity, 3, GridLayoutManager.VERTICAL, false)

        if (context != null && activity != null) {
            Log.d("UserGallery", "User photos $images")
            galleryItems = images
            galleryAdapter = ImageAdapter(galleryItems, null, activity, this, false)
            galleryRecyclerView.adapter = galleryAdapter
            galleryRecyclerView.layoutManager = galleryManager
            galleryRecyclerView.itemAnimator?.removeDuration
        }
    }

    fun removeGalleryItem(position: Int) {
        val removedURL = galleryItems[position]
        val mainUser = DatabaseManager(mainActivity).retrieveSavedUser()
        if (mainUser != null && mainUser.gallery.contains(removedURL)) {
            mainUser.gallery -= removedURL
            DatabaseManager(mainActivity).convertUserObject(mainUser,  {})
        }
        FirestoreHandler().deleteArrayInFirestore("User", mainUser!!.uid, removedURL)
        Log.d("URLToRemove", "Removed $removedURL")
    }
}