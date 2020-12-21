package com.kratsapps.memedom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.kratsapps.memedom.models.MemeDomUser
import kotlinx.android.synthetic.main.activity_image.*

class ImageActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val enlargeImageURL = intent.extras?.get("EnlargeImageURL") as String

        Glide
            .with(this)
            .load(enlargeImageURL)
            .fitCenter()
            .into(largeImageView)

        closeImgBtn.setOnClickListener {
            onBackPressed()
        }
    }
}