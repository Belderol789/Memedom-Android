package com.kratsapps.memedom.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.adapters.NotifAdapter
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Notification
import com.kratsapps.memedom.utils.DatabaseManager


class NotifFragment : Fragment() {

    lateinit var rootView: View
    var mainActivity: MainActivity? = null
    private var mainUser: MemeDomUser? = null

    lateinit var notifRecycler: RecyclerView
    var notifAdapter: NotifAdapter? = null

    var notifList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mainActivity = this.activity as MainActivity
        mainUser = mainActivity?.mainUser
        rootView = inflater.inflate(R.layout.fragment_notif, container, false)
        getAllUserNotifications()
        return rootView
    }

    private fun setupUI() {
        if (mainActivity != null) {
            notifList.clear()
            notifList.addAll(mainActivity!!.notifications)
            if (notifAdapter == null) {
                notifAdapter = NotifAdapter(notifList, mainActivity!!)
                notifRecycler = rootView.findViewById(R.id.notificationRecycler)
                notifRecycler.adapter = notifAdapter
                notifRecycler.layoutManager = LinearLayoutManager(mainActivity)
                notifRecycler.setHasFixedSize(true)
                notifRecycler.itemAnimator?.removeDuration
            } else {
                notifAdapter!!.clear()
                notifAdapter!!.addItems(mainActivity!!.notifications)
            }
        }
        val notifSwipe = rootView.findViewById<SwipeRefreshLayout>(R.id.notifSwipe)
        notifSwipe.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            getAllUserNotifications()
            notifSwipe.isRefreshing = false
        })
        notifSwipe.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_orange_light)
    }

    fun getAllUserNotifications() {
        val notifEmpty = rootView.findViewById<LinearLayout>(R.id.notifEmpty)
        if (mainActivity!!.notifications.isEmpty()) {
            //Show Empty State
            notifEmpty.visibility = View.VISIBLE
        } else {
            notifEmpty.visibility = View.GONE
            setupUI()
        }
    }
}