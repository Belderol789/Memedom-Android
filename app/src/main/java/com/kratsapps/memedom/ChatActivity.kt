package com.kratsapps.memedom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kratsapps.memedom.adapters.ChatAdapter

class ChatActivity : AppCompatActivity() {

    private var chatAdapter = ChatAdapter(mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
    }
}