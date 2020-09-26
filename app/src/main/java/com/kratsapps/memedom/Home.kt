package com.kratsapps.memedom

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*


class Home : Fragment() {

    lateinit var rootView: View
    lateinit var createButton: ImageButton
    lateinit var linkSegment: AppCompatRadioButton
    lateinit var popularSegment: AppCompatRadioButton
    lateinit var freshSegment: AppCompatRadioButton

    private var allMemes: List<Memes> = listOf<Memes>()
    var firebaseAuth: FirebaseAuth? = null
    var mAuthListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getAllMemes()
    }

    private fun getAllMemes() {
        FirestoreHandler().checkForNewMemes {
            Log.d("Memes", "Got all new memes ${it.size}")
            allMemes = it
            setupFeedView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val homeContext = container?.context
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        setupUI()
        checkLoginStatus()
        return rootView
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
        createButton = rootView.findViewById(R.id.buttonCreate) as ImageButton
        createButton.setOnClickListener{
            val intent: Intent = Intent(this.context, Create::class.java)
            startActivity(intent)
        }

        linkSegment = rootView.findViewById(R.id.linkSegment)
        popularSegment = rootView.findViewById(R.id.popularSegment)
        freshSegment = rootView.findViewById(R.id.freshSegment)

        linkSegment.setOnClickListener{
            Log.d("Segment", "Link segment tapped")
            updateSegments(0)
        }
        popularSegment.setOnClickListener{
            Log.d("Segment", "Link segment tapped")
            updateSegments(1)
        }
        freshSegment.setOnClickListener{
            Log.d("Segment", "Link segment tapped")
            updateSegments(2)
        }
    }

    private fun setupFeedView() {
        val feedRecyclerView = rootView.findViewById(R.id.recyclerViewHome) as RecyclerView
        feedRecyclerView.adapter = FeedAdapter(allMemes)
        feedRecyclerView.layoutManager = LinearLayoutManager(activity)
        feedRecyclerView.setHasFixedSize(true)
    }

    private fun setupCrentialView() {
        val signupButton = rootView.findViewById(R.id.home_signup_button) as Button
        val loginButton: Button = rootView.findViewById(R.id.home_login_button) as Button

        signupButton.setOnClickListener{
            navigateToCredentialView(true)
        }

        loginButton.setOnClickListener{
            navigateToCredentialView(false)
        }
    }

    private fun navigateToCredentialView(isSignup: Boolean) {
        val intent: Intent = Intent(this.context, Credential::class.java)
        intent.putExtra("CREDENTIAL_ACTION", isSignup)
        startActivity(intent)
    }

    private fun updateSegments(type: Int) {
        if(type == 0) {
            linkSegment.isChecked = true
            popularSegment.isChecked = false
            freshSegment.isChecked = false
        } else if (type == 1) {
            popularSegment.isChecked = true
            freshSegment.isChecked = false
            linkSegment.isChecked = false
        } else {
            freshSegment.isChecked = true
            popularSegment.isChecked = false
            linkSegment.isChecked = false
        }
    }
}
