package com.arrowhead.arrownet

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_home_page.*
import kotlinx.android.synthetic.main.latest_message_row.view.*

class HomePage : AppCompatActivity() {
    companion object {
        var currentUser: User? = null
        val contactsList = HashMap<String, String>()
        val USER_KEY = "USER_KEY"
        val NAME_KEY = "NAME_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        retrieveCurrentUser()
        checkIfUserLoggedIn()

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1) {android.Manifest.permission.READ_CONTACTS}, 111)
        }
        else {
            readContacts()
        }

        view_page_adapter.adapter = PageAdapter(supportFragmentManager, contactsList)
        home_page_tabs.setupWithViewPager(view_page_adapter)

        newMessageButton.setOnClickListener {
            val intent = Intent(this, NewMessageActivity::class.java)
            intent.putExtra("ContactsList", contactsList)
            startActivity(intent)
        }

        newGroupButton.setOnClickListener {
            val intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("ContactsList", contactsList)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            readContacts()
        }
    }

    class LatestMessageRow(private val chatMessage: ChatLogActivity.ChatMessage, private val contacts: HashMap<String, String>): Item<ViewHolder>() {
        var chatPartnerUser: User? = null
        lateinit var name: String
        override fun bind(viewHolder: ViewHolder, position: Int) {
            if(chatMessage.type == "text") {
                viewHolder.itemView.latest_message_text.text = chatMessage.text
            }
            else {
                viewHolder.itemView.latest_message_text.text = "IMAGE"
            }

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
                    val number = chatPartnerUser?.phoneNumber.toString()
                    if(contacts.containsKey(number)) {
                        viewHolder.itemView.latest_message_username.text = contacts[number].toString()
                        name = viewHolder.itemView.latest_message_username.text.toString()
                    }
                    else {
                        viewHolder.itemView.latest_message_username.text = chatPartnerUser?.userName
                        name = viewHolder.itemView.latest_message_username.text.toString()
                    }

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

    class LatestGroupMessageRow(private val groupMessage: GroupChatLogActivity.GroupChatMessage): Item<ViewHolder>() {
        var group: Group? = null
        val toUsers = HashMap<String, GroupMember>()
        val uidList: ArrayList<String> = arrayListOf()
        override fun bind(viewHolder: ViewHolder, position: Int) {
            if(groupMessage.type == "text") {
                viewHolder.itemView.latest_message_text.text = groupMessage.text
            }
            else {
                viewHolder.itemView.latest_message_text.text = "IMAGE"
            }

            val groupID: String = groupMessage.groupID

            val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/group-details")
            ref.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    group = snapshot.getValue(Group::class.java)
                    viewHolder.itemView.latest_message_username.text = group?.GroupName

                    val targetImage = viewHolder.itemView.latest_message_user_picture
                    Picasso.get().load(group?.GroupImageURI).into(targetImage)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Nada
                }

            })

            val membersReference = FirebaseDatabase.getInstance().getReference("groups/$groupID/members")
            membersReference.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val groupMember = it.getValue(GroupMember::class.java)
                        if (groupMember != null) {
                            toUsers[groupMember.uid] = groupMember
                            uidList.add(groupMember.uid)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Nada
                }
            })
        }

        override fun getLayout(): Int {
            return R.layout.latest_message_row
        }

    }

    private fun retrieveCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                ref.child("translate").setValue(false)
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

    private fun readContacts() {
        val phoneCursor: Cursor? = applicationContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        if (phoneCursor != null && phoneCursor.count > 0) {
            val contactName =
                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex =
                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (phoneCursor.moveToNext()) {
                var number: String = phoneCursor.getString(numberIndex)
                val name: String = phoneCursor.getString(contactName)
                number = number.replace("[\\s-]".toRegex(), "")
                contactsList[number] = name
            }
            phoneCursor.close()
        }
    }
}