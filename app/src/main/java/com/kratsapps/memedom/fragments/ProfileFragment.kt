package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.adapters.FeedAdapter
import com.kratsapps.memedom.adapters.ImageAdapter
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.utils.*


class ProfileFragment : Fragment() {

    lateinit var profileContext: Context
    lateinit var profileView: CardView
    lateinit var profileRecyclerView: RecyclerView
    lateinit var progressCardView: CardView

    lateinit var rootView: View
    lateinit var galleryAdapter: ImageAdapter
    lateinit var galleryRecyclerView: RecyclerView
    var galleryItems: MutableList<String> = mutableListOf()
    var profileIsExpanded: Boolean = false
    var profilePhotoSelected: Boolean = true

    var imageIsChanged: Boolean = false

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
        rootView = inflater.inflate(R.layout.fragment_profile, container, false)
        profileRecyclerView = rootView.findViewById(R.id.profileRecycler)
        galleryRecyclerView = rootView.findViewById(R.id.galleryRecycler) as RecyclerView
        getAllUserMemes()
        setupUI()
        setupGallery()
        return rootView
    }

    private fun getAllUserMemes() {
        val mainUser = DatabaseManager(profileContext).retrieveSavedUser()
        val mainUserID = mainUser?.uid

        val crownCount = rootView.findViewById<TextView>(R.id.crownsCount)
        val postCount = rootView.findViewById<TextView>(R.id.postCount)
        val matchCount = rootView.findViewById<TextView>(R.id.matchCount)

        val mainActivity = this.activity as? MainActivity

        if (mainUserID != null && mainActivity != null) {
            FirestoreHandler().getAllMemesOfMainUser(mainUserID) {

                Log.d("Money", "Losing Money on Profile")

                matchCount.setText("${mainUser.matches.count()}")
                postCount.setText("${it.count()}")
                var crowns = 0
                for (meme in it) {
                    crowns += meme.getPostLikeCount()
                }
                crownCount.setText("$crowns")

                if (this.activity != null) {
                    val feedAdapter = FeedAdapter(it, this.activity!!, true)
                    profileRecyclerView.addItemDecoration(
                        DefaultItemDecorator(
                            resources.getDimensionPixelSize(
                                R.dimen.vertical_recyclerView
                            )
                        )
                    )
                    profileRecyclerView.adapter = feedAdapter
                    profileRecyclerView.layoutManager = LinearLayoutManager(activity)
                    profileRecyclerView.setHasFixedSize(true)
                    profileRecyclerView.itemAnimator?.removeDuration
                }
            }
        }
    }

    private fun setupUI() {

        profileView = rootView.findViewById<CardView>(R.id.profile_cardView)

        val username = rootView.findViewById<TextView>(R.id.username)
        val gender = rootView.findViewById<TextView>(R.id.gender)
        val profilePhoto = rootView.findViewById<ImageButton>(R.id.profilePhoto)
        val bioText = rootView.findViewById<EditText>(R.id.bioText)
        val addGalleryBtn = rootView.findViewById<Button>(R.id.addGalleryBtn)
        val saveBtn = rootView.findViewById<Button>(R.id.saveBtn)
        progressCardView = rootView.findViewById<CardView>(R.id.progressCardView)
        progressCardView.visibility = View.INVISIBLE

        val mainUser = DatabaseManager(profileContext).retrieveSavedUser()
        val mainUserID = mainUser?.uid

        saveBtn.visibility = View.INVISIBLE

        if (mainUser != null) {
            username.setText(mainUser.name)
            gender.setText(mainUser.gender)
            if (!mainUser.bio.equals("")) {
                bioText.setText(mainUser.bio)
            } else {
                bioText.setHint("Write About Yourself")
            }

            Log.d("ProfileURL", "ProfilePhotoItem ${mainUser.profilePhoto}")

            Glide.with(this.activity!!)
                .load(mainUser.profilePhoto)
                .error(ContextCompat.getDrawable(this.context!!, R.drawable.ic_action_name))
                .circleCrop()
                .into(profilePhoto)
        }

        var height: Int = 0

        activity?.displayMetrics()?.run {
            height = heightPixels
            val width = widthPixels

            val params = profileView.layoutParams as ConstraintLayout.LayoutParams
            params.height = height
            params.width = width
            params.topMargin = height
            profileView.requestLayout()
        }

        val expandBtn = rootView.findViewById(R.id.expandBtn) as ImageButton
        expandBtn.setOnClickListener {
            if (!profileIsExpanded) {

                Log.d("Expand Profile Card", "Expanded")

                profileView
                    .animate()
                    .setDuration(500)
                    .translationY(((height / 1.8) - height).toFloat())
                    .withEndAction {
                        profileIsExpanded = !profileIsExpanded
                    }

                expandBtn
                    .animate()
                    .setDuration(500)
                    .rotationBy(180f)
                    .start()
            } else {
                saveBtn.visibility = View.INVISIBLE
                profileView
                    .animate()
                    .setDuration(500)
                    .translationY(-0F)
                    .withEndAction {
                        profileIsExpanded = !profileIsExpanded
                    }
                expandBtn
                    .animate()
                    .setDuration(500)
                    .rotationBy(180f)
                    .start()

            }
        }

        profilePhoto.setOnClickListener {
            profilePhotoSelected = true
            saveBtn.visibility = View.VISIBLE
            prepOpenImageGallery()
        }

        addGalleryBtn.setOnClickListener {
            profilePhotoSelected = false
            saveBtn.visibility = View.VISIBLE
            prepOpenImageGallery()
        }

        bioText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                Log.d("Expanded", "Profile view should increase")

                profileView
                    .animate()
                    .setDuration(500)
                    .translationY(((height / 1.8) - height).toFloat())
                    .withEndAction {
                        profileIsExpanded = !profileIsExpanded
                    }

                expandBtn
                    .animate()
                    .setDuration(500)
                    .rotationBy(180f)
                    .start()
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > 0) {
                    saveBtn.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        if (mainUserID != null) {
            saveBtn.setOnClickListener {
                //ProfilePhoto
                val profileImage = profilePhoto.drawable
                val userBio = bioText.text.toString()
                val progressOverlay = rootView.findViewById<View>(R.id.progress_overlay)
                progressCardView.visibility = View.VISIBLE
                AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

                if (imageIsChanged) {
                    Log.d("Saving", "Saving $mainUserID progress $progressOverlay")

                    FireStorageHandler().uploadPhotoWith(mainUserID, profileImage, {
                        val updatedProfile: HashMap<String, Any> = hashMapOf(
                            "bio" to userBio,
                            "profilePhoto" to it,
                            "gallery" to galleryItems
                        )

                        Log.d("Saving", "Updating user data")

                        FirestoreHandler().updateDatabaseObject("User", mainUserID, updatedProfile)

                        mainUser.profilePhoto = it
                        mainUser.bio = userBio
                        mainUser.gallery = galleryItems

                        DatabaseManager(this.context!!).convertUserObject(mainUser, "MainUser")
                        saveBtn.visibility = View.INVISIBLE
                        progressOverlay.visibility = View.GONE
                        progressCardView.visibility = View.INVISIBLE
                    })
                } else {
                    val updatedProfile: HashMap<String, Any> = hashMapOf(
                        "bio" to userBio
                    )
                    FirestoreHandler().updateDatabaseObject("User", mainUserID, updatedProfile)
                    mainUser.bio = userBio
                    DatabaseManager(this.context!!).convertUserObject(mainUser, "MainUser")
                    saveBtn.visibility = View.INVISIBLE
                    progressOverlay.visibility = View.GONE
                    progressCardView.visibility = View.INVISIBLE
                }
            }
        }
    }

    public fun showSave() {
        val saveBtn = rootView.findViewById<Button>(R.id.saveBtn)
        saveBtn.visibility = View.VISIBLE
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

        val images = DatabaseManager(profileContext).retrieveSavedUser()?.gallery
        val galleryManager: GridLayoutManager =
            GridLayoutManager(activity, 3, GridLayoutManager.VERTICAL, false)

        if (context != null && activity != null && images != null) {
            galleryItems = images.toMutableList()
            galleryAdapter = ImageAdapter(galleryItems, activity, this)
            galleryRecyclerView.addItemDecoration(
                DefaultItemDecorator(
                    resources.getDimensionPixelSize(
                        R.dimen.vertical_recyclerView
                    )
                )
            )
            galleryRecyclerView.adapter = galleryAdapter
            galleryRecyclerView.layoutManager = galleryManager
            galleryRecyclerView.itemAnimator?.removeDuration
        }
    }

    fun Activity.displayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val profilePhoto = rootView.findViewById<ImageView>(R.id.profilePhoto)
        val userID = DatabaseManager(this.context!!).getMainUserID()

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {

                imageIsChanged = true

                val imageData = data.data
                if (profilePhotoSelected) {
                    Glide.with(this)
                        .load(imageData)
                        .circleCrop()
                        .into(profilePhoto)
                } else if (userID != null && imageData != null) {
                    progressCardView.visibility = View.VISIBLE
                    FireStorageHandler().uploadGallery(userID, imageData, this.context!!, {
                        progressCardView.visibility = View.INVISIBLE
                        galleryItems.add(it)
                        galleryRecyclerView.adapter?.notifyDataSetChanged()
                        Log.d("Gallery Items", "$galleryItems")
                    })
                }
            }
        }
    }
}