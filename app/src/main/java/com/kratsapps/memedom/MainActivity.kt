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
import com.kratsapps.memedom.adapters.TutorialAdapter
import com.kratsapps.memedom.fragments.*
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.*
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val START_CHAT_REQUEST_CODE = 69
    }

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = NotifFragment()
    val msgFragment = MessagesFragment()
    val createFragment = CreateFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    var currentMatchUser: MemeDomUser? = null
    var mainUser: MemeDomUser? = null

    //HomeFragment
    var datingMemes = mutableListOf<Memes>()
    var allMemes = mutableListOf<Memes>()
    //MessagesFragment
    var userMatches = mutableListOf<Matches>()
    //NotificationFragment
    var notifications = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Main Activity", "Main Activity is being created")

        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext);}
        firebaseAnalytics = Firebase.analytics
        mainUser = DatabaseManager(this).retrieveSavedUser()
        Log.d("Main Activity", "Main Activity is being created again $mainUser")
        FirestoreHandler().setupFirestore()
        setupBottomNavigation()
        setupTutorialView()
        checkLoginStatus()
        getAllUserNotifications()
        activateOnline(true)
        MobileAds.initialize(this)
        activateFacebook()
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
                        Log.d("MainActivityHome", "SeenOldMemes ${mainUser!!.seenOldMemes} this meme ${it.postID}")
                        val minAge = mainUser!!.minAge
                        val maxAge = mainUser!!.maxAge
                        if (mainUser?.lookingFor == it.userGender && it.postType == "Dating" && it.userAge >= minAge && it.userAge <= maxAge) {
                            datingMemes.add(it)
                        }
                    }
                    allMemes.add(it)
                }
                completed()
            }
        }
    }
    //ProfileFragment
    fun setupProfileFragment(completed: (Memes) -> Unit) {
        if (mainUser?.uid != null) {
            FirestoreHandler().getAllMemesOfMainUser(mainUser!!.uid) {
                if (it != null) {
                    completed(it)
                    if (!mainUser!!.memes.contains(it.postImageURL)) {
                        mainUser!!.memes += it.postImageURL
                        Log.d("Saving New Meme", "Saving ${it.toString()}")
                        DatabaseManager(this).convertUserObject(mainUser!!, {})
                    }

                }
            }
        }
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
                            DatabaseManager(this).convertUserObject(mainUser!!, {})
                            showPendingView("You have NEW matches!")
                        }
                    }
                }
            })
        })
    }

    //NotificationFragment
    fun getAllUserNotifications() {
        val mainUser = DatabaseManager(this).retrieveSavedUser()
        val mainUserID = mainUser?.uid
        notifications.clear()
        if (mainUserID != null) {
            FirestoreHandler().getAllUserNotifications(mainUserID!!, {
                val notificationIDs = notifications.map { it.notifContentID }
                if (!notificationIDs.contains(it.notifContentID)) {
                    notifications.add(it)
                    Toast.makeText(baseContext, "You have a new Notification!", Toast.LENGTH_SHORT).show()
                } else {
                    notifications = (notifications.filter { s -> s.notifContentID != it.notifContentID }).toMutableList()
                }
                notifications.sortByDescending { it.notifDateLong }
            })
        }
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
        Log.d("OnlineStatus", "User uid ${mainUser?.uid}")
        if (mainUser?.uid != null) {
            if (!mainUser!!.uid.isEmpty()) {
                FirestoreHandler().addDataToFirestore("Online", mainUser!!.uid, hashMapOf(
                    "online" to status,
                    "onlineDate" to System.currentTimeMillis()
                ), {})
            }
        }
    }

    private fun setupBottomNavigation() {
        navigationBottom.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_home -> makeCurrentFragment(homeFragment)
                R.id.ic_profile -> makeCurrentFragment(profileFragment)
                R.id.ic_create -> makeCurrentFragment(createFragment)
                R.id.ic_notifs -> makeCurrentFragment(notifFragment)
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

        val user = FirebaseAuth.getInstance().currentUser

        val firstTutorial = TutorialModel()
        firstTutorial.tutorialImage = R.mipmap.tutorial_1
        firstTutorial.titleText = "Post/Like Memes"
        firstTutorial.subtitleText = "Post your own memes and start liking others"

        val secondTutorial = TutorialModel()
        secondTutorial.tutorialImage = R.mipmap.tutorial_2
        secondTutorial.titleText = "Find Connections"
        secondTutorial.subtitleText = "Like enough memes and make connections!"

        val thirdTutorial = TutorialModel()
        thirdTutorial.tutorialImage = R.mipmap.tutorial_3
        thirdTutorial.titleText = "Match!"
        thirdTutorial.subtitleText = "Find someone who matches your humor"

        val fourthTutorial = TutorialModel()
        fourthTutorial.tutorialImage = R.mipmap.tutorial_4
        fourthTutorial.titleText = "Chat and Share Memes"
        fourthTutorial.subtitleText = "Have fun chatting and sharing memes"

        val tutorialModel = mutableListOf<TutorialModel>(
            firstTutorial,
            secondTutorial,
            thirdTutorial,
            fourthTutorial
        )
        val tutorialAdapter = TutorialAdapter(tutorialModel)
        val tutorialManager: GridLayoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        tutorialRecycler.adapter = tutorialAdapter
        tutorialRecycler.layoutManager = tutorialManager

        skipBtn.setOnClickListener {
            tutorialView.visibility = View.GONE
            if (user != null) {
                navigationBottom.visibility = View.VISIBLE
                DatabaseManager(this).saveToPrefsBoolean("TutorialKey", true)
                showToastAlert("Like memes or maybe post your own?")
            }
        }

        var i = 1
        nextBtn.setOnClickListener {
            tutorialRecycler.smoothScrollToPosition(i)
            if (i < tutorialModel.count()) {
                i += 1
            } else if (i == tutorialModel.count()) {
                tutorialView.visibility = View.GONE
                if (user != null) {
                    navigationBottom.visibility = View.VISIBLE
                    DatabaseManager(this).saveToPrefsBoolean("TutorialKey", true)
                    showToastAlert("Like memes or maybe post your own?")
                }
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
                if (firebaseAuth.currentUser != null) {
                    setUIForUser(firebaseAuth.currentUser)
                } else {
                    DatabaseManager(this).saveToPrefsBoolean("TutorialKey", false)
                }
            }
        }
    }

    private fun setUIForUser(user: FirebaseUser?) {
        Log.d("UserCredential", "Current user $user")
        val tutorialStatus = DatabaseManager(this).retrievePrefsBoolean("TutorialKey", false)
        val user = FirebaseAuth.getInstance().currentUser
        Log.d("TutorialStatus", "Status $tutorialStatus")
        if (tutorialStatus && user != null) {
            tutorialView.visibility = View.GONE
            navigationBottom.visibility = View.VISIBLE
        } else {
            tutorialView.visibility = View.VISIBLE
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
            } else if (requestCode == 3001) {
                profileFragment.reloadProfile()
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

    fun showToastAlert(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }

}