package com.arrowhead.arrownet

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.fragment_chats.*
import kotlinx.android.synthetic.main.fragment_groups.*

class GroupsFragment : Fragment() {
    private var contactsList = HashMap<String, String>()
    val latestGroupMessagesMap = HashMap<String, GroupChatLogActivity.GroupChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsList = arguments?.getSerializable("ContactsList") as HashMap<String, String>

        listenForLatestMessages()

        adapter.setOnItemClickListener { item, view ->
            val intent = Intent(view.context, GroupChatLogActivity::class.java)
            val row = item as HomePage.LatestGroupMessageRow
            intent.putExtra("GroupName", row.group?.GroupName)
            intent.putExtra("ImageKey", row.group?.GroupImageURI)
            intent.putExtra("ContactsList", contactsList)
            intent.putExtra("GroupID", row.group?.GroupID)
            startActivity(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerview_latest_group.layoutManager = LinearLayoutManager(activity)
        recyclerview_latest_group.adapter = adapter
        recyclerview_latest_group.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    }

    private fun refreshMessages() {
        adapter.clear()
        latestGroupMessagesMap.values.forEach {
            adapter.add(HomePage.LatestGroupMessageRow(it))
        }
    }

    private fun listenForLatestMessages() {
        val fromID = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("latest-group-messages/$fromID")
        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupChatLogActivity.GroupChatMessage::class.java) ?: return
                latestGroupMessagesMap[snapshot.key!!] = chatMessage
                refreshMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupChatLogActivity.GroupChatMessage::class.java) ?: return
                latestGroupMessagesMap[snapshot.key!!] = chatMessage
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