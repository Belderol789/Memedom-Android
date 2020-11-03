package com.kratsapps.memedom.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.MessageItem
import com.kratsapps.memedom.models.MessageItem.Companion.TYPE_FRIEND_MESSAGE
import com.kratsapps.memedom.models.MessageItem.Companion.TYPE_MY_MESSAGE
import com.kratsapps.memedom.models.MessageViewHolder

class ChatAdapter(var data: MutableList<MessageItem>) : RecyclerView.Adapter<MessageViewHolder<*>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder<*> {
        val context = parent.context
        return when (viewType) {
            TYPE_MY_MESSAGE -> {
                val view = LayoutInflater.from(context).inflate(R.layout.my_message_item, parent, false)
                MyMessageViewHolder(view)
            }
            TYPE_FRIEND_MESSAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_message_item, parent, false)
                FriendMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder<*>, position: Int) {
        val item = data[position]
        Log.d("adapter View", position.toString() + item.content)
        when (holder) {
            is MyMessageViewHolder -> holder.bind(item)
            is FriendMessageViewHolder -> holder.bind(item)
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].senderType

    class MyMessageViewHolder(val view: View) : MessageViewHolder<MessageItem>(view) {
        private val messageContent = view.findViewById<TextView>(R.id.message)

        override fun bind(item: MessageItem) {
            messageContent.text = item.content
            messageContent.setTextColor(item.textColor)
        }
    }
    class FriendMessageViewHolder(val view: View) : MessageViewHolder<MessageItem>(view) {
        private val messageContent = view.findViewById<TextView>(R.id.message)

        override fun bind(item: MessageItem) {
            messageContent.text = item.content
            messageContent.setTextColor(item.textColor)
        }
    }
}