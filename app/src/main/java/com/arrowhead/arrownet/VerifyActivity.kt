package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class VerifyActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth

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
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this , MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this,"Invalid Code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}