package com.arrowhead.arrownet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_main.*
import java.sql.Time
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var storedVerificationId: String
    var number : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        ccp.registerCarrierNumberEditText(phonenumber_textview)
        sendCodeButton.setOnClickListener() {
            login()
        }

        resendCodeButton.setOnClickListener() {
            //dembois
        }
        codeConfirmButton.setOnClickListener() {
            authenticate()
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                startActivity(Intent(applicationContext, HomePage::class.java))
                finish()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                Toast.makeText(this@MainActivity, "Code Sent", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun login() {
        number = findViewById<EditText>(R.id.phonenumber_textview).text.trim().toString()
        var country = ccp.fullNumberWithPlus
        number = country
        if(number.isNotEmpty()) {
            sendVerificationCode(number)
            Toast.makeText(this, "Phone: $number", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show()

    }

    private fun signIn(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                task: Task<AuthResult> ->
                if(task.isSuccessful) {
                    Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomePage::class.java))
                }
            }
    }

    private fun authenticate() {
        val verifiNum = verificationCodeEntry.text.toString()
        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, verifiNum)
        signIn(credential)
    }

    private fun updateUI(user: FirebaseUser? = auth.currentUser) {
        if(user != null) {
            startActivity(Intent(applicationContext, HomePage::class.java))
            finish()
        }
    }
}