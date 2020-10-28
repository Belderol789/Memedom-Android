package com.kratsapps.memedom.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import com.kratsapps.memedom.utils.MatchAdapter


class MessagesFragment : Fragment() {

    lateinit var msgContext: Context
    lateinit var rootView: View
    var matches: MutableList<Matches> = mutableListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        msgContext = context
        Log.d("OnCreateView", "Called Attached")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_messages, container, false)
        getAllMatches()
        return rootView
    }

    private fun getAllMatches() {
        if(msgContext != null) {
            FirestoreHandler().checkNewMatches(msgContext, {
                matches = it
                Log.d("Firestore-matching", "Got matches ${matches.count()}")
                setupMatchRecycler()
            })
        }
    }

    private fun setupMatchRecycler() {
        val context = this.context
        val activity = this.activity

        if(context != null && activity != null) {
            val mainUser = DatabaseManager(context).retrieveSavedUser()
            val matchAdapter = MatchAdapter(matches, activity, mainUser!!)

            val matchRecycler = rootView.findViewById<RecyclerView>(R.id.matchesRecycler)
            matchRecycler.adapter = matchAdapter
            matchRecycler.layoutManager = LinearLayoutManager(activity)
            matchRecycler.setHasFixedSize(true)
            matchRecycler.itemAnimator?.removeDuration
        }
    }
}