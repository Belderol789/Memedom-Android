package com.kratsapps.memedom

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.fragments.*
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*


class MainActivity : AppCompatActivity() {

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = NotificationsFragment()
    val msgFragment = MessagesFragment()
    val createFragment = CreateFragment()

    var currentMatchUser: MemeDomUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activateFacebook()
        setupBottomNavigation()
        checkMatchingStatus()
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

    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.wrapperFL, fragment)
            commit()
        }

    private fun activateFacebook() {
        FacebookSdk.fullyInitialize()
    }

    private fun checkMatchingStatus() {
        val user = FirebaseAuth.getInstance().getCurrentUser()
        val mainUID = DatabaseManager(this).getMainUserID()

        if(user != null) {
            FirestoreHandler().checkMatchingStatus(this.applicationContext, user.uid, {
                currentMatchUser = it
                matchView.visibility = View.VISIBLE
                matchView.infoTextView.text = "You've liked ${it.name} \nmemes 10 times!"
                Glide.with(this)
                    .load(it.profilePhoto)
                    .circleCrop()
                    .into(matchView.profilePhoto)
                fadeOutAndHideImage(matchView.memeImageView)
                Log.d("Firestore-matching", "Got User ${it.uid}")
            })
        } else if (mainUID != null) {
            FirestoreHandler().checkMatchingStatus(this.applicationContext, mainUID, {
                currentMatchUser = it
                matchView.visibility = View.VISIBLE
                matchView.infoTextView.text = "You've liked ${it.name} \nmemes 10 times!"
                Glide.with(this)
                    .load(it.profilePhoto)
                    .circleCrop()
                    .into(matchView.profilePhoto)
                fadeOutAndHideImage(matchView.memeImageView)
                Log.d("Firestore-matching", "Got User ${it.uid}")
            })
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
                FirestoreHandler().matchUser(currentMatchUser!!, this.applicationContext)
                restartMatchView()
            }
        }
    }

    private fun proceedToProfile() {
        val intent: Intent = Intent(this@MainActivity, ProfileActivity::class.java)
        intent.putExtra("MatchedUser", currentMatchUser)
        startActivity(intent)
    }

    private fun fadeOutAndHideImage(img: ImageView) {
        val fadeOut = AlphaAnimation(1F, 0F)
        fadeOut.setInterpolator(AccelerateInterpolator())
        fadeOut.setDuration(2500)

        fadeOut.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationEnd(animation:Animation) {
                img.setVisibility(View.GONE)
            }
            override fun onAnimationRepeat(animation:Animation) {}
            override  fun onAnimationStart(animation:Animation) {}
        })
        img.startAnimation(fadeOut)
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
                if (user != null) {
                    setUIForUser(user)
                }
            }
        }
    }

    private fun setUIForUser(user: FirebaseUser?) {
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