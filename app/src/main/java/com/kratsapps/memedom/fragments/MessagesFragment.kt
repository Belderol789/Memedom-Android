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


class MessagesFragment : Fragment() {

    lateinit var msgContext: Context
    lateinit var rootView: View
    lateinit var matchRecycler: RecyclerView
    lateinit var matchAdapter: MatchAdapter
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
        setupMatchRecycler()
        setupSearch()
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
        val context = this.context
        val activity = this.activity

        if(context != null && activity != null) {
            val mainUser = DatabaseManager(context).retrieveSavedUser()
            matchAdapter = MatchAdapter(matches, activity, mainUser!!)

            matchRecycler = rootView.findViewById<RecyclerView>(R.id.matchesRecycler)
            matchRecycler.adapter = matchAdapter
            matchRecycler.layoutManager = LinearLayoutManager(activity)
            matchRecycler.setHasFixedSize(true)
            matchRecycler.itemAnimator?.removeDuration
        }
    }
}