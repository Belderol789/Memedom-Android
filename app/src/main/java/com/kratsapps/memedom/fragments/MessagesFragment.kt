package com.kratsapps.memedom.fragments

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
import androidx.appcompat.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.AppCompatRadioButton
import com.facebook.internal.Mutable


class MessagesFragment : Fragment() {

    lateinit var msgContext: Context
    lateinit var rootView: View
    lateinit var matchRecycler: RecyclerView
    lateinit var matchAdapter: MatchAdapter

    lateinit var matchedSegment: AppCompatRadioButton
    lateinit var pendingSegment: AppCompatRadioButton

    var filteredMatches: MutableList<Matches> = mutableListOf()
    var pending: MutableList<Matches> = mutableListOf()
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
        setupUI()
        getAllMatches()
        setupMatchRecycler()
        setupSearch()
        return rootView
    }

    private fun getAllMatches() {

        if(msgContext != null) {

            val mainUser = DatabaseManager(msgContext).retrieveSavedUser()

            FirestoreHandler().checkNewMatches(msgContext, {

                filteredMatches.clear()
                matches.clear()
                pending.clear()

                for(match in it) {
                    if(!mainUser!!.matches.contains(match.uid)) {
                        pending.add(match)
                    } else {
                        matches.add(match)
                    }
                }
                filteredMatches.addAll(matches)

                Log.d("Matching-Fragment", "Got matches ${filteredMatches.count()}")

                matchAdapter.notifyDataSetChanged()
            })
        }
    }

    private fun setupUI() {

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
        val activity = this.activity
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
            filteredMatches.addAll(matches)
            matchedSegment.isChecked = true
            pendingSegment.isChecked = false
        } else {
            filteredMatches.addAll(pending)
            pendingSegment.isChecked = true
            matchedSegment.isChecked = false
        }

        Log.d("Filtering", "Filtered Matches ${filteredMatches.count()}")
        matchAdapter.addItems(filteredMatches)

    }
}