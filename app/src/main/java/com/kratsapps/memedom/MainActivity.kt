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
import com.bumptech.glide.Glide
import com.facebook.FacebookSdk
import com.facebook.internal.Mutable
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.kratsapps.memedom.fragments.*
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*


class MainActivity : AppCompatActivity() {

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = SettingsFragment()
    val msgFragment = MessagesFragment()
    val createFragment = CreateFragment()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    var currentMatchUser: MemeDomUser? = null
    var mainUser: MemeDomUser? = null
    var profileIsLoaded: Boolean = false

    //HomeFragment
    var friendMemes = mutableListOf<Memes>()
    var datingMemes = mutableListOf<Memes>()
    var filteredMemems = mutableListOf<Memes>()
    var allMemes = mutableListOf<Memes>()
    var matchedMemes = mutableListOf<Memes>()
    //ProfileFragment
    var profileMemes = mutableListOf<Memes>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Main Activity", "Main Activity is being created")

        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext);}

        firebaseAnalytics = Firebase.analytics
        mainUser = DatabaseManager(this).retrieveSavedUser()

        Log.d("Main Activity", "Main Activity is being created again $mainUser")

        MobileAds.initialize(this)
        checkLoginStatus()
        activateFacebook()
        setupBottomNavigation()
        checkMatchingStatus()
    }

    fun activateNavBottom(active: Boolean) {
        navigationBottom.isEnabled = active
    }

    fun setupHomeFragment(completed: () -> Unit) {
        FirestoreHandler().getAppSettings() {points, dayLimit, memeLimit, matchLimit ->
            DatabaseManager(this).saveToPrefsInt("matchLimit", matchLimit.toInt())
            FirestoreHandler().getAllFriendMemes(this, mainUser, dayLimit, memeLimit) {
                filteredMemems.clear()
                allMemes.clear()
                matchedMemes.clear()

                it.forEach {
                    if (mainUser?.matches != null) {
                        if(mainUser!!.matches.contains(it.postUserUID)) {
                            matchedMemes.add(it)
                        }
                    }
                    filteredMemems.add(it)
                    allMemes.add(it)
                }
                completed()
            }
        }
    }

    fun setupProfileFragment(completed: (MutableList<Memes>) -> Unit) {
        if (mainUser?.uid != null) {
            FirestoreHandler().getAllMemesOfMainUser(mainUser!!.uid) {
                profileMemes.add(it)
                Log.d("UserMemes", "Memes ${profileMemes.count()}")
                completed(profileMemes)
            }
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
        makeCurrentFragment(homeFragment)
    }

    fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.wrapperFL, fragment)
            commit()
        }

    private fun activateFacebook() {
        FacebookSdk.fullyInitialize()
    }

    private fun checkMatchingStatus() {
        val user = FirebaseAuth.getInstance().currentUser

        val numberOfTimes = DatabaseManager(this).retrievePrefsInt("matchLimit", 5)

        if (user != null) {
            navigationBottom.visibility = View.VISIBLE
            FirestoreHandler().checkMatchingStatus(this.applicationContext, user.uid, {
                currentMatchUser = it
                matchView.visibility = View.VISIBLE
                matchView.infoTextView.text = "You've liked ${it.name} \nmemes $numberOfTimes times!"
                Glide.with(this)
                    .load(it.profilePhoto)
                    .circleCrop()
                    .into(matchView.profilePhoto)

                val randomGif =
                    listOf(R.raw.gif1, R.raw.gif2, R.raw.gif3, R.raw.gif4, R.raw.gif5).random()

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

    private fun proceedToProfile() {
        val intent: Intent = Intent(this@MainActivity, ProfileActivity::class.java)
        intent.putExtra("MatchID", currentMatchUser!!.uid)
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

        val firebaseAuth = FirebaseAuth.getInstance()
        val mAuthListener = FirebaseAuth.AuthStateListener {
            fun onAuthStateChanged(@NonNull firebaseAuth: FirebaseAuth) {
                val user = FirebaseAuth.getInstance().currentUser
                setUIForUser(user)
            }
        }
    }

    private fun setUIForUser(user: FirebaseUser?) {

        Log.d("UserCredential", "Current user $user")

        if (user != null) {
            navigationBottom.visibility = View.VISIBLE
            makeCurrentFragment(homeFragment)
        } else {
            navigationBottom.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        Log.d("ChatUser", "Request code $requestCode")

        if (requestCode == 999 && data != null) {

            Log.d("ChatUser", "Request data $data")

            val chatUserData = data.getSerializableExtra("ChatMatch") as Matches

            Log.d("ChatUser", "Request dataSerial $chatUserData")

            goToChat(chatUserData)
        }
    }

    private fun goToChat(currentMatch: Matches) {
        val intent: Intent = Intent(this, ChatActivity::class.java)
        val chatUser = MemeDomUser()
        chatUser.name = currentMatch.name
        chatUser.profilePhoto = currentMatch.profilePhoto
        chatUser.uid = currentMatch.uid
        intent.putExtra("ChatUser", chatUser)
        this.startActivity(intent)
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
}