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
    lateinit var auth: FirebaseAuth
    lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    lateinit var storedVerificationId: String
    lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        ccp.registerCarrierNumberEditText(phonenumber_textview)

        var sendButton = findViewById<Button>(R.id.sendCodeButton)

        var currentUser = auth.currentUser
        if(currentUser != null) {
            startActivity(Intent(applicationContext, HomePage::class.java))
            finish()
        }

        sendButton.setOnClickListener() {
            sendVerificationCode()
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                startActivity(Intent(applicationContext, SettingsView::class.java))
                finish()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(applicationContext, "Failed", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                Toast.makeText(applicationContext, "Code Sent...", Toast.LENGTH_SHORT).show()
                var intent = Intent(applicationContext, VerifyActivity::class.java)
                intent.putExtra("PhoneNumber", phoneNumber)
                intent.putExtra("storedVerificationId", storedVerificationId)
                startActivity(intent)
            }
        }
    }

    private fun sendVerificationCode() {
        val number = findViewById<EditText>(R.id.phonenumber_textview)
        phoneNumber = number.text.toString().trim()

        if(phoneNumber.isNotEmpty()) {
            phoneNumber = ccp.fullNumberWithPlus
            Toast.makeText(this, "$phoneNumber", Toast.LENGTH_SHORT).show()
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