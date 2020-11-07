package com.kratsapps.memedom

import DefaultItemDecorator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.kratsapps.memedom.adapters.ChatAdapter
import com.kratsapps.memedom.adapters.CommentsAdapter
import com.kratsapps.memedom.adapters.MatchAdapter
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.Chat
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.MessageItem
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_comments.*

class ChatActivity : AppCompatActivity() {

    private var chatAdapter = ChatAdapter(mutableListOf())
    lateinit var currentChat: MemeDomUser
    var contentText: String = ""
    var contentType: Long = 0
    var allChats = mutableListOf<Chat>()
    var mainUser: MemeDomUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        currentChat = intent.extras?.get("ChatUser") as MemeDomUser
        mainUser = DatabaseManager(this).retrieveSavedUser()

        FirestoreHandler().retrieveChats(mainUser!!.uid, currentChat.uid, {
            if(!allChats.contains(it)) {
                allChats.add(it)
            }
            val sortedChats = allChats.sortedByDescending { it.chatDate }
            var messageItems = mutableListOf<MessageItem>()
            val mainUser = DatabaseManager(this).getMainUserID()

            for (chat in sortedChats) {
                var chatType: Long = if (mainUser.equals(chat.chatUserID)) 0 else 1

                Log.d("ChatUser", "Current Chat Type $chatType")

                val messageItem = MessageItem(chat.chatID, chat.chatUserID, chatType, chat.commentDateString(), chat.chatContent, currentChat.profilePhoto)
                messageItems.add(messageItem)
            }

            Log.d("CurrentChat", "Current Chat Count ${allChats.count()} Chats $allChats")

            setupRecyclerView(messageItems)
        })

        usernameText.setText(currentChat.name)

        chatSendBtn.setOnClickListener {
            val chatText = edittext_chatbox.text.toString()
            if (!chatText.isEmpty()) {
                sendChat(chatText, 0)
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun setupRecyclerView(messageItems: MutableList<MessageItem>) {
        val context = applicationContext
        val activity = this
        if (activity != null) {
            val chatAdapter = ChatAdapter(messageItems)

            chatRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
            chatRecyclerView.adapter = chatAdapter
            chatRecyclerView.layoutManager = LinearLayoutManager(activity)
            chatRecyclerView.setHasFixedSize(true)
            chatRecyclerView.itemAnimator?.removeDuration
        }
    }

    private fun sendChat(content: String, type: Long) {
        val chatID = generateRandomString()
        FirestoreHandler().sendUserChat(chatID, mainUser!!.uid, content, type, currentChat)
    }

    private fun generateRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { charset.random() }
            .joinToString("")
    }

}