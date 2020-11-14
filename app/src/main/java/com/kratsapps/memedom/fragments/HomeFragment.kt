package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.CredentialActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.adapters.FeedAdapter
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.utils.AndroidUtils

class HomeFragment : Fragment() {

    lateinit var rootView: View
    lateinit var matchedSegment: AppCompatRadioButton
    lateinit var memedomSegment: AppCompatRadioButton
    lateinit var homeContext: Context
    lateinit var feedAdapter: FeedAdapter
    lateinit var feedRecyclerView: RecyclerView
    lateinit var homeSwipe: SwipeRefreshLayout

    private var filteredMemems = mutableListOf<Memes>()
    private var allMemes = mutableListOf<Memes>()
    private var matchedMemes = mutableListOf<Memes>()
    var firebaseAuth: FirebaseAuth? = null
    var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private fun getAllMemes() {
        var progressOverlay: View = rootView.findViewById(R.id.progress_overlay)
        AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)
        val mainUser = DatabaseManager(this.context!!).retrieveSavedUser()
        FirestoreHandler().getAppSettings() { points, dayLimit ->
            FirestoreHandler().checkForFreshMemes(homeContext, mainUser, dayLimit) {

                filteredMemems.clear()
                allMemes.clear()
                matchedMemes.clear()

                it.forEach {
                    if(mainUser != null) {
                        if(mainUser.matches.contains(it.postUserUID)) {
                            matchedMemes.add(it)
                        }
                    }
                    filteredMemems.add(it)
                    allMemes.add(it)
                }

                homeSwipe.isRefreshing = false
                progressOverlay.visibility = View.GONE
                setupFeedView()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        homeContext = context
        Log.d("OnCreateView", "Called Attached")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("OnCreateView", "Called")
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        setupUI()
        checkLoginStatus()
        setupFirestore()
        return rootView
    }

    private fun setupFirestore() {
        Log.d("HomeContext", "${homeContext != null} memes ${allMemes.count()}")

        if(homeContext != null) {
            if(allMemes.isEmpty()) {
                getAllMemes()
            } else {
                setupFeedView()
            }
            DatabaseManager(homeContext).retrieveSavedUser()
        }
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

    private fun setupUI() {

        memedomSegment = rootView.findViewById(R.id.memedomSegment)
        matchedSegment = rootView.findViewById(R.id.matchSegment)

        Log.d("HomeContext", "Views initialized $memedomSegment")

        matchedSegment.setOnClickListener{
            Log.d("Segment", "Link segment tapped ${matchedMemes.count()}")
            updateSegments("Matched")

        }
        memedomSegment.setOnClickListener{
            Log.d("Segment", "Popular segment tapped ${allMemes.count()}")
            updateSegments("Memedom")
        }


        homeSwipe = rootView.findViewById<SwipeRefreshLayout>(R.id.homeSwipe)
        homeSwipe.setOnRefreshListener(OnRefreshListener {
            getAllMemes()
        })
        homeSwipe.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_orange_light)

    }

    private fun setupFeedView() {
        Log.d("Memes", "recyclerview setup")
        val context = this.context
        val activity = this.activity
        if (context != null && activity != null) {
            feedAdapter = FeedAdapter(filteredMemems, activity, false)

            feedRecyclerView = rootView.findViewById(R.id.recyclerViewHome) as RecyclerView
            feedRecyclerView.addItemDecoration(
                DefaultItemDecorator(
                    resources.getDimensionPixelSize(
                        R.dimen.vertical_recyclerView
                    )
                )
            )
            feedRecyclerView.adapter = feedAdapter
            feedRecyclerView.layoutManager = LinearLayoutManager(activity)
            feedRecyclerView.setHasFixedSize(true)
            feedRecyclerView.itemAnimator?.removeDuration
        }
    }

    private fun setupCrentialView() {
        val signupButton = rootView.findViewById(R.id.signupHomeButton) as Button
        val loginButton: Button = rootView.findViewById(R.id.loginHomeButton) as Button

        signupButton.setOnClickListener{
            navigateToCredentialView(true)
        }

        loginButton.setOnClickListener{
            navigateToCredentialView(false)
        }
    }

    private fun navigateToCredentialView(userSignup: Boolean) {
        activity?.let{
            val intent = Intent(it, CredentialActivity::class.java)
            intent.putExtra("CREDENTIAL_ACTION", userSignup)
            it.startActivity(intent)
        }
    }

    private fun updateSegments(type: String) {

        feedAdapter.clear()

        if(type.equals("Matched")) {
            filteredMemems.addAll(matchedMemes)
            matchedSegment.isChecked = true
            memedomSegment.isChecked = false
        } else {
            filteredMemems.addAll(allMemes)
            memedomSegment.isChecked = true
            matchedSegment.isChecked = false
        }

        Log.d("Filtering", "Filtered Memes ${filteredMemems.count()}")
        feedAdapter.addItems(filteredMemems)

    }
}
