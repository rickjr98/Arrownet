package com.arrowhead.arrownet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_group_info.*

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent.putExtra("ContactsList", contactsList)
        intent.putExtra("toUsers", toUsers)
        intent.putExtra("GroupName", name)
        intent.putExtra("ImageKey", image)
        intent.putExtra("UidList", uidList)
        intent.putExtra("GroupID", groupID)

        Log.d("UID", uidList.toString())
    }
}