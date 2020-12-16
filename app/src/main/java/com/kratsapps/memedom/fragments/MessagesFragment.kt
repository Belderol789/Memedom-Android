package com.kratsapps.memedom.fragments

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.adapters.MatchAdapter
import android.app.SearchManager
import android.content.Intent
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.cardview.widget.CardView
import com.facebook.internal.Mutable
import com.kratsapps.memedom.ChatActivity
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.AndroidUtils


class MessagesFragment : Fragment() {

    lateinit var msgContext: Context
    lateinit var rootView: View
    lateinit var matchRecycler: RecyclerView
    lateinit var matchAdapter: MatchAdapter
    lateinit var blankScreen: LinearLayout

    lateinit var mainActivity: MainActivity

    lateinit var matchedSegment: AppCompatRadioButton
    lateinit var pendingSegment: AppCompatRadioButton

    var currentSegment: Boolean = true

    var filteredMatches: MutableList<Matches> = mutableListOf()
    var pending: MutableList<Matches> = mutableListOf()
    var matches: MutableList<Matches> = mutableListOf()
    var matchesID: MutableList<String> = mutableListOf()

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
        mainActivity = this.activity as MainActivity
        rootView = inflater.inflate(R.layout.fragment_messages, container, false)
        setupUI()
        setupMatchRecycler()
        getAllMatches()
        setupSearch()
        return rootView
    }

    private fun getAllMatches() {

        val mainUser = DatabaseManager(msgContext).retrieveSavedUser()
        blankScreen.visibility = View.GONE
        filteredMatches.clear()

        if (mainUser != null) {
            Log.d("UserMatches", "Load matches ${mainActivity.userMatches.count()}")
            if (filteredMatches.isEmpty() && mainActivity.userMatches.isEmpty()) {
                mainActivity.getAllMatches {
                    filteredMatches.clear()
                    if (it != null && !mainUser.rejects.contains(it.uid)) {
                        filterOutMatch(it, mainUser)
                    }
                }
            } else {
                filteredMatches.clear()
                for (it in mainActivity.userMatches) {
                    if (!mainUser.rejects.contains(it.uid)) {
                        filterOutMatch(it, mainUser)
                    }
                }
            }
        }
    }

    private fun filterOutMatch(match: Matches, mainUser: MemeDomUser) {
        Log.d("UserMatches", "Filtering out $matchesID for ${match.uid} status ${match.matchStatus}")
        if (match.matchStatus.equals(true)) {
            if (matchesID.contains(match.uid)) {
                matches = (matches.filter { s -> s.uid != match.uid }).toMutableList()
                Log.d("UserMatches", "Matches count ${matches.count()}")
                matches.add(match)
            } else {
                pending = (pending.filter { s -> s.uid != match.uid }).toMutableList()
                matches.add(match)
                matchesID.add(match.uid)
            }
        } else if (match.offered.equals(mainUser.uid)) {
            pending.add(match)
        }

        pendingSegment.text = "Pending (${pending.count()})"
        matchedSegment.text = "Matches (${matches.count()})"

        if (currentSegment) {
            filteredMatches.addAll(matches)
        } else {
            filteredMatches.addAll(pending)
        }
        Log.d("MatchFragment", "Matches count ${matches.count()}")
        setupBlankScreen(filteredMatches)
        matchAdapter.addItems(filteredMatches)
    }

    private fun setupBlankScreen(messages: MutableList<Matches>) {
        if (messages.isEmpty()) {
            blankScreen.visibility = View.VISIBLE
        } else {
            blankScreen.visibility = View.GONE
        }
    }

    private fun setupUI() {
        blankScreen = rootView.findViewById(R.id.messagesBlank)
        matchedSegment = rootView.findViewById(R.id.matchedSegment)
        pendingSegment = rootView.findViewById(R.id.pendingSegment)

        matchedSegment.setOnClickListener{
            Log.d("Segment", "Matches segment tapped ${matches.count()}")
            updateSegments("Matched")
        }
        pendingSegment.setOnClickListener{
            Log.d("Segment", "Pending segment tapped ${pending.count()}")
            updateSegments("Memedom")
        }
    }

    private fun setupSearch() {
        val matchSearchView = rootView.findViewById<SearchView>(R.id.matchSearch)
        matchSearchView.setQueryHint("Search Matches")

        val searchIcon = matchSearchView.findViewById<ImageView>(R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.WHITE)

        val cancelIcon = matchSearchView.findViewById<ImageView>(R.id.search_close_btn)
        cancelIcon.setColorFilter(Color.WHITE)

        val textView = matchSearchView.findViewById<TextView>(R.id.search_src_text)
        textView.setHint("Search Matches")
        textView.setHintTextColor(Color.WHITE)
        textView.setTextColor(Color.WHITE)

        matchSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                matchAdapter.filter.filter(newText)
                return false
            }

        })
    }

    private fun setupMatchRecycler() {

        val activity = this.activity as? MainActivity

        if(msgContext != null && activity != null) {

            Log.d("Matching-Fragment", "Setting up recyclerView with $filteredMatches")

            val mainUser = DatabaseManager(msgContext).retrieveSavedUser()
            matchAdapter = MatchAdapter(filteredMatches, activity, mainUser!!)

            matchRecycler = rootView.findViewById<RecyclerView>(R.id.matchesRecycler)
            matchRecycler.adapter = matchAdapter
            matchRecycler.layoutManager = LinearLayoutManager(activity)
            matchRecycler.setHasFixedSize(true)
            matchRecycler.itemAnimator?.removeDuration
        }
    }

    private fun updateSegments(type: String) {

        matchAdapter.clear()

        if(type.equals("Matched")) {
            currentSegment = true
            filteredMatches.addAll(matches)
            matchedSegment.isChecked = true
            pendingSegment.isChecked = false
        } else {
            currentSegment = false
            filteredMatches.addAll(pending)
            pendingSegment.isChecked = true
            matchedSegment.isChecked = false
        }

        Log.d("Filtering", "Filtered Matches ${filteredMatches.count()}")
        matchAdapter.addItems(filteredMatches)
        setupBlankScreen(filteredMatches)
    }
}