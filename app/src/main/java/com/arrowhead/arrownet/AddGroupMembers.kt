package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_add_group_members.*
import kotlinx.android.synthetic.main.activity_group_chat.*
import kotlinx.android.synthetic.main.new_group_toolbar.*
import kotlinx.android.synthetic.main.user_row.view.*

class AddGroupMembers : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var uidList: ArrayList<String> = arrayListOf()
    private var toUsers = HashMap<String, GroupMember>()
    private var groupID: String = ""
    private var fromID: String = ""
    private val adapter = GroupAdapter<ViewHolder>()
    private var userList: ArrayList<User> = arrayListOf()
    private lateinit var groupName: String
    private lateinit var groupPhotoUri: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_group_members)

        groupName = intent.getStringExtra("GroupName")!!
        groupPhotoUri = intent.getStringExtra("ImageKey")!!
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = HomePage.contactsList
        uidList = intent.getSerializableExtra("UidList") as ArrayList<String>
        toUsers = intent.getSerializableExtra("toUsers") as HashMap<String, GroupMember>
        fromID = HomePage.currentUser!!.uid

        add_member_recyclerview.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.new_group_toolbar)
        select_users_title.text = "Select Users to Add"

        selectGroupUsers()

        new_group_back_button.setOnClickListener {
            val intent = Intent(applicationContext, GroupChatLogActivity::class.java)
            intent.putExtra("ContactsList", contactsList)
            intent.putExtra("GroupName", groupName)
            intent.putExtra("ImageKey", groupPhotoUri)
            intent.putExtra("GroupID", groupID)
            intent.putExtra("UidList", uidList)
            intent.putExtra("toUsers", toUsers)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(applicationContext, GroupChatLogActivity::class.java)
        intent.putExtra("ContactsList", contactsList)
        intent.putExtra("GroupName", groupName)
        intent.putExtra("ImageKey", groupPhotoUri)
        intent.putExtra("GroupID", groupID)
        intent.putExtra("UidList", uidList)
        intent.putExtra("toUsers", toUsers)
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.add_members_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.add_members_button ->
            {
                if(userList.size == 0) {
                    Toast.makeText(applicationContext, "Please Select at least 1 user", Toast.LENGTH_SHORT).show()
                    return false
                }
                addNewUsers()
                val intent = Intent(applicationContext, GroupChatLogActivity::class.java)
                intent.putExtra("ContactsList", contactsList)
                intent.putExtra("GroupName", groupName)
                intent.putExtra("ImageKey", groupPhotoUri)
                intent.putExtra("GroupID", groupID)
                intent.putExtra("UidList", uidList)
                intent.putExtra("toUsers", toUsers)
                startActivity(intent)
                finish()
            }
        }
        return false
    }

    private fun selectGroupUsers() {
        val mDatabase = FirebaseDatabase.getInstance().getReference("users")
        mDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()

                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    val number = user?.phoneNumber.toString()
                    if (user != null) {
                        if(contactsList.containsKey(number)) {
                            if(!uidList.contains(user.uid)) {
                                val contactName = contactsList[number].toString()
                                adapter.add(GroupUserItem(user, contactName))
                            }
                        }
                    }

                    adapter.setOnItemClickListener { item, view ->
                        val userItem = item as GroupUserItem
                        userItem.isSelected = !userItem.isSelected
                        if(userItem.isSelected) {
                            view.isSelectedImage.visibility = View.VISIBLE
                            userList.add(userItem.user)
                        }
                        else {
                            view.isSelectedImage.visibility = View.INVISIBLE
                            userList.remove(userItem.user)
                        }
                    }
                    add_member_recyclerview.adapter = adapter
                    add_member_recyclerview.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }
        })
    }

    private fun addNewUsers() {
        val mDatabase = FirebaseDatabase.getInstance().getReference("groups/$groupID")
        for(User in userList) {
            val newMember = GroupMember(User.uid, "member", User.userName, User.flagUrl, User.phoneNumber, User.photoUrl, "active")
            mDatabase.child("members").child(User.uid).setValue(newMember)
            uidList.add(User.uid)
            toUsers[newMember.uid] = newMember
        }
    }
}