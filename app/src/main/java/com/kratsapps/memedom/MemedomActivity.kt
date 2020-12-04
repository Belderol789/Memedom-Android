package com.kratsapps.memedom

import DefaultItemDecorator
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kratsapps.memedom.adapters.ChatAdapter
import com.kratsapps.memedom.adapters.ImageAdapter
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_memedom.*

class MemedomActivity : AppCompatActivity() {

    var savedUserImages = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memedom)
        val savedUser = DatabaseManager(this).retrieveSavedUser()
        savedUserImages = savedUser!!.memes.toMutableList()

        Log.d("Saved User Images", "$savedUserImages")

        closeBtn.setOnClickListener {
            onBackPressed()
        }

        val galleryManager: GridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        val memeAdapter = ImageAdapter(savedUserImages, null,this, null, true)

        memedomRecycler.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
        memedomRecycler.adapter = memeAdapter
        memedomRecycler.layoutManager = galleryManager
        memedomRecycler.itemAnimator?.removeDuration
    }

    fun didSelectCurrentImage(position: Int) {
        val intent = Intent().apply {
            putExtra("SelectedImage", savedUserImages[position])
        }
        setResult(Activity.RESULT_OK, intent)
        onBackPressed()
    }
}