package com.arrowhead.arrownet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_group_chat_log.*
import kotlinx.android.synthetic.main.chat_toolbar.*

class GroupChatLogActivity : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var userList: ArrayList<User> = arrayListOf()
    private lateinit var groupID: String
    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat_log)

        val name = intent.getStringExtra("GroupName")
        val image = intent.getStringExtra("ImageKey")
        userList = intent.getSerializableExtra("UserList") as ArrayList<User>
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = intent.getSerializableExtra("ContactsList") as HashMap<String, String>

        group_chat_recyclerview.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title_name.text = name
        Picasso.get().load(image).into(profile_image)

        listenForMessages()

        group_chat_send_button.setOnClickListener {
            if(group_chat_text.text.isNotEmpty()) {
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

    class GroupChatMessage(val id: String, val text: String, val fromID: String?, val groupID: String) {
        constructor() : this("", "", "", "")
    }

    private fun sendMessage() {
        val text = group_chat_text.text.toString()
        val fromID = FirebaseAuth.getInstance().uid

        val reference = FirebaseDatabase.getInstance().getReference("groups/$groupID").child("messages").push()
        val groupChatMessage = GroupChatMessage(reference.key!!, text, fromID, groupID)
        reference.setValue(groupChatMessage).addOnSuccessListener {
            group_chat_text.text.clear()
            group_chat_recyclerview.scrollToPosition(adapter.itemCount - 1)
        }

        val latestGroupReference = FirebaseDatabase.getInstance().getReference("latest-group-messages/$fromID/$groupID")
        latestGroupReference.setValue(groupChatMessage)

        for(User in userList) {
            val toID = User.uid
            val latestGroupReferenceTo = FirebaseDatabase.getInstance().getReference("latest-group-messages/$toID/$groupID")
            latestGroupReferenceTo.setValue(groupChatMessage)
        }
    }

    private fun listenForMessages() {
        val reference = FirebaseDatabase.getInstance().getReference("groups/$groupID").child("messages")

        reference.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupChatMessage::class.java)

                if(chatMessage != null) {
                    if(chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                        val currentUser = HomePage.currentUser ?: return
                        adapter.add(ChatToItem(chatMessage.text, currentUser))
                    }
                    else {
                        //adapter.add(ChatFromItem(chatMessage.text))
                    }
                }
                group_chat_recyclerview.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Nada
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Nada
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Nada
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }

        })
    }
}