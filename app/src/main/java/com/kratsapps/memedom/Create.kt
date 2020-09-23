package com.kratsapps.memedom

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.android.synthetic.main.activity_signup.*

class Create : AppCompatActivity() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        Log.i("Navigation", "Navigated to Create")
        setupUI()

    }

    private fun setupUI() {
        imageViewMeme.setImageDrawable(null)

        addImageButton.setOnClickListener {

            val hasMeme: Boolean = imageViewMeme.drawable != null

            Log.d("Create", "ImageView has meme $hasMeme")
            if (hasMeme) {
                imageViewMeme.setImageDrawable(null)
                addImageButton.setImageResource(R.drawable.ic_action_create)
            } else {
                prepOpenImageGallery()
            }
        }

        buttonPost.setOnClickListener {
            val title = editTextTitle.text.toString()
            if(imageViewMeme.drawable != null) {
                Log.d("Create", "Has Image from gallery")
            } else {
                setupAlertDialog("Meme is missing!")
            }
        }

    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {
                val imageData = data.data
                Glide.with(this)
                    .load(imageData)
                    .centerCrop()
                    .into(imageViewMeme)
                addImageButton.setImageResource(R.drawable.ic_action_delete)
            }
        }
    }

    private fun setupAlertDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Post Error")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            Toast.makeText(applicationContext, R.string.alert_ok, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

}

