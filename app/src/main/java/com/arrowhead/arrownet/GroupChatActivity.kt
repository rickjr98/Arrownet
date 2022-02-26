package com.arrowhead.arrownet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_group_chat.*
import kotlinx.android.synthetic.main.user_row.view.*

class GroupChatActivity : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        supportActionBar?.title = "Select Users"

        mDatabase = FirebaseDatabase.getInstance().getReference("users")

        contactsList = intent.getSerializableExtra("ContactsList") as HashMap<String, String>

        selectGroupUsers()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.group_chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.new_group_button ->
            {
                //val intent = Intent(applicationContext, ChatLogActivity::class.java)
                //intent.putExtra(USER_LIST, list)
                //startActivity(intent)
                //finish()
            }
        }
        return false
    }

    private fun selectGroupUsers() {
        mDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()

                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    val number = user?.phoneNumber.toString()
                    if (user != null) {
                        if(contactsList.containsKey(number)) {
                            val contactName = contactsList[number].toString()
                            adapter.add(GroupUserItem(user, contactName))
                        }
                    }

                    adapter.setOnItemClickListener { item, view ->
                        val userItem = item as GroupUserItem
                        userItem.isSelected = !userItem.isSelected
                        if(userItem.isSelected) {
                            view.isSelectedImage.visibility = View.VISIBLE
                        }
                        else {
                            view.isSelectedImage.visibility = View.INVISIBLE
                        }
                    }
                    group_recycler.adapter = adapter
                    group_recycler.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }
        })
    }
}

class GroupUserItem(val user: User, val displayName: String): Item<ViewHolder>() {
    var isSelected: Boolean = false

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.destinationUser.text = displayName
        Picasso.get().load(user.photoUrl).into(viewHolder.itemView.userPicture)
    }

    override fun getLayout(): Int {
        return R.layout.user_row
    }
}