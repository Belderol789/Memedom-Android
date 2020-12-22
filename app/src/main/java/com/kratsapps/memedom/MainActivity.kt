package com.kratsapps.memedom

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.facebook.FacebookSdk
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.irozon.alertview.AlertActionStyle
import com.irozon.alertview.AlertStyle
import com.irozon.alertview.AlertView
import com.irozon.alertview.objects.AlertAction
import com.kratsapps.memedom.adapters.TutorialAdapter
import com.kratsapps.memedom.fragments.*
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.models.TutorialModel
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val START_CHAT_REQUEST_CODE = 69
    }

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = SettingsFragment()
    val msgFragment = MessagesFragment()
    val createFragment = CreateFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    var currentMatchUser: MemeDomUser? = null
    var mainUser: MemeDomUser? = null

    //HomeFragment
    var datingMemes = mutableListOf<Memes>()
    var allMemes = mutableListOf<Memes>()
    //ProfileFragment
    var profileMemes = mutableListOf<Memes>()
    var profileMemeIDs = mutableListOf<String>()
    //MessagesFragment
    var userMatches = mutableListOf<Matches>()
    var userMatchesID = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Main Activity", "Main Activity is being created")

        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext);}
        firebaseAnalytics = Firebase.analytics
        mainUser = DatabaseManager(this).retrieveSavedUser()

        Log.d("Main Activity", "Main Activity is being created again $mainUser")
        activateOnline(true)
        MobileAds.initialize(this)
        checkLoginStatus()
        activateFacebook()
        setupBottomNavigation()
        getAllMatches {}
        setupProfileFragment {}
    }

    override fun onStop() {
        super.onStop()
        activateOnline(false)
    }

    fun activateNavBottom(active: Boolean) {
        navigationBottom.isEnabled = active
    }
    //HomeFragment
    fun setupHomeFragment(completed: () -> Unit) {
        FirestoreHandler().getAppSettings() {points, dayLimit, memeLimit, matchLimit ->
            DatabaseManager(this).saveToPrefsInt("matchLimit", matchLimit.toInt())
            checkMatchingStatus(matchLimit)
            FirestoreHandler().getAllMemes(mainUser, dayLimit, memeLimit) {

                datingMemes.clear()
                allMemes.clear()

                it.forEach {
                    if (mainUser != null) {
                        if (!(mainUser!!.seenOldMemes).contains(it.postID)) {
                            Log.d("MainActivityHome", "SeenOldMemes ${mainUser!!.seenOldMemes} this meme ${it.postID}")
                            if (mainUser?.lookingFor == it.userGender && it.postType == "Dating") {
                                datingMemes.add(it)
                            }
                        }
                    }
                    allMemes.add(it)
                }
                completed()
            }
        }
    }
    //ProfileFragment
    fun setupProfileFragment(completed: (MutableList<Memes>) -> Unit) {
        if (mainUser?.uid != null) {
            FirestoreHandler().getAllMemesOfMainUser(mainUser!!.uid) {
                if (it != null) {
                    if (!profileMemeIDs.contains(it.postID)) {
                        profileMemeIDs.contains(it.postID)
                        profileMemes.add(it)
                    }
                    Log.d("UserMemes", "Memes ${profileMemes.count()}")
                    if (!mainUser!!.memes.contains(it.postImageURL)) {
                        mainUser!!.memes += it.postImageURL
                        Log.d("Saving New Meme", "Saving ${it.toString()}")
                        DatabaseManager(this).convertUserObject(mainUser!!, "MainUser", {})
                    }

                }
                completed(profileMemes)
            }
        }
    }

    fun saveProfileEdits(hashMap: HashMap<String, Any>) {
        Log.d("Saving", "Updating user data")
        FirestoreHandler().updateDatabaseObject("User", mainUser!!.uid, hashMap)
    }

    //MessagesFragment
    fun getAllMatches(completed: (Matches) -> Unit) {
        FirestoreHandler().checkNewMatch(this, { matches ->
            FirestoreHandler().getOnlineStatus(matches, { match ->
                if (mainUser != null) {

                    if (!mainUser!!.rejects.contains(match.uid)) {
                        Log.d("UserMatches", "Adding current match $match")

                        userMatches.add(match)
                        completed(match)

                        if (match.matchStatus == false && match.offered.equals(mainUser?.uid)) {
                            // Display Pending view
                            showPendingView("You have PENDING matches!")
                        } else if (match.matchStatus == true && !mainUser!!.matches.contains(match.uid)) {
                            mainUser!!.matches += match.uid
                            DatabaseManager(this).convertUserObject(mainUser!!, "MainUser", {})
                            showPendingView("You have NEW matches!")
                        }
                    }
                }
            })
        })
    }

    private fun showPendingView(text: String) {
        pendingView.visibility = View.VISIBLE
        pendingTextView.text = text

        niceBtn.setOnClickListener {
            pendingView.visibility = View.GONE
        }

        val randomGif =
            listOf(R.raw.gif1, R.raw.gif2, R.raw.gif3, R.raw.gif4, R.raw.gif5, R.raw.gif6, R.raw.gif7, R.raw.gif8).random()

        Glide.with(this)
            .asGif()
            .load(randomGif)
            .into(pendingGif)
    }

    private fun activateOnline(status: Boolean) {
        if (mainUser?.uid != null) {
            FirestoreHandler().addDataToFirestore("Online", mainUser!!.uid, hashMapOf(
                "online" to status,
                "onlineDate" to System.currentTimeMillis()
            ), {})
        }
    }

    private fun setupBottomNavigation() {
        navigationBottom.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_home -> makeCurrentFragment(homeFragment)
                R.id.ic_profile -> makeCurrentFragment(profileFragment)
                R.id.ic_create -> makeCurrentFragment(createFragment)
                R.id.ic_settings -> makeCurrentFragment(notifFragment)
                R.id.ic_chat -> makeCurrentFragment(msgFragment)
            }
            true
        }
    }

    fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.wrapperFL, fragment)
            commit()
        }

    private fun activateFacebook() {
        FacebookSdk.fullyInitialize()
    }

    private fun checkMatchingStatus(matchLimit: Long) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            navigationBottom.visibility = View.VISIBLE
            FirestoreHandler().checkMatchingStatus(this, user.uid, {
                currentMatchUser = it
                matchView.visibility = View.VISIBLE
                matchView.infoTextView.text = "You've liked ${it.name} \nmemes $matchLimit times!"
                Glide.with(this)
                    .load(it.profilePhoto)
                    .circleCrop()
                    .into(matchView.profilePhoto)

                val randomGif =
                    listOf(R.raw.gif1, R.raw.gif2, R.raw.gif3, R.raw.gif4, R.raw.gif5, R.raw.gif6, R.raw.gif7, R.raw.gif8).random()

                Glide.with(this)
                    .asGif()
                    .load(randomGif)
                    .into(memeImageView)

                fadeOutAndHideImage(matchView.memeImageView)
                Log.d("Firestore-matching", "Got User ${it.uid}")
            })
        } else {
            navigationBottom.visibility = View.GONE
        }

        matchView.profileBtn.setOnClickListener {
            if (currentMatchUser != null) {
                proceedToProfile()
                restartMatchView()
            }
        }

        matchView.cancelBtn.setOnClickListener {
            if (currentMatchUser != null) {
                FirestoreHandler().rejectUser(currentMatchUser!!, this.applicationContext)
                restartMatchView()
            }
        }

        matchView.matchBtn.setOnClickListener {
            if (currentMatchUser != null) {
                FirestoreHandler().sendToMatchUser(currentMatchUser!!, this.applicationContext)
                Toast.makeText(baseContext, "Match Request Sent!", Toast.LENGTH_SHORT).show()
                restartMatchView()
            }
        }
    }

    private fun setupTutorialView() {

        tutorialView.visibility = View.VISIBLE

        val firstTutorial = TutorialModel()
        firstTutorial.tutorialImage = R.mipmap.tutorial_1
        firstTutorial.titleText = "Post/Like Memes"
        firstTutorial.subtitleText = "Post your own memes and start liking others"

        val secondTutorial = TutorialModel()
        secondTutorial.tutorialImage = R.mipmap.tutorial_2
        secondTutorial.titleText = "Find your Match!"
        secondTutorial.subtitleText = "You match with people whose memes you like"

        val thirdTutorial = TutorialModel()
        thirdTutorial.tutorialImage = R.mipmap.tutorial_3
        thirdTutorial.titleText = "Chat and Share Memes"
        thirdTutorial.subtitleText = "Have fun chatting and sharing memes"

        val tutorialModel = mutableListOf<TutorialModel>(
            firstTutorial,
            secondTutorial,
            thirdTutorial
        )
        val tutorialAdapter = TutorialAdapter(tutorialModel)
        val tutorialManager: GridLayoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        tutorialRecycler.adapter = tutorialAdapter
        tutorialRecycler.layoutManager = tutorialManager

        skipBtn.setOnClickListener {
            tutorialView.visibility = View.GONE
            DatabaseManager(this).saveToPrefsBoolean("TutorialKey", true)
        }

        var i = 1
        nextBtn.setOnClickListener {
            tutorialRecycler.smoothScrollToPosition(i)
            if (i < tutorialModel.count()) {
                i += 1
            } else if (i == tutorialModel.count()) {
                tutorialView.visibility = View.GONE
                DatabaseManager(this).saveToPrefsBoolean("TutorialKey", true)
            }
        }
    }

    private fun proceedToProfile() {
        val intent: Intent = Intent(this@MainActivity, ProfileActivity::class.java)
        intent.putExtra("MatchID", currentMatchUser!!.uid)
        intent.putExtra("isMatching", true)
        startActivity(intent)
    }

    private fun fadeOutAndHideImage(img: ImageView) {
        val timer = object : CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                val fadeOut = AlphaAnimation(1F, 0F)
                fadeOut.interpolator = AccelerateInterpolator()
                fadeOut.duration = 2000

                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(animation: Animation) {
                        img.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationStart(animation: Animation) {}
                })
                img.startAnimation(fadeOut)
            }
        }
        timer.start()
    }

    private fun restartMatchView() {
        matchView.visibility = View.INVISIBLE
        matchView.memeImageView.alpha = 1.0f
        currentMatchUser?.let { currentMatchUser = null }
    }

    private fun checkLoginStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        setUIForUser(user)

        FirebaseAuth.AuthStateListener {
            fun onAuthStateChanged(@NonNull firebaseAuth: FirebaseAuth) {
                setUIForUser(FirebaseAuth.getInstance().currentUser)
            }
        }
    }

    private fun setUIForUser(user: FirebaseUser?) {
        Log.d("UserCredential", "Current user $user")
        val  tutorialStatus = DatabaseManager(this).retrievePrefsBoolean("TutorialKey", false)
        if (user != null) {
            tutorialView.visibility = View.GONE
            navigationBottom.visibility = View.VISIBLE
        } else if (tutorialStatus == false) {
            setupTutorialView()
            navigationBottom.visibility = View.GONE
        }
        makeCurrentFragment(homeFragment)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        Log.d("ChatUser", "Request code $requestCode")
        if (data != null) {
            if (requestCode == 999) {

                Log.d("ChatUser", "Request data $data")

                val chatUserData = data.getSerializableExtra("ChatMatch") as Matches

                Log.d("ChatUser", "Request dataSerial $chatUserData")

                goToChat(chatUserData)
            } else if (requestCode == 420) {
                Log.d("Rejected", "Got Position $data")
                val rejectPosition = data.getIntExtra("Position", 0)
                msgFragment.matchAdapter.removeRow(rejectPosition)
            } else if (requestCode == START_CHAT_REQUEST_CODE) {

                val lastText = data.getStringExtra("matchText")
                val currentChatUID = data.getStringExtra("currentChatUID")
                val chatDate = data.getLongExtra("chatDate", System.currentTimeMillis())

                val chatData = hashMapOf<String, Any>(
                    "matchText" to lastText!!,
                    "chatDate" to chatDate
                )

                Log.d("UserChat", "Sending the latest chat $chatData")

                FirestoreHandler().updateMatch(currentChatUID!!, chatData, this, {})
            }
        }
    }

    private fun goToChat(currentMatch: Matches) {
        val intent: Intent = Intent(this, ChatActivity::class.java)
        val chatUser = MemeDomUser()
        chatUser.name = currentMatch.name
        chatUser.profilePhoto = currentMatch.profilePhoto
        chatUser.uid = currentMatch.uid
        intent.putExtra("ChatUser", chatUser)
        this.startActivityForResult(intent, START_CHAT_REQUEST_CODE)
        this.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    fun showStrangerAlert() {
        Toast.makeText(baseContext, "You must be logged in to like", Toast.LENGTH_SHORT).show()
    }

}