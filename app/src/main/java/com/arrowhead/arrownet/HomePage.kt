package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserInfo
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_home_page.*
import kotlinx.android.synthetic.main.latest_message_row.view.*

class HomePage : AppCompatActivity() {
    companion object {
        var currentUser: User? = null
    }
    val latestMessagesMap = HashMap<String, ChatLogActivity.ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        recyclerview_latest_message.adapter = adapter
        recyclerview_latest_message.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter.setOnItemClickListener { item, view ->
            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as LatestMessageRow
            intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartnerUser)
            intent.putExtra(NewMessageActivity.NAME_KEY, row.name)
            startActivity(intent)
        }

        listenForLatestMessages()

        retrieveCurrentUser()
        checkIfUserLoggedIn()

        newMessageButton.setOnClickListener {
            val intent = Intent(this, NewMessageActivity::class.java)
            startActivity(intent)
        }
    }

    class LatestMessageRow(val chatMessage: ChatLogActivity.ChatMessage): Item<ViewHolder>() {
        var chatPartnerUser: User? = null
        var name: String? = null
        override fun bind(viewHolder: ViewHolder, position: Int) {
            viewHolder.itemView.latest_message_text.text = chatMessage.text

            val chatPartnerID: String
            if(chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                chatPartnerID = chatMessage.toID
            }
            else {
                chatPartnerID = chatMessage.fromID
            }

            val ref = FirebaseDatabase.getInstance().getReference("users/$chatPartnerID")
            ref.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatPartnerUser = snapshot.getValue(User::class.java)
                    name = chatPartnerUser?.userName
                    viewHolder.itemView.latest_message_username.text = name

                    val targetImage = viewHolder.itemView.latest_message_user_picture
                    Picasso.get().load(chatPartnerUser?.photoUrl).into(targetImage)
                }

                override fun onCancelled(error: DatabaseError) {
                    // nada
                }

            })
        }

        override fun getLayout(): Int {
            return R.layout.latest_message_row
        }

    }

    private fun refreshMessages() {
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessageRow(it))
        }
    }

    private fun listenForLatestMessages() {
        val fromID = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("latest-messages/$fromID")
        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatLogActivity.ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatLogActivity.ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshMessages()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                //nada
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                //nada
            }

            override fun onCancelled(error: DatabaseError) {
                //nada
            }

        })
    }

    val adapter = GroupAdapter<ViewHolder>()

    private fun retrieveCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }

        })
    }

    private fun checkIfUserLoggedIn() {
        val uid = FirebaseAuth.getInstance().uid
        if(uid == null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.logout_option ->
            {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                return true
            }
            R.id.settings_option ->
            {
                val intent = Intent(this, SettingsView::class.java)
                startActivity(intent)
            }
        }
        return false
    }
}