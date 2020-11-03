package com.kratsapps.memedom.models

class MessageItem(val content: String, val textColor: Int, val senderType: Int, messageType: Int) {
    companion object {
        const val TYPE_MY_MESSAGE = 0
        const val TYPE_FRIEND_MESSAGE = 1
    }
}