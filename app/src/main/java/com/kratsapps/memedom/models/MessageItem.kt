package com.kratsapps.memedom.models

class MessageItem(val chatID: String, val chatUserID: String, val chatType: Long, val chatDateString: String, val chatContent: String, val userProfile: String, val contentType: Long) {
    companion object {
        const val TYPE_MY_MESSAGE = 0
        const val TYPE_FRIEND_MESSAGE = 1
    }
}