package com.kratsapps.memedom

import android.content.Intent
import android.media.Image
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_home.*


class Home : Fragment() {

    lateinit var rootView: View
    lateinit var createButton: ImageButton

    var userLoggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        setupFeedView()
        return rootView
    }

    private fun checkLoginStatus() {
        if (userLoggedIn) {
            val credentialView = rootView.findViewById(R.id.home_credential_view) as LinearLayout
            (credentialView.parent as? ViewGroup)?.removeView(credentialView)
        } else {
            (createButton.parent as? ViewGroup)?.removeView(createButton)
            setupCrentialView()
        }
    }

    private fun setupUI() {
        createButton = rootView.findViewById(R.id.create_button) as ImageButton
        createButton.setOnClickListener{
            val intent: Intent = Intent(this.context, Create::class.java)
            startActivity(intent)
        }
    }

    private fun setupFeedView() {
        //For Testing
        val dummyList = ArrayList<FeedItem>()
        val item = FeedItem(R.drawable.ic_action_home, "This is a sample Title")
        dummyList += item
        //
        val feedRecyclerView = rootView.findViewById(R.id.home_recycler_view) as RecyclerView
        feedRecyclerView.adapter = FeedAdapter(dummyList)
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

}