package com.arrowhead.arrownet

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_new_group.*
import kotlinx.android.synthetic.main.activity_settings_view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class NewGroupActivity : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private lateinit var mDatabase: DatabaseReference
    private var userList: ArrayList<User> = arrayListOf()
    private var selectedPhotoUri: Uri? = null
    private lateinit var groupPhotoUri: String
    private lateinit var groupName: String
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_group)

        groupPhotoUri = Uri.parse("android.resource://com.arrowhead.arrownet/" + R.drawable.blank_profile).toString()

        actionBar?.setDisplayHomeAsUpEnabled(true)

        contactsList = intent.getSerializableExtra("ContactsList") as HashMap<String, String>
        userList = intent.getSerializableExtra("UserList") as ArrayList<User>
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        group_image.setOnClickListener {
            pickFromGallery()
        }

        create_group_button.setOnClickListener {
            uploadImageToFirebase()
            createGroup()
            val intent = Intent(applicationContext, GroupChatLogActivity::class.java)
            intent.putExtra("USER_KEY", userList)
            intent.putExtra("ContactsList", contactsList)
            intent.putExtra("NAME_KEY", groupName)
            startActivity(intent)
            finish()
        }
    }

    private fun createGroup() {
        groupName = group_name.text.toString()
        val group = Group(groupName, groupPhotoUri)
        mDatabase = FirebaseDatabase.getInstance().getReference("groups").push()

        mDatabase.child("group-details").setValue(group)

        val admin = GroupMember(uid, "admin")

        mDatabase.child("members").child(uid).setValue(admin)
        for(User in userList) {
            val newMember = GroupMember(User.uid, "member")
            mDatabase.child("members").child(User.uid).setValue(newMember)
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, SettingsView.IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == SettingsView.IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            photo_button.setImageURI(data?.data)
            if (data != null) {
                selectedPhotoUri = data.data
            }
        }
    }

    private fun uploadImageToFirebase() {
        if(selectedPhotoUri == null) {
            return
        }
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity","Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("RegisterActivity","File Location: $it")
                    groupPhotoUri = it.toString()
                }
            }
    }
}

class Group(val GroupName: String, val GroupImageURI: String)

class GroupMember(val uid: String, val role: String)