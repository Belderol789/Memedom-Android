package com.kratsapps.memedom.models

class MemeItem(val meme: Memes?, val feedType: Int) {
    companion object {
        const val TYPE_FEED_ITEM = 69
        const val TYPE_AD_ITEM = 420
    }
}