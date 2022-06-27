package com.arrowhead.arrownet

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.activity_group_info.view.*
import kotlinx.android.synthetic.main.activity_settings_view.*
import kotlinx.android.synthetic.main.group_info_member_item.view.*
import kotlinx.android.synthetic.main.group_info_toolbar.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class GroupInfo : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var uidList: ArrayList<String> = arrayListOf()
    private var toUsers = HashMap<String, GroupMember>()
    private var groupID: String = ""
    private var fromID: String = ""
    private val adapter = GroupAdapter<ViewHolder>()
    private var myRole: String = ""
    private var adminCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)

        val name = intent.getStringExtra("GroupName")
        val image = intent.getStringExtra("ImageKey")
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = intent.getSerializableExtra("ContactsList") as HashMap<String, String>
        uidList = intent.getSerializableExtra("UidList") as ArrayList<String>
        toUsers = intent.getSerializableExtra("toUsers") as HashMap<String, GroupMember>
        fromID = FirebaseAuth.getInstance().uid.toString()

        Picasso.get().load(image).into(group_info_photo)
        group_info_name.setText(name)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.group_info_toolbar)
        group_name_info.text = "Group Information"

        group_info_member_list.adapter = adapter

        getUserRole()
        loadUsers()
        checkAdminCount()

        group_info_name.addTextChangedListener {
            update_group_info_button.visibility = View.VISIBLE
            update_group_info_button.isClickable = true
        }

        group_info_photo.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, SettingsView.PERMISSION_CODE)
                }
                else {
                    pickFromGallery()
                }
            }
            else {
                pickFromGallery()
            }
        }

        update_group_info_button.setOnClickListener {
            uploadImageToFirebase()
            updateGroupInfo()
            finish()
            startActivity(intent)
        }

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

        add_participants.setOnClickListener {
            val intent = Intent(this, AddGroupMembers::class.java)
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

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, SettingsView.IMAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            SettingsView.PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery()
                }
                else {
                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var selectedPhotoUri: Uri? = null
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
        val groupPhotoRef = FirebaseDatabase.getInstance().getReference("groups/$groupID/group-details")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity","Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("RegisterActivity","File Location: $it")
                    groupPhotoRef.child("groupImageURI").setValue(it.toString())
                }
            }
    }

    private fun updateGroupInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/group-details")
        val name = group_info_name.text.toString()
        ref.child("groupName").setValue(name)
    }

    private fun leaveGroup() {
        val messageRef = FirebaseDatabase.getInstance().getReference("groups/$groupID/members/$fromID/memberStatus")
        messageRef.setValue("inactive")
        toUsers.remove(fromID)
        uidList.remove(fromID)
        if(myRole == "admin") {
            adminCount--
            add_participants.visibility = View.INVISIBLE
            add_participants.isClickable = false
        }
        if(adminCount == 0) {
            val newAdmin = uidList[0]
            val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members/$newAdmin/role")
            ref.setValue("admin")
        }
        delete_group_button.visibility = View.INVISIBLE
        delete_group_button.isClickable = false
        Toast.makeText(applicationContext, "You are no longer a member of this group", Toast.LENGTH_SHORT).show()
    }

    private fun checkAdminCount() {
        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val member = it.getValue(GroupMember::class.java)
                    if(member!!.role == "admin" && member.memberStatus == "active") {
                        adminCount++
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }

        })
    }

    private fun getUserRole() {
        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members/$fromID")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentMember = snapshot.getValue(GroupMember::class.java)
                myRole = currentMember!!.role
                if(myRole != "admin" && currentMember.memberStatus == "active") {
                    add_participants.visibility = View.INVISIBLE
                    add_participants.isClickable = false
                    delete_group_button.setOnClickListener {
                        val dialogTitle = "Leave Group"
                        val dialogDescription = "Are you sure you want to leave this group?"
                        val positiveButtonTitle = "LEAVE"

                        val builder = AlertDialog.Builder(this@GroupInfo)
                        builder.setTitle(dialogTitle)
                            .setMessage(dialogDescription)
                            .setPositiveButton(
                                positiveButtonTitle,
                                DialogInterface.OnClickListener { _, _ ->
                                    leaveGroup()
                                })
                            .setNegativeButton(
                                "CANCEL",
                                DialogInterface.OnClickListener { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                })
                            .show()
                    }
                }
                else if(myRole == "admin" && currentMember.memberStatus == "active") {
                    delete_group_button.setOnClickListener {
                        val dialogTitle = "Leave Group"
                        val dialogDescription = "Are you sure you want to leave this group?"
                        val positiveButtonTitle = "LEAVE"

                        val builder = AlertDialog.Builder(this@GroupInfo)
                        builder.setTitle(dialogTitle)
                            .setMessage(dialogDescription)
                            .setPositiveButton(
                                positiveButtonTitle,
                                DialogInterface.OnClickListener { _, _ ->
                                    leaveGroup()
                                })
                            .setNegativeButton(
                                "CANCEL",
                                DialogInterface.OnClickListener { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                })
                            .show()
                    }
                }
                else if(currentMember.memberStatus == "inactive") {
                    add_participants.visibility = View.INVISIBLE
                    add_participants.isClickable = false
                    delete_group_button.visibility = View.INVISIBLE
                    delete_group_button.isClickable = false
                    group_info_name.isClickable = false
                    group_info_photo.isClickable = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }

        })
    }

    private fun loadUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val groupMember = snapshot.getValue(GroupMember::class.java)

                if(groupMember != null && groupMember.memberStatus == "active") {
                    adapter.add(GroupUser(groupID, applicationContext, myRole, groupMember, uidList, toUsers))
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

class GroupUser(private val groupID: String, private val context: Context, private val myRole: String, private val groupMember: GroupMember, private val uidList: ArrayList<String>, private val toUsers: HashMap<String, GroupMember>): Item<ViewHolder>() {
    val uid = groupMember.uid

    private fun popUpMenu(view: View) {
        val popUpMenus = PopupMenu(context, view)
        popUpMenus.inflate(R.menu.group_member_controls_admin)
        if(groupMember.uid != HomePage.currentUser!!.uid) {
            popUpMenus.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.make_admin -> {
                        val dialogTitle = "Make User Admin"
                        val dialogDescription = "Are you sure you want to make this user an admin?"
                        val positiveButtonTitle = "CONFIRM"

                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(dialogTitle)
                            .setMessage(dialogDescription)
                            .setPositiveButton(
                                positiveButtonTitle,
                                DialogInterface.OnClickListener { _, _ ->
                                    val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members/$uid/role")
                                    ref.setValue("admin")
                                })
                            .setNegativeButton(
                                "CANCEL",
                                DialogInterface.OnClickListener { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                })
                            .show()
                        true
                    }
                    R.id.remove_member -> {
                        val dialogTitle = "Remove User"
                        val dialogDescription = "Are you sure you want to remove this user from the group?"
                        val positiveButtonTitle = "CONFIRM"

                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(dialogTitle)
                            .setMessage(dialogDescription)
                            .setPositiveButton(
                                positiveButtonTitle,
                                DialogInterface.OnClickListener { _, _ ->
                                    val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/members/$uid/memberStatus")
                                    ref.setValue("inactive")
                                    toUsers.remove(uid)
                                    uidList.remove(uid)
                                })
                            .setNegativeButton(
                                "CANCEL",
                                DialogInterface.OnClickListener { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                })
                            .show()
                        true
                    }
                    else -> true
                }
            }
            popUpMenus.show()
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val profileUri = groupMember.profilePicture
        val image = viewHolder.itemView.group_info_member_image
        Picasso.get().load(profileUri).into(image)

        viewHolder.itemView.group_info_member_name.text = groupMember.displayName
        viewHolder.itemView.group_info_member_role.text = groupMember.role

        if(myRole == "admin") {
            viewHolder.itemView.setOnClickListener {
                popUpMenu(it)
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.group_info_member_item
    }
}