package com.arrowhead.arrownet

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row.view.*

class NewMessageActivity : AppCompatActivity() {
    private val contactsList = HashMap<String, String>()
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select a User"

        mDatabase = FirebaseDatabase.getInstance().getReference("users")

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1) {android.Manifest.permission.READ_CONTACTS}, 111)
        }
        else {
            readContacts()
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

    companion object {
        val USER_KEY = "USER_KEY"
        val NAME_KEY = "NAME_KEY"
    }

    private fun readContacts() {
        val phoneCursor: Cursor? = applicationContext.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        if(phoneCursor != null && phoneCursor.count > 0) {
            val contactName = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while(phoneCursor.moveToNext()) {
                var number: String = phoneCursor.getString(numberIndex)
                val name: String = phoneCursor.getString(contactName)
                number = number.replace("[\\s-]".toRegex(), "")
                contactsList.put(number, name)
            }
            phoneCursor.close()
        }

        mDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()

                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    val number = user?.phoneNumber.toString()
                    if (user != null) {
                        if(contactsList.containsKey(number)) {
                            val contactName = contactsList.get(number).toString()
                            adapter.add(UserItem(user, contactName))
                        }
                    }

                    adapter.setOnItemClickListener { item, view ->
                        val userItem = item as UserItem
                        val intent = Intent(view.context, ChatLogActivity::class.java)
                        intent.putExtra(USER_KEY, userItem.user)
                        intent.putExtra(NAME_KEY, userItem.displayName)
                        startActivity(intent)
                        finish()
                    }
                    recyclerview_newmessage.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }
        })
    }
}

class UserItem(val user: User, val displayName: String): Item<ViewHolder>() {
    constructor() : this(User("", "", "", ""), "")
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.destinationUser.text = displayName
        Picasso.get().load(user.photoUrl).into(viewHolder.itemView.userPicture)
    }

    override fun getLayout(): Int {
        return R.layout.user_row
    }
}