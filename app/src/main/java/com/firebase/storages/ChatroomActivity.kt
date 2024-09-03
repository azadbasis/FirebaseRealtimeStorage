package com.firebase.storages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.firebase.storages.model.ChatMessage
import com.firebase.storages.model.Chatroom
import com.firebase.storages.model.User
import com.firebase.storages.utility.ChatMessageListAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatroomActivity : AppCompatActivity() {

    private val TAG = "ChatroomActivity"

    // Firebase
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private lateinit var mMessagesReference: DatabaseReference

    // Widgets
    private lateinit var mChatroomName: TextView
    private lateinit var mListView: ListView
    private lateinit var mMessage: EditText
    private lateinit var mCheckmark: ImageView

    // Vars
    private lateinit var mChatroom: Chatroom
    private lateinit var mMessagesList: MutableList<ChatMessage>
    private lateinit var mAdapter: ChatMessageListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)
        mChatroomName = findViewById(R.id.text_chatroom_name)
        mListView = findViewById(R.id.listView)
        mMessage = findViewById(R.id.input_message)
        mCheckmark = findViewById(R.id.checkmark)
        supportActionBar?.hide()
        Log.d(TAG, "onCreate: started.")
        mMessagesList = mutableListOf()
        setupAdapter()
        setupFirebaseAuth()
        getChatroom()
        init()
        hideSoftKeyboard()
    }

    private fun setupAdapter() {
        mAdapter = ChatMessageListAdapter(this, R.layout.layout_chatmessage_listitem, mMessagesList)
        mListView.adapter = mAdapter
    }

    private fun init() {
        mMessage.setOnClickListener {
            mListView.setSelection(mAdapter.count - 1) // Scroll to the bottom of the list
        }

        mCheckmark.setOnClickListener {
            val message = mMessage.text.toString()
            if (message.isNotEmpty()) {
                Log.d(TAG, "onClick: sending new message: $message")

                // Create the new message object for inserting
                val newMessage = ChatMessage().apply {
                    this.message = message
                    this.timestamp = getTimestamp()
                    this.user_id = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                }

                // Get a database reference
                val reference = FirebaseDatabase.getInstance().getReference()
                    .child(getString(R.string.dbnode_chatrooms))
                    .child(mChatroom.chatroom_id.toString())
                    .child(getString(R.string.field_chatroom_messages))

                // Create the new message's id
                val newMessageId = reference.push().key

                // Insert the new message into the chatroom
                newMessageId?.let {
                    reference.child(it).setValue(newMessage)
                }

                // Clear the EditText
                mMessage.setText("")
            }
        }
    }

    /**
     * Retrieve the chatroom name using a query
     */
    private fun getChatroom() {
        Log.d(TAG, "getChatroom: getting selected chatroom details")

        intent?.let {
            if (it.hasExtra(getString(R.string.intent_chatroom))) {
                mChatroom = it.getParcelableExtra(getString(R.string.intent_chatroom))!!
                Log.d(TAG, "getChatroom: chatroom: $mChatroom")
                mChatroomName.text = mChatroom.chatroom_name

                enableChatroomListener()
            }
        }
    }

    private fun getChatroomMessages() {
        mMessagesList.clear()
        mAdapter.clear()

        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_chatrooms))
            .child(mChatroom.chatroom_id.toString())
            .child(getString(R.string.field_chatroom_messages))

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    Log.d(TAG, "onDataChange: found chatroom message: ${singleSnapshot.value}")
                    try {
                        val message = singleSnapshot.getValue(ChatMessage::class.java)
                        message?.let {
                            mMessagesList.add(it)
                        }
                    } catch (e: NullPointerException) {
                        Log.e(TAG, "onDataChange: NullPointerException: ${e.message}")
                    }
                }
                // Query the users node to get the profile images and names
                getUserDetails()
                initMessagesList()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun getUserDetails() {
        val reference = FirebaseDatabase.getInstance().reference
        mMessagesList.forEachIndexed { i, message ->
            Log.d(TAG, "onDataChange: searching for userId: ${message.user_id}")
            message.user_id?.let { userId ->
                val query = reference.child(getString(R.string.dbnode_users))
                    .orderByKey()
                    .equalTo(userId)
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val singleSnapshot = dataSnapshot.children.firstOrNull()
                        singleSnapshot?.let {
                            Log.d(TAG, "onDataChange: found user id: ${it.getValue(User::class.java)?.user_id}")
                            mMessagesList[i].apply {
                                profile_image = it.getValue(User::class.java)?.profile_image
                                name = it.getValue(User::class.java)?.name
                            }
                            mAdapter.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
            }
        }
    }

    private fun initMessagesList() {
        mAdapter = ChatMessageListAdapter(this, R.layout.layout_chatmessage_listitem, mMessagesList)
        mListView.adapter = mAdapter
        mListView.setSelection(mAdapter.count - 1) // Scroll to the bottom of the list
    }

    /**
     * Return the current timestamp in the form of a string
     */
    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }

    private fun hideSoftKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    /*
        ----------------------------- Firebase setup ---------------------------------
    */

    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.")

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.")

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated.")
        }
    }

    private fun setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started.")

        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:${user.uid}")
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
                Toast.makeText(this@ChatroomActivity, "Signed out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ChatroomActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mMessagesReference.removeEventListener(mValueEventListener)
    }

    private val mValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            getChatroomMessages()
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Handle error
        }
    }

    private fun enableChatroomListener() {
        /*
           ---------- Listener that will watch the 'chatroom_messages' node ----------
        */
        mMessagesReference = FirebaseDatabase.getInstance().getReference()
            .child(getString(R.string.dbnode_chatrooms))
            .child(mChatroom.chatroom_id.toString())
            .child(getString(R.string.field_chatroom_messages))

        mMessagesReference.addValueEventListener(mValueEventListener)
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener)
    }
}
