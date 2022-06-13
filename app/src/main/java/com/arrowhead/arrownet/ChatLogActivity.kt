package com.arrowhead.arrownet

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.arrowhead.arrownet.SettingsView.Companion.IMAGE_REQUEST_CODE
import com.arrowhead.arrownet.SettingsView.Companion.PERMISSION_CODE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification.getClient
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from.view.*
import kotlinx.android.synthetic.main.chat_from_image.view.*
import kotlinx.android.synthetic.main.chat_to.view.*
import kotlinx.android.synthetic.main.chat_to_image.view.*
import kotlinx.android.synthetic.main.chat_toolbar.*
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ChatLogActivity : AppCompatActivity() {
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null
    private var checkTranslatedMessage: Boolean = false
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
        setContentView(R.layout.activity_chat_log)

        var name = intent.getStringExtra(NewMessageActivity.NAME_KEY)
        if (name == null) {
            name = intent.getStringExtra(HomePage.NAME_KEY)
        }
        toUser = intent.getParcelableExtra(NewMessageActivity.USER_KEY)
        if (toUser == null) {
            toUser = intent.getParcelableExtra(HomePage.USER_KEY)
        }

        recyclerview_chat.adapter = adapter

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.chat_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title_name.text = name
        Picasso.get().load(toUser?.photoUrl).into(profile_image)
        Picasso.get().load(HomePage.currentUser?.flagUrl).into(translate_image)

        listenForMessages()

        translate_switch.setOnCheckedChangeListener { _, p1 ->
            checkTranslatedMessage = p1
            listenForTranslatedMessages(p1)
        }

        sendButton.setOnClickListener {
            if(newMessageText.text.isNotEmpty() || image_preview.isVisible) {
                sendMessage()
            }
            else {
                return@setOnClickListener
            }
        }

        image_message_button.setOnClickListener {
            onImageButtonPressed()
        }

        take_picture_button.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(Manifest.permission.CAMERA)
                    requestPermissions(permissions, PERMISSION_CODE)
                }
                else {
                    takePicture()
                }
            }
            else {
                takePicture()
            }
        }

        choose_image_button.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_CODE)
                }
                else {
                    pickFromGallery()
                }
            }
            else {
                pickFromGallery()
            }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            PERMISSION_CODE -> {
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
            image_preview.setImageBitmap(thumbnail)
            image_preview.visibility = View.VISIBLE
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == IMAGE_REQUEST_CODE) {
            if (data != null) {
                fileUri = data.data
            }
            image_preview.setImageURI(fileUri)
            image_preview.visibility = View.VISIBLE
        }
    }

    private fun translateText() {
        val fromID = FirebaseAuth.getInstance().uid
        val currentUserLanguageCode = HomePage.currentUser?.languageID!!
        val toID = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chatMessage = it.getValue(ChatMessage::class.java)
                    val key = it.key.toString()

                    if (chatMessage != null) {
                        if (chatMessage.type == "image" || chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                            return@forEach
                        } else {
                            val languageIdentifier = getClient()
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

    class ChatMessage(val id: String, val text: String, val fromID: String, val toID: String, var translatedMessage: String, val type: String, val imageUri: String, val timestamp: Long) {
        constructor() : this("", "", "", "", "", "", "", -1)
    }

    private fun sendMessage() {
        val text = newMessageText.text.toString()
        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser!!.uid

        if (fromID == null) {
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID").push()
        val toReference = FirebaseDatabase.getInstance().getReference("messages/$toID/$fromID").push()

        if(text.isNotEmpty() && image_preview.isVisible) {
            val chatMessage = ChatMessage(reference.key!!, text, fromID, toID, "", "text", "", System.currentTimeMillis())
            reference.setValue(chatMessage).addOnSuccessListener {
                newMessageText.text.clear()
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }
            toReference.setValue(chatMessage)

            val latestMessageRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromID/$toID")
            latestMessageRef.setValue(chatMessage)

            val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toID/$fromID")
            latestMessageToRef.setValue(chatMessage)

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
                        val chatMessage = ChatMessage(reference.key!!, "", fromID, toID, "", "image", it.toString(), System.currentTimeMillis())
                        reference.setValue(chatMessage).addOnSuccessListener {
                            image_preview.visibility = View.GONE
                            recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                        toReference.setValue(chatMessage)
                        latestMessageRef.setValue(chatMessage)
                        latestMessageToRef.setValue(chatMessage)
                    }
                }
        }
        else if(text.isNotEmpty()) {
            val chatMessage = ChatMessage(reference.key!!, text, fromID, toID, "", "text", "", System.currentTimeMillis())
            reference.setValue(chatMessage).addOnSuccessListener {
                newMessageText.text.clear()
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }
            toReference.setValue(chatMessage)

            val latestMessageRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromID/$toID")
            latestMessageRef.setValue(chatMessage)

            val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toID/$fromID")
            latestMessageToRef.setValue(chatMessage)
        }
        else if(image_preview.isVisible) {
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
                        val chatMessage = ChatMessage(reference.key!!, "", fromID, toID, "", "image", it.toString(), System.currentTimeMillis())
                        reference.setValue(chatMessage).addOnSuccessListener {
                            image_preview.visibility = View.GONE
                            recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                        toReference.setValue(chatMessage)

                        val latestMessageRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromID/$toID")
                        latestMessageRef.setValue(chatMessage)

                        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toID/$fromID")
                        latestMessageToRef.setValue(chatMessage)
                    }
                }
        }
    }

    private fun listenForMessages() {
        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

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
                                adapter.add(ChatFromItem(chatMessage.text, toUser!!, chatMessage.timestamp))
                            }
                            else {
                                adapter.add(ChatFromImageItem(chatMessage.imageUri, toUser!!, chatMessage.timestamp))
                            }
                        }
                    }
                }
                recyclerview_chat.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val changedChatMessage = snapshot.getValue(ChatMessage::class.java)

                if(changedChatMessage != null) {
                    if (checkTranslatedMessage) {
                        adapter.add(ChatFromItem(changedChatMessage.translatedMessage, toUser!!, changedChatMessage.timestamp))
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // nada
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // nada
            }

            override fun onCancelled(error: DatabaseError) {
                // nada
            }

        })
    }

    private fun listenForTranslatedMessages(checkTranslate: Boolean) {
        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.clear()
                snapshot.children.forEach {
                    val chatMessage = it.getValue(ChatMessage::class.java)

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
                                    adapter.add(ChatFromItem(chatMessage.translatedMessage, toUser!!, chatMessage.timestamp))
                                }
                                else {
                                    adapter.add(ChatFromImageItem(chatMessage.imageUri, toUser!!, chatMessage.timestamp))
                                }
                            } else {
                                if(chatMessage.type == "text") {
                                    adapter.add(ChatFromItem(chatMessage.text, toUser!!, chatMessage.timestamp))
                                }
                                else {
                                    adapter.add(ChatFromImageItem(chatMessage.imageUri, toUser!!, chatMessage.timestamp))
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
}

class ChatFromItem(val text: String, private val user: User, val timestamp: Long): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userFromText.text = text

        val uri = user.flagUrl
        val image = viewHolder.itemView.userFromImage
        Picasso.get().load(uri).into(image)

        val locale = Locale.getDefault()
        val date = SimpleDateFormat("hh:mm aa", locale).format(Date(timestamp))
        viewHolder.itemView.userFromTimeStamp.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_from
    }
}

class ChatToItem(val text: String, private val timestamp: Long): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userToText.text = text

        val locale = Locale.getDefault()
        val date = SimpleDateFormat("hh:mm aa", locale).format(Date(timestamp))
        viewHolder.itemView.userToTimeStamp.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_to
    }
}

class ChatToImageItem(private val imageUri: String, val timestamp: Long): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        val image = viewHolder.itemView.chatMessageImage
        Picasso.get().load(imageUri).into(image)

        val locale = Locale.getDefault()
        val date = SimpleDateFormat("hh:mm aa", locale).format(Date(timestamp))
        viewHolder.itemView.chatMessageImageTimeStamp.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_image
    }
}

class ChatFromImageItem(private val imageUri: String, private val user: User, val timestamp: Long): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        val uri = user.flagUrl
        val image = viewHolder.itemView.userImageMessageFrom
        Picasso.get().load(uri).into(image)

        val imageMessage = viewHolder.itemView.imageMessageFrom
        Picasso.get().load(imageUri).into(imageMessage)

        val locale = Locale.getDefault()
        val date = SimpleDateFormat("hh:mm aa", locale).format(Date(timestamp))
        viewHolder.itemView.imageMessageFromTimeStamp.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_image
    }
}