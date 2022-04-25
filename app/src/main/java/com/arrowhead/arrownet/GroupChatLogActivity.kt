package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_group_chat_log.*
import kotlinx.android.synthetic.main.chat_from.view.*
import kotlinx.android.synthetic.main.chat_from.view.userFromText
import kotlinx.android.synthetic.main.chat_from_group.view.*
import kotlinx.android.synthetic.main.chat_to.view.*
import kotlinx.android.synthetic.main.chat_toolbar.*

class GroupChatLogActivity : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var uidList: ArrayList<String> = arrayListOf()
    private var toUsers = HashMap<String, GroupMember>()
    val adapter = GroupAdapter<ViewHolder>()
    private var groupID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat_log)

        val name = intent.getStringExtra("GroupName")
        val image = intent.getStringExtra("ImageKey")
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = HomePage.contactsList
        uidList = intent.getSerializableExtra("UidList") as ArrayList<String>
        toUsers = intent.getSerializableExtra("toUsers") as HashMap<String, GroupMember>

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

        title_name.setOnClickListener {
            val intent = Intent(this, GroupInfo::class.java)
            intent.putExtra("ContactsList", contactsList)
            intent.putExtra("toUsers", toUsers)
            intent.putExtra("GroupName", name)
            intent.putExtra("ImageKey", image)
            intent.putExtra("UidList", uidList)
            intent.putExtra("GroupID", groupID)
            startActivity(intent)
            finish()
        }
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

        for(User in uidList) {
            val latestGroupReferenceTo = FirebaseDatabase.getInstance().getReference("latest-group-messages/$User/$groupID")
            latestGroupReferenceTo.setValue(groupChatMessage)
        }
    }

    private fun listenForMessages() {
        val reference = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages")

        reference.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupChatMessage::class.java)

                if(chatMessage != null) {
                    if(chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                        val currentUser = HomePage.currentUser ?: return
                        adapter.add(ChatToItem(chatMessage.text, currentUser))
                    }
                    else {
                        val toUser: GroupMember? = toUsers[chatMessage.fromID]
                        toUser?.let { ChatFromGroupItem(chatMessage.text, it) }?.let {
                            adapter.add(it)
                        }
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

class ChatFromGroupItem(val text: String, private val member: GroupMember): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userFromGroupText.text = text
        viewHolder.itemView.userFromGroupName.text = member.displayName

        val uri = member.flagUrl
        val image = viewHolder.itemView.userFromImageGroup
        Picasso.get().load(uri).into(image)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_group
    }

}