package com.arrowhead.arrownet

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_settings_view.*
import java.util.*

class SettingsView : AppCompatActivity() {
    companion object {
        val IMAGE_REQUEST_CODE = 100
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference
    private lateinit var userName: String
    private lateinit var user: User
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_view)

        supportActionBar?.title = "Settings"

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid)

        getInfo()

        photo_button.setOnClickListener {
            pickFromGallery()
        }

        save_button.setOnClickListener {
            uploadImageToFirebase()
            uploadName()
            Toast.makeText(applicationContext, "User Profile Updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    var selectedPhotoUri: Uri? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
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
                    mDatabase.child("photoUrl").setValue(it.toString())
                }
            }
    }

    private fun uploadName() {
        userName = NameEntry.text.toString()
        mDatabase.child("userName").setValue(userName)
    }

    private fun getInfo() {
        mDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(User::class.java)!!
                NameEntry.setText(user.userName)
                val uri = user.photoUrl
                Picasso.with(applicationContext).load(uri).into(photo_button)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to update", Toast.LENGTH_SHORT).show()
            }

        })
    }
}