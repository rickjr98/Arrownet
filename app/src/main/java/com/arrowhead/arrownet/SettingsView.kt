package com.arrowhead.arrownet

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_settings_view.*
import kotlinx.android.synthetic.main.language_item.view.*
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
    private lateinit var languageID: String
    private lateinit var flagUrl: String
    private lateinit var spot: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_view)

        supportActionBar?.title = "Settings"

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid)

        getInfo()

        setUpLanguageSelect()

        photo_button.setOnClickListener {
            pickFromGallery()
        }

        save_button.setOnClickListener {
            uploadImageToFirebase()
            uploadName()
            mDatabase.child("languageID").setValue(languageID)
            mDatabase.child("flagUrl").setValue(flagUrl)
            mDatabase.child("spot").setValue(spot)
            finish()
            Toast.makeText(applicationContext, "User Profile Updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpLanguageSelect() {
        val adapter = LanguageAdapter(this, Languages.list!!)
        language_selection.adapter = adapter

        language_selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedItem = adapter.getItem(p2)
                spot = p2.toString()
                languageID = selectedItem?.languageID.toString()
                flagUrl = Uri.parse("android.resource://com.arrowhead.arrownet/" + selectedItem?.image).toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // Nada
            }
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
        mDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(User::class.java)!!
                NameEntry.setText(user.userName)
                val uri = user.photoUrl
                Picasso.get().load(uri).into(photo_button)
                spot = user.spot
                language_selection.setSelection(spot.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to update", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

class LanguageAdapter(context: Context, languageList: List<Language>) : ArrayAdapter<Language>(context, 0, languageList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        val language = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.language_item, parent, false)
        view.flag_image.setImageResource(language!!.image)
        view.languageSelection.text = language.name

        return view
    }
}