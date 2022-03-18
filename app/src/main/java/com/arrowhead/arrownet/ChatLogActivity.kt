package com.arrowhead.arrownet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification.getClient
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_chat_log.view.*
import kotlinx.android.synthetic.main.activity_home_page.*
import kotlinx.android.synthetic.main.chat_from.view.*
import kotlinx.android.synthetic.main.chat_to.view.*
import kotlinx.android.synthetic.main.chat_toolbar.*
import java.util.*
import kotlin.collections.HashMap

class ChatLogActivity : AppCompatActivity() {
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null
    private var translate: Boolean = false
    private val userReference = FirebaseDatabase.getInstance().getReference("users/${HomePage.currentUser?.uid}")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        var name = intent.getStringExtra(NewMessageActivity.NAME_KEY)
        if(name == null) {
            name = intent.getStringExtra(HomePage.NAME_KEY)
        }
        toUser = intent.getParcelableExtra(NewMessageActivity.USER_KEY)
        if(toUser == null) {
            toUser = intent.getParcelableExtra(HomePage.USER_KEY)
        }
        userReference.child("translate").setValue(false)

        recyclerview_chat.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title_name.text = name
        Picasso.get().load(toUser?.photoUrl).into(profile_image)
        Picasso.get().load(HomePage.currentUser?.flagUrl).into(translate_image)

        listenForMessages()

        sendButton.setOnClickListener {
            if(newMessageText.text.isNotEmpty()) {
                sendMessage()
            }
            else {
                return@setOnClickListener
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)

        val item = menu!!.findItem(R.id.translate_button)
        item.setActionView(R.layout.switch_item)
        val translateButton = item.actionView.findViewById<SwitchCompat>(R.id.translate_switch)
        translateButton.setOnCheckedChangeListener { _, p1 ->
            if (p1) {
                translate = true
                adapter.clear()
                translateText()
                listenForMessages()
            } else {
                translate = false
                adapter.clear()
                listenForMessages()
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun translateText() {
        val fromID = FirebaseAuth.getInstance().uid
        val currentUserLanguageCode = HomePage.currentUser?.languageID!!
        val toID = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chatMessage = it.getValue(ChatMessage::class.java)
                    val key = it.key.toString()

                    if(chatMessage != null) {
                        if(chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                            return@forEach
                        }
                        else {
                            val languageIdentifier = getClient()
                            languageIdentifier.identifyLanguage(chatMessage.text).addOnSuccessListener { languageCode ->
                                if (languageCode == "und") {
                                    Log.d("HERE", "Can't identify language.")
                                }
                                else {
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
                                                    ref.child(key).child("translatedMessage").setValue(it)
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

    class ChatMessage(val id: String, val text: String, val fromID: String, val toID: String, var translatedMessage: String) {
        constructor() : this("", "", "", "", "")
    }

    private fun sendMessage() {
        val text = newMessageText.text.toString()
        val fromID = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toID = user!!.uid

        if(fromID == null) {
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID").push()
        val toReference = FirebaseDatabase.getInstance().getReference("messages/$toID/$fromID").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromID, toID, "")
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

        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                if(chatMessage != null) {
                    if(chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                        val currentUser = HomePage.currentUser ?: return
                        adapter.add(ChatToItem(chatMessage.text, currentUser))
                    }
                    else {
                        if(translate) {
                            adapter.add(ChatFromItem(chatMessage.translatedMessage, toUser!!))
                        }
                        else {
                            adapter.add(ChatFromItem(chatMessage.text, toUser!!))
                        }
                    }
                }
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // nada
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

class ChatToItem(val text: String, private val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userToText.text = text

        val uri = user.flagUrl
        val image = viewHolder.itemView.userToImage
        Picasso.get().load(uri).into(image)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to
    }
}