package com.arrowhead.arrownet

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.iceteck.silicompressorr.SiliCompressor
import java.io.File

class SendMedia : Service() {
    private var images: ArrayList<String>? = null
    private var fromID: String? = null
    private var toID: String? = null
    private var MAX_PROGRESS = 0

    override fun onBind(p0: Intent?): IBinder? {
        TODO("NOT YET IMPLEMENTED")
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fromID = intent!!.getStringExtra("fromID")
        toID = intent.getStringExtra("toID")
        images = intent.getStringArrayListExtra("images")
        MAX_PROGRESS = images!!.size

        for(a in images!!.indices) {
            val fileName = compressImage(images!![a])
            uploadImage(fileName!!)
        }

        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun uploadImage(fileName: String) {
        val storageRef = FirebaseStorage.getInstance().getReference("image/")
        val uri = Uri.fromFile(File(fileName))

        storageRef.putFile(uri).addOnSuccessListener {
            val task = it.storage.downloadUrl
            task.addOnCompleteListener {
                if(it.isSuccessful) {
                    val path = it.result.toString()
                    val databaseRef = FirebaseDatabase.getInstance().getReference("messages/$fromID/$toID")
                    val databaseRefTo = FirebaseDatabase.getInstance().getReference("messages/$toID/$fromID")

                    val message = ChatLogActivity.ChatMessage(databaseRef.key!!, "", fromID!!, toID!!, "", "image", path, System.currentTimeMillis() / 1000)
                    databaseRef.push().setValue(message)
                    databaseRefTo.push().setValue(message)
                }
            }
        }
    }

    private fun compressImage(fileName: String): String? {
        val file = File(Environment.getExternalStorageDirectory().absoluteFile, "/image/")
        if(!file.exists()) {
            file.mkdirs()
        }
        return SiliCompressor.with(this).compress(fileName, file, false)
    }
}
