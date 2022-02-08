package com.arrowhead.arrownet

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.parcel.Parcelize

class VerifyActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        auth = FirebaseAuth.getInstance()

        val storedVerificationId = intent.getStringExtra("storedVerificationId")

        val verify = findViewById<Button>(R.id.codeConfirmButton)
        val codeGiven = findViewById<EditText>(R.id.verificationCodeEntry)

        verify.setOnClickListener {
            var code = codeGiven.text.toString().trim()
            if(!code.isEmpty()) {
                val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(
                    storedVerificationId.toString(), code)
                signInWithPhoneAuthCredential(credential)
            }
            else {
                Toast.makeText(this, "Enter a code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val phoneNumber = intent.getStringExtra("PhoneNumber")
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this , MainActivity::class.java)
                    if (phoneNumber != null) {
                        newUser(phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this,"Invalid Code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun newUser(phoneNumber: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val uri = Uri.parse("android.resource://com.arrowhead.arrownet/" + R.drawable.blank_profile).toString()
        val flagUri = Uri.parse("android.resource://com.arrowhead.arrownet/" + R.drawable.blank_profile).toString()
        val user = User(uid, phoneNumber, "", uri, "", flagUri, "0")
        ref.setValue(user)
    }
}

@Parcelize
class User(val uid: String, val phoneNumber: String, val userName: String, val photoUrl: String, val languageID: String, val flagUrl: String, val spot: String): Parcelable {
    constructor() : this("", "", "", "", "", "", "")
}