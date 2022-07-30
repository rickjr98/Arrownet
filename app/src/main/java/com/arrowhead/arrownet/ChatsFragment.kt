package com.arrowhead.arrownet

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_home_page.*
import kotlinx.android.synthetic.main.fragment_chats.*
import java.lang.reflect.Array
import java.util.ArrayList

class ChatsFragment : Fragment() {
    private var contactsList = HashMap<String, String>()
    val latestMessagesMap = HashMap<String, ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsList = arguments?.getSerializable("ContactsList") as HashMap<String, String>

        listenForLatestMessages()

        adapter.setOnItemClickListener { item, view ->
            val row = item as HomePage.LatestMessageRow
            if(row.chatType == "chat") {
                val intent = Intent(view.context, ChatLogActivity::class.java)
                intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartnerUser)
                intent.putExtra(NewMessageActivity.NAME_KEY, row.name)
                startActivity(intent)
                activity?.finish()
            }
            else if(row.chatType == "group") {
                val intent = Intent(view.context, GroupChatLogActivity::class.java)
                intent.putExtra("GroupName", row.group?.GroupName)
                intent.putExtra("ImageKey", row.group?.GroupImageURI)
                intent.putExtra("ContactsList", contactsList)
                intent.putExtra("GroupID", row.group?.GroupID)
                intent.putExtra("UidList", row.uidList)
                intent.putExtra("toUsers", row.toUsers)
                startActivity(intent)
                activity?.finish()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerview_latest_message.layoutManager = LinearLayoutManager(activity)
        recyclerview_latest_message.adapter = adapter
        recyclerview_latest_message.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    }

    private fun refreshMessages() {
        adapter.clear()
        val sortedHashMap = latestMessagesMap.toList().sortedByDescending { (_, v) -> v.timestamp }.toMap()
        sortedHashMap.values.forEach {
            adapter.add(HomePage.LatestMessageRow(it, contactsList))
        }
    }

    private fun listenForLatestMessages() {
        val fromID = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("latest-messages/$fromID")
        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
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

    private val adapter = GroupAdapter<ViewHolder>()
}