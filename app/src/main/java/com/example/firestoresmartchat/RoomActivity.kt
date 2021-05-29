package com.example.firestoresmartchat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.codingwithme.firebasechat.model.NotificationData
import com.codingwithme.firebasechat.model.PushNotification
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RoomActivity : AppCompatActivity() {


    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val chatMessages = ArrayList<ChatMessage>()
    var chatRegistration: ListenerRegistration? = null
    var roomId: String? = null
    var topic = ""
    @SuppressLint("StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
     //   checkUser()
        initList()
        setViewListeners()

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = getString(R.string.app_name, token)
            Log.d("FCM", msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })

        Firebase.messaging.subscribeToTopic("weather")
            .addOnCompleteListener { task ->
                var msg = getString(R.string.msg_subscribed)
                if (!task.isSuccessful) {
                    msg = getString(R.string.msg_subscribe_failed)
                }
                Log.d("FCM", msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }



    private fun setViewListeners() {
        button_send.setOnClickListener {
            sendChatMessage()

        }
    }
    private fun initList() {
        if (user == null)
            return
        list_chat.layoutManager = LinearLayoutManager(this)
        val adapter = ChatAdapter(chatMessages, user.uid)
        list_chat.adapter = adapter
        listenForChatMessages()
    }
    private fun listenForChatMessages() {
        roomId = intent.getStringExtra("INTENT_EXTRA_ROOMID")
        if (roomId == null) {
            finish()
            return
        }

        chatRegistration = firestore.collection("rooms")
            .document(roomId!!)
            .collection("messages")
            .addSnapshotListener { messageSnapshot, exception ->
                if (messageSnapshot == null || messageSnapshot.isEmpty)
                    return@addSnapshotListener
                chatMessages.clear()
                for (messageDocument in messageSnapshot.documents) {

                    chatMessages.add(
                        ChatMessage(
                            messageDocument["text"] as String,
                            messageDocument["user"] as String,
                            ( messageDocument["timestamp"] as Timestamp?)?.toDate()

                        ))
                    Log.e("Timestamp",
                        (messageDocument["timestamp"] as Timestamp?)?.toDate().toString()
                    )
                }
                chatMessages.sortBy { it.timestamp }
                list_chat.adapter?.notifyDataSetChanged()
            }
    }
    private fun sendChatMessage() {
        val message = edittext_chat.text.toString()
        edittext_chat.setText("")
        firestore.collection("rooms").document(roomId!!).collection("messages")
            .add(mapOf(
                Pair("text", message),
                Pair("user", user?.uid),
                Pair("timestamp", Timestamp.now())
            ))
        topic = "/topics/${auth.currentUser!!.uid}"
        PushNotification(
            NotificationData( roomId!!,message),
            topic).also {
            sendNotification(it)
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d("TAG", "Response: ${Gson().toJson(response)}")
            } else {
                Log.e("TAG", response.errorBody()!!.string())
            }
        } catch(e: Exception) {
            Log.e("TAG", e.toString())
        }
    }
    override fun onDestroy() {
        chatRegistration?.remove()
        super.onDestroy()
    }
}
