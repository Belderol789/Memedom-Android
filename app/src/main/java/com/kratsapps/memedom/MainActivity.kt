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
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.facebook.FacebookSdk
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.fragments.*
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import java.io.Serializable


class MainActivity : AppCompatActivity() {

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = SettingsFragment()
    val msgFragment = MessagesFragment()
    val createFragment = CreateFragment()

    var currentMatchUser: MemeDomUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileAds.initialize(this)
        checkLoginStatus()
        activateFacebook()
        setupBottomNavigation()
        checkMatchingStatus()
    }

    fun activateNavBottom(active: Boolean) {
        navigationBottom.isEnabled = active
    }

    private fun setupBottomNavigation() {
        navigationBottom.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.ic_home -> makeCurrentFragment(homeFragment)
                R.id.ic_profile -> makeCurrentFragment(profileFragment)
                R.id.ic_create -> makeCurrentFragment(createFragment)
                R.id.ic_notifs -> makeCurrentFragment(notifFragment)
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
        val user = FirebaseAuth.getInstance().getCurrentUser()

        if(user != null) {
            navigationBottom.visibility = View.VISIBLE
            FirestoreHandler().checkMatchingStatus(this.applicationContext, user.uid, {
                currentMatchUser = it
                matchView.visibility = View.VISIBLE
                matchView.infoTextView.text = "You've liked ${it.name} \nmemes 10 times!"
                Glide.with(this)
                    .load(it.profilePhoto)
                    .circleCrop()
                    .into(matchView.profilePhoto)

                val randomGif = listOf(R.raw.gif1, R.raw.gif2, R.raw.gif3, R.raw.gif4, R.raw.gif5).random()

                Glide.with(this)
                    .asGif()
                    .load(randomGif)
                    .into(memeImageView)

                fadeOutAndHideImage(matchView.memeImageView)
                Log.d("Firestore-matching", "Got User ${it.uid}")
            })
        }  else {
            navigationBottom.visibility = View.GONE
        }

        matchView.profileBtn.setOnClickListener {
            if(currentMatchUser != null) {
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
                FirestoreHandler().sendMatchToUser(currentMatchUser!!.uid, this.applicationContext)

                val intent: Intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("ChatUser", currentMatchUser)

                this.startActivity(intent)
                this.overridePendingTransition(
                    R.anim.enter_activity,
                    R.anim.enter_activity
                )
                Log.d("ChatUser", "Sending Chat User $currentMatchUser")
                restartMatchView()
            }
        }
    }

    private fun proceedToProfile() {
        val intent: Intent = Intent(this@MainActivity, ProfileActivity::class.java)
        intent.putExtra("MatchedUser", currentMatchUser!!.uid)
        startActivity(intent)
    }

    private fun fadeOutAndHideImage(img: ImageView) {
        val timer = object: CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                val fadeOut = AlphaAnimation(1F, 0F)
                fadeOut.setInterpolator(AccelerateInterpolator())
                fadeOut.setDuration(2000)

                fadeOut.setAnimationListener(object: Animation.AnimationListener {
                    override fun onAnimationEnd(animation:Animation) {
                        img.setVisibility(View.GONE)
                    }
                    override fun onAnimationRepeat(animation:Animation) {}
                    override  fun onAnimationStart(animation:Animation) {}
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
        val user = FirebaseAuth.getInstance().getCurrentUser()
        setUIForUser(user)

        val firebaseAuth = FirebaseAuth.getInstance()
        val mAuthListener = FirebaseAuth.AuthStateListener() {
            fun onAuthStateChanged(@NonNull firebaseAuth: FirebaseAuth) {
                val user = FirebaseAuth.getInstance().getCurrentUser()
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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

}