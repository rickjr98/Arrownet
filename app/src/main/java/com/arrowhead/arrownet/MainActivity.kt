package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var storedVerificationId: String
    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        ccp.registerCarrierNumberEditText(phonenumber_textview)

        val sendButton = findViewById<Button>(R.id.sendCodeButton)

        sendButton.setOnClickListener() {
            sendVerificationCode()
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                startActivity(Intent(applicationContext, HomePage::class.java))
                finish()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(applicationContext, "Failed", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                Toast.makeText(applicationContext, "Code Sent...", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, VerifyActivity::class.java)
                intent.putExtra("PhoneNumber", phoneNumber)
                intent.putExtra("storedVerificationId", storedVerificationId)
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null) {
            startActivity(Intent(applicationContext, HomePage::class.java))
            finish()
        }
    }

    private fun sendVerificationCode() {
        val number = findViewById<EditText>(R.id.phonenumber_textview)
        phoneNumber = number.text.toString().trim()

        if(phoneNumber.isNotEmpty()) {
            phoneNumber = ccp.fullNumberWithPlus
            Toast.makeText(this, phoneNumber, Toast.LENGTH_SHORT).show()
            startPhoneNumberVerification(phoneNumber)
        }
        else {
            Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}