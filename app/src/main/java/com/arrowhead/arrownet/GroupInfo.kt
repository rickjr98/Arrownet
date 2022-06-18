package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.view.menu.MenuView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.group_info_member_item.view.*
import kotlinx.android.synthetic.main.group_info_toolbar.*

class GroupInfo : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var uidList: ArrayList<String> = arrayListOf()
    private var toUsers = HashMap<String, GroupMember>()
    private var groupID: String = ""
    private var fromID: String = ""
    private val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)

        val name = intent.getStringExtra("GroupName")
        val image = intent.getStringExtra("ImageKey")
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = HomePage.contactsList
        uidList = intent.getSerializableExtra("UidList") as ArrayList<String>
        toUsers = intent.getSerializableExtra("toUsers") as HashMap<String, GroupMember>
        fromID = HomePage.currentUser!!.uid

        Picasso.get().load(image).into(group_info_photo)
        group_info_name.text = name
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.group_info_toolbar)
        group_name_info.text = "Group Information"

        group_info_member_list.adapter = adapter

        group_info_back_button.setOnClickListener {
            val intent = Intent(this, GroupChatLogActivity::class.java)
            intent.putExtra("ContactsList", contactsList)
            intent.putExtra("toUsers", toUsers)
            intent.putExtra("GroupName", name)
            intent.putExtra("ImageKey", image)
            intent.putExtra("UidList", uidList)
            intent.putExtra("GroupID", groupID)
            startActivity(intent)
            finish()
        }

        loadUsers()
    }

    private fun loadUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val groupMember = snapshot.getValue(GroupMember::class.java)

                if(groupMember != null) {
                    adapter.add(GroupUser(groupMember))
                }
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

class GroupUser(private val groupMember: GroupMember): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        val profileUri = groupMember.profilePicture
        val image = viewHolder.itemView.group_info_member_image
        Picasso.get().load(profileUri).into(image)

        viewHolder.itemView.group_info_member_name.text = groupMember.displayName
        viewHolder.itemView.group_info_member_role.text = groupMember.role
    }

    override fun getLayout(): Int {
        return R.layout.group_info_member_item
    }
}