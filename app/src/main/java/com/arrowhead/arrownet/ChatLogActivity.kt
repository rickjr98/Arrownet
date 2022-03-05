package com.arrowhead.arrownet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
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

class ChatLogActivity : AppCompatActivity() {
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null

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

        recyclerview_chat.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title_name.text = name
        Picasso.get().load(toUser?.photoUrl).into(profile_image)

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
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.translate ->
            {
                return false
            }
        }
        return false
    }

    class ChatMessage(val id: String, val text: String, val fromID: String, val toID: String, val timestamp: Long) {
        constructor() : this("", "", "", "", -1)
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

        val chatMessage = ChatMessage(reference.key!!, text, fromID, toID, System.currentTimeMillis() / 1000)
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
                        adapter.add(ChatFromItem(chatMessage.text, toUser!!))
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