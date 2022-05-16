package com.arrowhead.arrownet

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.fxn.pix.Options
import com.fxn.pix.Pix
import com.fxn.utility.PermUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification.getClient
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from.view.*
import kotlinx.android.synthetic.main.chat_from_image.view.*
import kotlinx.android.synthetic.main.chat_to.view.*
import kotlinx.android.synthetic.main.chat_to_image.view.*
import kotlinx.android.synthetic.main.chat_toolbar.*

class ChatLogActivity : AppCompatActivity() {
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null
    private var checkTranslatedMessage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        var name = intent.getStringExtra(NewMessageActivity.NAME_KEY)
        if (name == null) {
            name = intent.getStringExtra(HomePage.NAME_KEY)
        }
        toUser = intent.getParcelableExtra(NewMessageActivity.USER_KEY)
        if (toUser == null) {
            toUser = intent.getParcelableExtra(HomePage.USER_KEY)
        }

        recyclerview_chat.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title_name.text = name
        Picasso.get().load(toUser?.photoUrl).into(profile_image)
        Picasso.get().load(HomePage.currentUser?.flagUrl).into(translate_image)

        listenForMessages()

        translate_switch.setOnCheckedChangeListener { _, p1 ->
            checkTranslatedMessage = p1
            listenForTranslatedMessages(p1)
        }

        sendButton.setOnClickListener {
            if (newMessageText.text.isNotEmpty()) {
                sendMessage()
            } else {
                return@setOnClickListener
            }
        }

        image_message_button.setOnClickListener {
            pickImage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK && requestCode == 100) {
            val returnValue = data?.getStringArrayListExtra(Pix.IMAGE_RESULTS)

            val intent = Intent(this, SendMedia::class.java)
            intent.putExtra("images", returnValue)
            intent.putExtra("fromID", FirebaseAuth.getInstance().uid)
            intent.putExtra("toID", toUser!!.uid)

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
            else {
                startService(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            PermUtil.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImage()
                }
                else {
                    Toast.makeText(this@ChatLogActivity, "Approve permissions to open images", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun translateText() {
        val fromID = FirebaseAuth.getInstance().uid
        val currentUserLanguageCode = HomePage.currentUser?.languageID!!
        val toID = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chatMessage = it.getValue(ChatMessage::class.java)
                    val key = it.key.toString()

                    if (chatMessage != null) {
                        if (chatMessage.type == "image" || chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                            return@forEach
                        } else {
                            val languageIdentifier = getClient()
                            languageIdentifier.identifyLanguage(chatMessage.text)
                                .addOnSuccessListener { languageCode ->
                                    if (languageCode == "und") {
                                        ref.child(key).child("translatedMessage")
                                            .setValue(chatMessage.text)
                                    } else {
                                        val options = TranslatorOptions.Builder()
                                            .setSourceLanguage(languageCode)
                                            .setTargetLanguage(currentUserLanguageCode)
                                            .build()

                                        val translator = Translation.getClient(options)

                                        val conditions = DownloadConditions.Builder().build()

                                        translator.downloadModelIfNeeded(conditions)
                                            .addOnSuccessListener {
                                                translator.translate(chatMessage.text)
                                                    .addOnSuccessListener {
                                                        ref.child(key).child("translatedMessage")
                                                            .setValue(it)
                                                    }
                                            }
                                            .addOnFailureListener {
                                                // Model Failed
                                            }
                                    }
                                }.addOnFailureListener {
                                // Nada
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }

        })
    }

    class ChatMessage(
        val id: String,
        val text: String,
        val fromID: String,
        val toID: String,
        var translatedMessage: String,
        val type: String,
        val imageUri: String,
        val timestamp: Long
    ) {
        constructor() : this("", "", "", "", "", "", "", -1)
    }

    private fun sendMessage() {
        val text = newMessageText.text.toString()
        val fromID = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toID = user!!.uid

        if (fromID == null) {
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID").push()
        val toReference = FirebaseDatabase.getInstance().getReference("messages/$toID/$fromID").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromID, toID, "", "text", "", System.currentTimeMillis() / 1000)
        reference.setValue(chatMessage).addOnSuccessListener {
            newMessageText.text.clear()
            recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
        }
        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromID/$toID")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toID/$fromID")
        latestMessageToRef.setValue(chatMessage)
    }

    private fun listenForMessages() {
        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                if (chatMessage != null) {
                    translateText()
                    if (chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                        if(chatMessage.type == "text") {
                            adapter.add(ChatToItem(chatMessage.text))
                        }
                        else {
                            adapter.add(ChatToImageItem(chatMessage.imageUri))
                        }
                    }
                    else {
                        if(checkTranslatedMessage) {
                            return
                        }
                        else {
                            if(chatMessage.type == "text") {
                                adapter.add(ChatFromItem(chatMessage.text, toUser!!))
                            }
                            else {
                                adapter.add(ChatFromImageItem(chatMessage.imageUri, toUser!!))
                            }
                        }
                    }
                }
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val changedChatMessage = snapshot.getValue(ChatMessage::class.java)

                if(changedChatMessage != null) {
                    if (checkTranslatedMessage) {
                        adapter.add(ChatFromItem(changedChatMessage.translatedMessage, toUser!!))
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // nada
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // nada
            }

            override fun onCancelled(error: DatabaseError) {
                // nada
            }

        })
    }

    private fun listenForTranslatedMessages(checkTranslate: Boolean) {
        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.clear()
                snapshot.children.forEach {
                    val chatMessage = it.getValue(ChatMessage::class.java)

                    if (chatMessage != null) {
                        if (chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                            adapter.add(ChatToItem(chatMessage.text))
                        } else {
                            if (checkTranslate) {
                                adapter.add(ChatFromItem(chatMessage.translatedMessage, toUser!!))
                            } else {
                                adapter.add(ChatFromItem(chatMessage.text, toUser!!))
                            }
                        }
                    }
                }
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }
        })
    }

    private fun pickImage() {
        val options: Options = Options.init()
            .setRequestCode(100) //Request code for activity results
            .setCount(3) //Number of images to restict selection count
            .setFrontfacing(false) //Front Facing camera on start
            .setSpanCount(4) //Span count for gallery min 1 & max 5
            .setVideoDurationLimitinSeconds(30) //Duration for video recording
            .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT) //Orientaion
            .setPath("/image/")

        Pix.start(this@ChatLogActivity, options)
    }
}

class ChatFromItem(val text: String, private val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userFromText.text = text

        val uri = user.flagUrl
        val image = viewHolder.itemView.userFromImage
        Picasso.get().load(uri).into(image)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from
    }
}

class ChatToItem(val text: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userToText.text = text
    }

    override fun getLayout(): Int {
        return R.layout.chat_to
    }
}

class ChatToImageItem(val imageUri: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        val image = viewHolder.itemView.chatMessageImage
        Picasso.get().load(imageUri).into(image)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_image
    }
}

class ChatFromImageItem(val imageUri: String, private val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        val uri = user.flagUrl
        val image = viewHolder.itemView.userImageMessageFrom
        Picasso.get().load(uri).into(image)

        val imageMessage = viewHolder.itemView.imageMessageFrom
        Picasso.get().load(imageUri).into(imageMessage)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_image
    }
}