package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.*
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.adapters.FeedAdapter

class HomeFragment : Fragment() {

    lateinit var rootView: View
    lateinit var datingSegment: AppCompatRadioButton
    lateinit var friendsSegment: AppCompatRadioButton
    var feedAdapter: FeedAdapter? = null
    lateinit var feedRecyclerView: RecyclerView
    lateinit var homeSwipe: SwipeRefreshLayout
    //lateinit var loadingView: CardView
    lateinit var mainActivity: MainActivity

    private var allMemes = mutableListOf<Memes>()
    private var datingMemes = mutableListOf<Memes>()
    private var filteredMemems = mutableListOf<Memes>()

    var isMemedom = true

    var firebaseAuth: FirebaseAuth? = null
    var mAuthListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = this.activity as MainActivity
        Log.d("OnCreateView", "Called with Main Activity $mainActivity")
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        setupUI()
        checkLoginStatus()
        getAllMemes()
        return rootView
    }

    private fun setupUI() {

        friendsSegment = rootView.findViewById(R.id.friendsSegment)
        datingSegment = rootView.findViewById(R.id.datingSegment)

        val loadingImageView = rootView.findViewById<ImageView>(R.id.loadingImageView)

        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)

        Log.d("HomeContext", "Views initialized $friendsSegment")

        datingSegment.setOnClickListener{
            Log.d("Segment-Matched", "Link segment tapped ${datingMemes.count()}")
            updateSegments(true)
        }
        friendsSegment.setOnClickListener{
            Log.d("Segment-Memedom", "Popular segment tapped ${allMemes.count()}")
            updateSegments(false)
        }
        homeSwipe = rootView.findViewById<SwipeRefreshLayout>(R.id.homeSwipe)
        homeSwipe.setOnRefreshListener(OnRefreshListener {
            mainActivity.allMemes.clear()
            mainActivity.datingMemes.clear()
            feedAdapter?.clear()
            getAllMemes()
        })
        homeSwipe.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_orange_light)
    }

    private fun checkLoginStatus() {

        val user = FirebaseAuth.getInstance().getCurrentUser()
        if (user != null) {
            val credentialView = rootView.findViewById(R.id.credentialViewHome) as LinearLayout
            (credentialView.parent as? ViewGroup)?.removeView(credentialView)
        } else {
            setupCrentialView()
        }

        firebaseAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener() {
            fun onAuthStateChanged(@NonNull firebaseAuth: FirebaseAuth) {
                val user = FirebaseAuth.getInstance().getCurrentUser()
                if (user != null) {
                    val credentialView = rootView.findViewById(R.id.credentialViewHome) as LinearLayout
                    (credentialView.parent as? ViewGroup)?.removeView(credentialView)
                }
            }
        }
    }

    private fun getAllMemes() {

        var blankScreen = rootView.findViewById<LinearLayout>(R.id.blankLayout)
        val loadingView = rootView.findViewById<CardView>(R.id.progressCardView)

        blankScreen.visibility = View.GONE
        filteredMemems.clear()

        if (mainActivity.allMemes.isEmpty()) {

            loadingView.visibility = View.VISIBLE
            Log.d("Main Activity", "Home-Fragment getting memes")

            mainActivity.setupHomeFragment {

                if (mainActivity.allMemes.isEmpty()) {
                    blankScreen.visibility = View.VISIBLE
                } else {
                    blankScreen.visibility = View.GONE
                }

                datingMemes = mainActivity.datingMemes
                allMemes = mainActivity.allMemes

                if (isMemedom) {
                    filteredMemems.addAll(allMemes)
                } else {
                    filteredMemems.addAll(datingMemes)
                }

                Log.d("MainActivityMemes", "filtered memes $filteredMemems")

                homeSwipe.isRefreshing = false
                loadingView.visibility = View.INVISIBLE
                setupFeedView()
            }
        } else {

            Log.d("Main Activity", "AllMemes exist")

            Log.d("MainActivityMemes", "filtered memes $filteredMemems")

            datingMemes = mainActivity.datingMemes
            allMemes = mainActivity.allMemes
            if (isMemedom) {
                filteredMemems.addAll(allMemes)
            } else {
                filteredMemems.addAll(datingMemes)
            }

            homeSwipe.isRefreshing = false
            loadingView.visibility = View.INVISIBLE
            setupFeedView()
        }
    }

    private fun setupFeedView() {
        Log.d("MainActivityMemes", "Setting up feed view ${filteredMemems.count()}")
        feedAdapter = FeedAdapter(filteredMemems, mainActivity, isMemedom)
        feedRecyclerView = rootView.findViewById(R.id.recyclerViewHome) as RecyclerView
        feedRecyclerView.adapter = feedAdapter
        feedRecyclerView.layoutManager = LinearLayoutManager(mainActivity)
        feedRecyclerView.setHasFixedSize(true)
        feedRecyclerView.itemAnimator?.removeDuration
    }

    private fun setupCrentialView() {
        val signupButton = rootView.findViewById(R.id.signupHomeButton) as Button
        val loginButton: Button = rootView.findViewById(R.id.loginHomeButton) as Button

        signupButton.setOnClickListener{
            val intent = Intent(mainActivity, SignupActivity::class.java)
            mainActivity.startActivity(intent)
        }

        loginButton.setOnClickListener{
            val intent = Intent(mainActivity, LoginActivity::class.java)
            mainActivity.startActivity(intent)
        }
    }

    private fun updateSegments(isDating: Boolean) {
        if (feedAdapter != null) {
            feedAdapter!!.clear()
        }

        Log.d("Filtereing", "Current memes ${datingMemes.count()} ${allMemes.count()}")

        if(isDating) {
            isMemedom = false
            filteredMemems.addAll(datingMemes)
            datingSegment.isChecked = true
            datingSegment.setTextColor(Color.parseColor("#FF69B4"))
            friendsSegment.isChecked = false
            friendsSegment.setTextColor(Color.WHITE)
        } else {
            isMemedom = true
            filteredMemems.addAll(allMemes)
            friendsSegment.isChecked = true
            friendsSegment.setTextColor(Color.parseColor("#58BADC"))
            datingSegment.isChecked = false
            datingSegment.setTextColor(Color.WHITE)
        }

        Log.d("Filtering", "Filtered Memes ${filteredMemems.count()}")
        if (feedAdapter != null) {
            feedAdapter!!.addItems(filteredMemems, isMemedom)
        }
    }
}
