package com.arrowhead.arrownet

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_settings_view.*
import java.util.*

class SettingsView : AppCompatActivity() {
    companion object {
        val IMAGE_REQUEST_CODE = 100
    }

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_view)
        database = Firebase.database.reference

        supportActionBar?.title = "Settings"

        photo_button.setOnClickListener {
            pickFromGallery()
        }

        save_button.setOnClickListener {
            uploadImageToFirebase()
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

                    updateInfo(it.toString())
                }
            }
    }

    private fun updateInfo(profileImageUrl: String) {
        val user = Firebase.auth.currentUser
        val uid = user.uid
        database.child("users").child(uid).child("name").setValue(NameEntry.text.toString())
        database.child("users").child(uid).child("profileImageUrl").setValue(profileImageUrl)
    }
}