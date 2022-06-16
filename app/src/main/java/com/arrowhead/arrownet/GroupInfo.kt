package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.ActionBar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.group_info_toolbar.*

class GroupInfo : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var uidList: ArrayList<String> = arrayListOf()
    private var toUsers = HashMap<String, GroupMember>()
    private var groupID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)

        val name = intent.getStringExtra("GroupName")
        val image = intent.getStringExtra("ImageKey")
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = HomePage.contactsList
        uidList = intent.getSerializableExtra("UidList") as ArrayList<String>
        toUsers = intent.getSerializableExtra("toUsers") as HashMap<String, GroupMember>

        Picasso.get().load(image).into(group_info_photo)
        group_info_name.text = name
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.group_info_toolbar)
        group_name_info.text = "Group Information"

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
    }
}