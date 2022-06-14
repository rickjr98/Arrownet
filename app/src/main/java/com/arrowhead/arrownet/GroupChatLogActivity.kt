package com.arrowhead.arrownet

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_group_chat_log.*
import kotlinx.android.synthetic.main.chat_from.view.*
import kotlinx.android.synthetic.main.chat_from.view.userFromText
import kotlinx.android.synthetic.main.chat_from_group.view.*
import kotlinx.android.synthetic.main.chat_from_image.view.*
import kotlinx.android.synthetic.main.chat_from_image_group.view.*
import kotlinx.android.synthetic.main.chat_to.view.*
import kotlinx.android.synthetic.main.chat_toolbar.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class GroupChatLogActivity : AppCompatActivity() {
    private var contactsList = HashMap<String, String>()
    private var uidList: ArrayList<String> = arrayListOf()
    private var toUsers = HashMap<String, GroupMember>()
    private var checkTranslatedMessage: Boolean = false
    val adapter = GroupAdapter<ViewHolder>()
    private var groupID: String = ""
    private var fileUri: Uri? = null
    private val MY_CAMERA_REQUEST_CODE = 7171
    private val IMAGE_REQUEST_CODE = 100
    private var thumbnail: Bitmap? = null
    private var clicked: Boolean = false

    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat_log)

        val name = intent.getStringExtra("GroupName")
        val image = intent.getStringExtra("ImageKey")
        groupID = intent.getStringExtra("GroupID").toString()
        contactsList = HomePage.contactsList
        uidList = intent.getSerializableExtra("UidList") as ArrayList<String>
        toUsers = intent.getSerializableExtra("toUsers") as HashMap<String, GroupMember>

        group_chat_recyclerview.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title_name.text = name
        Picasso.get().load(image).into(profile_image)

        listenForMessages()

        translate_switch.setOnCheckedChangeListener { _, p1 ->
            checkTranslatedMessage = p1
            listenForTranslatedMessages(p1)
        }

        group_chat_send_button.setOnClickListener {
            if(group_chat_text.text.isNotEmpty() || group_image_message_preview.isVisible) {
                sendMessage()
            }
            else {
                return@setOnClickListener
            }
        }

        group_image_message_button.setOnClickListener {
            onImageButtonPressed()
        }

        group_take_picture.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(Manifest.permission.CAMERA)
                    requestPermissions(permissions, SettingsView.PERMISSION_CODE)
                }
                else {
                    takePicture()
                }
            }
            else {
                takePicture()
            }
        }

        group_choose_image.setOnClickListener {
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

        title_name.setOnClickListener {
            val intent = Intent(this, GroupInfo::class.java)
            intent.putExtra("ContactsList", contactsList)
            intent.putExtra("toUsers", toUsers)
            intent.putExtra("GroupName", name)
            intent.putExtra("ImageKey", image)
            intent.putExtra("UidList", uidList)
            intent.putExtra("GroupID", groupID)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            SettingsView.PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                    pickFromGallery()
                }
                else {
                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK && requestCode == MY_CAMERA_REQUEST_CODE) {
            thumbnail = MediaStore.Images.Media
                .getBitmap(contentResolver, fileUri)
            group_image_message_preview.setImageBitmap(thumbnail)
            group_image_message_preview.visibility = View.VISIBLE
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == IMAGE_REQUEST_CODE) {
            if (data != null) {
                fileUri = data.data
            }
            image_preview.setImageURI(fileUri)
            image_preview.visibility = View.VISIBLE
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    private fun onImageButtonPressed() {
        setVisibility()
        setAnimation()
        clicked = !clicked
    }

    private fun setAnimation() {
        if(!clicked) {
            take_picture_button.startAnimation(fromBottom)
            choose_image_button.startAnimation(fromBottom)
            image_message_button.startAnimation(rotateOpen)
        }
        else {
            take_picture_button.startAnimation(toBottom)
            choose_image_button.startAnimation(toBottom)
            image_message_button.startAnimation(rotateClose)
        }
    }

    private fun setVisibility() {
        if(!clicked) {
            take_picture_button.visibility = View.VISIBLE
            choose_image_button.visibility = View.VISIBLE
        }
        else {
            take_picture_button.visibility = View.INVISIBLE
            choose_image_button.visibility = View.INVISIBLE
        }
    }

    private fun takePicture() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        fileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
        startActivityForResult(intent, MY_CAMERA_REQUEST_CODE)
    }

    class GroupChatMessage(val id: String, val text: String, val fromID: String?, val groupID: String, var translatedText: String, val type: String, val imageUri: String, val timestamp: Long) {
        constructor() : this("", "", "", "", "", "", "", -1)
    }

    private fun sendMessage() {
        val text = group_chat_text.text.toString()
        val fromID = FirebaseAuth.getInstance().uid
        val reference = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages/$fromID").push()

        if(text.isNotEmpty() && group_image_message_preview.isVisible) {
            val groupChatMessage = GroupChatMessage(reference.key!!, text, fromID, groupID, "", "text", "", System.currentTimeMillis())
            reference.setValue(groupChatMessage).addOnSuccessListener {
                group_chat_text.text.clear()
                group_chat_recyclerview.scrollToPosition(adapter.itemCount - 1)
            }
            for(User in uidList) {
                val memberReference = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages/$User").push()
                memberReference.setValue(groupChatMessage)
            }

            val latestGroupReference = FirebaseDatabase.getInstance().getReference("latest-group-messages/$fromID/$groupID")
            latestGroupReference.setValue(groupChatMessage)

            for(User in uidList) {
                val latestGroupReferenceTo = FirebaseDatabase.getInstance().getReference("latest-group-messages/$User/$groupID")
                latestGroupReferenceTo.setValue(groupChatMessage)
            }

            if(fileUri == null) {
                return
            }
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

            ref.putFile(fileUri!!)
                .addOnSuccessListener {
                    Log.d("RegisterActivity","Successfully uploaded image: ${it.metadata?.path}")

                    ref.downloadUrl.addOnSuccessListener {
                        Log.d("RegisterActivity","File Location: $it")
                        val groupChatMessage = GroupChatMessage(reference.key!!, "", fromID, groupID, "", "image", it.toString(), System.currentTimeMillis())
                        reference.setValue(groupChatMessage).addOnSuccessListener {
                            image_preview.visibility = View.GONE
                            recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                        latestGroupReference.setValue(groupChatMessage)

                        for(User in uidList) {
                            val latestGroupReferenceTo = FirebaseDatabase.getInstance().getReference("latest-group-messages/$User/$groupID")
                            latestGroupReferenceTo.setValue(groupChatMessage)
                        }
                    }
                }
        }
        else if(text.isNotEmpty()) {
            val groupChatMessage = GroupChatMessage(reference.key!!, text, fromID, groupID, "", "text", "", System.currentTimeMillis())
            reference.setValue(groupChatMessage).addOnSuccessListener {
                group_chat_text.text.clear()
                group_chat_recyclerview.scrollToPosition(adapter.itemCount - 1)
            }
            for(User in uidList) {
                val memberReference = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages/$User").push()
                memberReference.setValue(groupChatMessage)
            }

            val latestGroupReference = FirebaseDatabase.getInstance().getReference("latest-group-messages/$fromID/$groupID")
            latestGroupReference.setValue(groupChatMessage)

            for(User in uidList) {
                val latestGroupReferenceTo = FirebaseDatabase.getInstance().getReference("latest-group-messages/$User/$groupID")
                latestGroupReferenceTo.setValue(groupChatMessage)
            }
        }
        else if(group_image_message_preview.isVisible) {
            if(fileUri == null) {
                return
            }
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

            ref.putFile(fileUri!!)
                .addOnSuccessListener {
                    Log.d("RegisterActivity","Successfully uploaded image: ${it.metadata?.path}")

                    ref.downloadUrl.addOnSuccessListener {
                        Log.d("RegisterActivity","File Location: $it")
                        val groupChatMessage = GroupChatMessage(reference.key!!, "", fromID, groupID, "", "image", it.toString(), System.currentTimeMillis())
                        reference.setValue(groupChatMessage).addOnSuccessListener {
                            image_preview.visibility = View.GONE
                            recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                        val latestGroupReference = FirebaseDatabase.getInstance().getReference("latest-group-messages/$fromID/$groupID")
                        latestGroupReference.setValue(groupChatMessage)

                        for(User in uidList) {
                            val latestGroupReferenceTo = FirebaseDatabase.getInstance().getReference("latest-group-messages/$User/$groupID")
                            latestGroupReferenceTo.setValue(groupChatMessage)
                        }
                    }
                }
        }
    }

    private fun translateText() {
        val fromID = FirebaseAuth.getInstance().uid
        val currentUserLanguageCode = HomePage.currentUser?.languageID!!
        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages/$fromID")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chatMessage = it.getValue(ChatLogActivity.ChatMessage::class.java)
                    val key = it.key.toString()

                    if (chatMessage != null) {
                        if (chatMessage.type == "image" || chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                            return@forEach
                        } else {
                            val languageIdentifier = LanguageIdentification.getClient()
                            languageIdentifier.identifyLanguage(chatMessage.text)
                                .addOnSuccessListener { languageCode ->
                                    if (languageCode == "und") {
                                        ref.child(key).child("translatedMessage")
                                            .setValue(chatMessage.text)
                                    } else {
                                        val options = TranslatorOptions.Builder()
                                            .setSourceLanguage(languageCode)
                                            .setTargetLanguage(currentUserLanguageCode)
                                            .build()

                                        val translator = Translation.getClient(options)

                                        val conditions = DownloadConditions.Builder().build()

                                        translator.downloadModelIfNeeded(conditions)
                                            .addOnSuccessListener {
                                                translator.translate(chatMessage.text)
                                                    .addOnSuccessListener {
                                                        ref.child(key).child("translatedMessage")
                                                            .setValue(it)
                                                    }
                                            }
                                            .addOnFailureListener {
                                                // Model Failed
                                            }
                                    }
                                }.addOnFailureListener {
                                    // Nada
                                }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }

        })
    }

    private fun listenForTranslatedMessages(checkTranslate: Boolean) {
        val fromID = FirebaseAuth.getInstance().uid

        val ref = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages/$fromID")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.clear()
                snapshot.children.forEach {
                    val chatMessage = it.getValue(GroupChatMessage::class.java)

                    if (chatMessage != null) {
                        if (chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                            if(chatMessage.type == "text") {
                                adapter.add(ChatToItem(chatMessage.text, chatMessage.timestamp))
                            }
                            else {
                                adapter.add(ChatToImageItem(chatMessage.imageUri, chatMessage.timestamp))
                            }
                        } else {
                            if (checkTranslate) {
                                if(chatMessage.type == "text") {
                                    val toUser: GroupMember? = toUsers[chatMessage.fromID]
                                    toUser?.let { ChatFromGroupItem(chatMessage.translatedText, it, chatMessage.timestamp) }?.let {
                                        adapter.add(it)
                                    }
                                }
                                else {
                                    val toUser: GroupMember? = toUsers[chatMessage.fromID]
                                    toUser?.let { ChatFromGroupImageItem(chatMessage.imageUri, toUser, chatMessage.timestamp) }?.let {
                                        adapter.add(it)
                                    }
                                }
                            } else {
                                if(chatMessage.type == "text") {
                                    val toUser: GroupMember? = toUsers[chatMessage.fromID]
                                    toUser?.let { ChatFromGroupItem(chatMessage.text, it, chatMessage.timestamp) }?.let {
                                        adapter.add(it)
                                    }
                                }
                                else {
                                    val toUser: GroupMember? = toUsers[chatMessage.fromID]
                                    toUser?.let { ChatFromGroupImageItem(chatMessage.imageUri, toUser, chatMessage.timestamp) }?.let {
                                        adapter.add(it)
                                    }
                                }
                            }
                        }
                    }
                }
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                // Nada
            }
        })
    }

    private fun listenForMessages() {
        val fromID = FirebaseAuth.getInstance().uid
        val reference = FirebaseDatabase.getInstance().getReference("groups/$groupID/messages/$fromID")

        reference.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(GroupChatMessage::class.java)

                if (chatMessage != null) {
                    translateText()
                    if (chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                        if(chatMessage.type == "text") {
                            adapter.add(ChatToItem(chatMessage.text, chatMessage.timestamp))
                        }
                        else {
                            adapter.add(ChatToImageItem(chatMessage.imageUri, chatMessage.timestamp))
                        }
                    }
                    else {
                        if(checkTranslatedMessage) {
                            return
                        }
                        else {
                            if(chatMessage.type == "text") {
                                val toUser: GroupMember? = toUsers[chatMessage.fromID]
                                toUser?.let { ChatFromGroupItem(chatMessage.text, it, chatMessage.timestamp) }?.let {
                                    adapter.add(it)
                                }
                            }
                            else {
                                val toUser: GroupMember? = toUsers[chatMessage.fromID]
                                toUser?.let { ChatFromGroupImageItem(chatMessage.text, toUser, chatMessage.timestamp) }?.let {
                                    adapter.add(it)
                                }
                            }
                        }
                    }
                }
                group_chat_recyclerview.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val changedChatMessage = snapshot.getValue(GroupChatMessage::class.java)

                if(changedChatMessage != null) {
                    if (checkTranslatedMessage) {
                        val toUser: GroupMember? = toUsers[changedChatMessage.fromID]
                        toUser?.let { ChatFromGroupItem(changedChatMessage.translatedText, it, changedChatMessage.timestamp) }?.let {
                            adapter.add(it)
                        }
                    }
                }
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

class ChatFromGroupItem(val text: String, private val member: GroupMember, private val timestamp: Long): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userFromGroupText.text = text
        viewHolder.itemView.userFromGroupName.text = member.displayName

        val uri = member.flagUrl
        val image = viewHolder.itemView.userFromImageGroup
        Picasso.get().load(uri).into(image)

        val locale = Locale.getDefault()
        val date = SimpleDateFormat("hh:mm aa", locale).format(Date(timestamp))
        viewHolder.itemView.userFromTimeSent.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_group
    }
}

class ChatFromGroupImageItem(private val imageUri: String, private val member: GroupMember, private val timestamp: Long): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        val uri = member.flagUrl
        val image = viewHolder.itemView.userImageGroupMessageFrom
        Picasso.get().load(uri).into(image)

        val imageMessage = viewHolder.itemView.groupImageMessageFrom
        Picasso.get().load(imageUri).into(imageMessage)

        val locale = Locale.getDefault()
        val date = SimpleDateFormat("hh:mm aa", locale).format(Date(timestamp))
        viewHolder.itemView.imageMessageGroupFromTime.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_image_group
    }
}