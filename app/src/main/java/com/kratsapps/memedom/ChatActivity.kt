package com.kratsapps.memedom

import DefaultItemDecorator
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.irozon.alertview.AlertActionStyle
import com.irozon.alertview.AlertStyle
import com.irozon.alertview.AlertView
import com.irozon.alertview.objects.AlertAction
import com.kratsapps.memedom.adapters.ChatAdapter
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.Chat
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.MessageItem
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_chat.usernameText

class ChatActivity : AppCompatActivity() {
    companion object {
        const val START_MEMEDOM_REQUEST_CODE = 0
    }

    var chatAdapter: ChatAdapter? = null
    lateinit var currentChat: MemeDomUser
    var chatUniqueID: String = ""
    var allChats = mutableListOf<Chat>()
    var allMessageItems = mutableListOf<MessageItem>()
    var allChatsID = mutableListOf<String>()
    var mainUser: MemeDomUser? = null

    var lastChatDate: Long = System.currentTimeMillis()

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    private val PERMISSION_CODE = 1000;
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        Log.d("Created Chat", "Chat is created again")

        currentChat = intent.extras?.get("ChatUser") as MemeDomUser
        mainUser = DatabaseManager(this).retrieveSavedUser()

        setupUI()

        val userIDs = mainUser!!.uid + currentChat!!.uid

        chatUniqueID = userIDs.toCharArray().sorted().joinToString("")

        Log.d("ChatUser", "Chat UniqueID $chatUniqueID")

        FirestoreHandler().retrieveChats(chatUniqueID, {

            Log.d("CurrentChat", "Chat ID is ${it.chatID} chatIDs are $allChatsID")

            if(!allChatsID.contains(it.chatID) && !allChats.contains(it)) {
                allChatsID.add(it.chatID)
                allChats.add(it)
                val sortedChats = allChats.sortedBy { it -> it.chatDate }
                val mainUser = DatabaseManager(this).getMainUserID()

                allMessageItems.clear()

                for (chat in sortedChats) {
                    var chatType: Long = if (mainUser.equals(chat.chatUserID)) 0 else 1

                    Log.d("ChatUser", "Current Chat Type $chatType")

                    val messageItem = MessageItem(chat.chatID, chat.chatUserID, chatType, chat.commentDateString(), chat.chatContent, currentChat.profilePhoto, chat.chatImageURL)
                    lastChatDate = chat.chatDate
                    allMessageItems.add(messageItem)
                }

                Log.d("CurrentChat", "Current Chat Count ${allChats.count()} Chats $allChats")

                setupRecyclerView(allMessageItems)
            }
        })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun setupUI() {

        backBtn.setOnClickListener {
            updateLastMessage()
        }

        usernameText.setText(currentChat.name)
        Glide
            .with(this)
            .load(currentChat.profilePhoto)
            .circleCrop()
            .into(userPhotoImage)

        chatSendBtn.setOnClickListener {
            val chatText = edittext_chatbox.text.toString()
            if (!chatText.isEmpty()) {
                sendChat(chatText, 0)
            }
        }

        cameraBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    //permission was not enabled
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_CODE)
                } else{
                    //permission already granted
                    openCamera()
                }
            } else{
                //system os is < marshmallow
                openCamera()
            }
        }

        memedomBtn.setOnClickListener {
            openMemedom()
        }

        galleryBtn.setOnClickListener {
            prepOpenImageGallery()
        }

        optionBtn.setOnClickListener {
            val alert = AlertView("Select an Option", "", AlertStyle.IOS)
            alert.addAction(AlertAction("Unmatch", AlertActionStyle.DEFAULT, {
                FirestoreHandler().unmatchUser(chatUniqueID)
                onBackPressed()
            }))
            alert.addAction(AlertAction("Report", AlertActionStyle.NEGATIVE, {
                FirestoreHandler().unmatchUser(chatUniqueID)
                FirestoreHandler().rejectUser(currentChat, this)
                onBackPressed()
            }))

            alert.show(this)
        }

        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(chatLoadingImageView)

    }

    private fun openMemedom() {
        val intent: Intent = Intent(this, MemedomActivity::class.java)
        startActivityForResult(intent, START_MEMEDOM_REQUEST_CODE)
    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted
                    openCamera()
                }
                else{
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK) {
            //set image captured to image view
            val chatID = generateRandomString()
            val userID = DatabaseManager(this).getMainUserID()

            Log.d("MemedomImage", "Got image back with requestCode $requestCode")

            if (userID != null) {
                if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {
                    progressChatView.visibility = View.VISIBLE
                    val imageData = data.data
                    FireStorageHandler().uploadChatMeme(chatID, chatUniqueID, imageData!!, 0L, userID, this, {
                        progressChatView.visibility = View.GONE
                    })
                } else if (requestCode == START_MEMEDOM_REQUEST_CODE && data != null) {
                    Log.d("MemedomImage", "Got Image $data")
                    val chatImageURL = data?.getStringExtra("SelectedImage")
                    if (chatImageURL != null) {
                        FirestoreHandler().sendUserChats(chatID, chatUniqueID, chatImageURL, "", 0L, userID)
                    }
                } else {
                    progressChatView.visibility = View.VISIBLE
                    val imageData = image_uri
                    FireStorageHandler().uploadChatMeme(chatID, chatUniqueID, imageData!!, 0L, userID, this, {
                        progressChatView.visibility = View.GONE
                    })
                }
            }
        }
    }

    fun updateLastMessage() {
        Log.d("ChatActivity", "Allmessages ${allMessageItems.count()}")
        if (!allMessageItems.isEmpty()) {
            val lastItem = allMessageItems.last()

            Log.d("ChatActivity", "Valid URL ${lastItem.chatContent}")

            var lastText = "Sent a message!"
            if (URLUtil.isValidUrl(lastItem.chatImageURL)) {
                lastText = "Sent  an image!"
            } else if (!lastItem.chatContent.isEmpty()) {
                lastText = lastItem.chatContent
            }
            val intent = Intent().apply {
                putExtra("matchText", lastText)
                putExtra("currentChatUID", currentChat.uid)
                putExtra("chatDate", lastChatDate)
            }
            setResult(Activity.RESULT_OK, intent)
            onBackPressed()
        } else {
            onBackPressed()
        }
    }

    private fun setupRecyclerView(messageItems: MutableList<MessageItem>) {
        val activity = this
        Log.d("ChatUser", "Chatadapter $chatAdapter")

        chatAdapter = ChatAdapter(messageItems)
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.reverseLayout
        linearLayoutManager.setStackFromEnd(true);

        chatRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = linearLayoutManager
        chatRecyclerView.setHasFixedSize(true)
        chatRecyclerView.itemAnimator?.removeDuration
    }

    private fun sendChat(content: String, type: Long) {
        val chatID = generateRandomString()
        FirestoreHandler().sendUserChats(chatID, chatUniqueID, "", content, type, mainUser!!.uid)
        edittext_chatbox.setText(null)
        hideKeyboard()
    }

    private fun generateRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { charset.random() }
            .joinToString("")
    }
}