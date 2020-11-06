package com.kratsapps.memedom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kratsapps.memedom.adapters.ChatAdapter
import com.kratsapps.memedom.models.MemeDomUser

class ChatActivity : AppCompatActivity() {

    private var chatAdapter = ChatAdapter(mutableListOf())
    lateinit var currentChat: MemeDomUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        currentChat = intent.extras?.get("ChatUser") as MemeDomUser

        Log.d("ChatUser", "Current User $currentChat")

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

}