package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.*
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler

class HomeFragment : Fragment() {

    lateinit var rootView: View
    lateinit var createButton: ImageButton
    lateinit var linkSegment: AppCompatRadioButton
    lateinit var freeMemeSegment: AppCompatRadioButton
    lateinit var homeContext: Context
    lateinit var feedAdapter: FeedAdapter
    lateinit var feedRecyclerView: RecyclerView

    private var allMemes: List<Memes> = listOf<Memes>()
    var firebaseAuth: FirebaseAuth? = null
    var mAuthListener: FirebaseAuth.AuthStateListener? = null


    private fun getAllMemes() {
        FirestoreHandler().getAppSettings() { points, dayLimit ->
            FirestoreHandler().checkForFreshMemes(dayLimit) {
                Log.d("Memes", "Got all new memes ${it.count()}")
                allMemes = it
                if(homeContext != null) {
                    setupFeedView()
                }
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
            (createButton.parent as? ViewGroup)?.removeView(createButton)
            setupCrentialView()
        }

        firebaseAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener() {
            fun onAuthStateChanged(@NonNull firebaseAuth:FirebaseAuth) {
                val user = FirebaseAuth.getInstance().getCurrentUser()
                if (user != null) {
                    val credentialView = rootView.findViewById(R.id.credentialViewHome) as LinearLayout
                    (credentialView.parent as? ViewGroup)?.removeView(credentialView)
                }
            }
        }
    }

    private fun setupUI() {

        linkSegment = rootView.findViewById(R.id.linkSegment)
        freeMemeSegment = rootView.findViewById(R.id.freeMemeSegment)

        Log.d("HomeContext", "Views initialized $freeMemeSegment")

        linkSegment.setOnClickListener{
            Log.d("Segment", "Link segment tapped")
            updateSegments(0)
        }
        freeMemeSegment.setOnClickListener{
            Log.d("Segment", "Popular segment tapped")
            updateSegments(1)
        }
    }

    private fun setupFeedView() {
        Log.d("Memes", "recyclerview setup")
        val context = this.context
        val activity = this.activity
        if (context != null && activity != null) {
            feedAdapter = FeedAdapter(allMemes, activity)

            feedRecyclerView = rootView.findViewById(R.id.recyclerViewHome) as RecyclerView
            feedRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
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

    private fun navigateToCredentialView(isSignup: Boolean) {
        val intent: Intent = Intent(this.context, CredentialActivity::class.java)
        intent.putExtra("CREDENTIAL_ACTION", isSignup)
        startActivity(intent)
    }

    private fun updateSegments(type: Int) {
        if(type == 0) {
            linkSegment.isChecked = true
            freeMemeSegment.isChecked = false
        } else if (type == 1) {
            freeMemeSegment.isChecked = true
            linkSegment.isChecked = false
        }
    }
}
