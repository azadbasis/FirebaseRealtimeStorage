package com.firebase.storages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.firebase.storages.model.ChatMessage
import com.firebase.storages.model.Chatroom
import com.firebase.storages.utility.ChatroomListAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"

    // Widgets
    private lateinit var mListView: ListView
    private lateinit var mFob: FloatingActionButton

    // Vars
    private lateinit var mChatrooms: ArrayList<Chatroom>
    private lateinit var mAdapter: ChatroomListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        mListView = findViewById(R.id.listView)
        mFob = findViewById(R.id.fob)

        init()
    }

     fun init() {
        getChatrooms()
        mFob.setOnClickListener {
            val dialog = NewChatroomDialog()
            dialog.show(supportFragmentManager, getString(R.string.dialog_new_chatroom))

        }
    }

    private fun setupChatroomList() {
        Log.d(TAG, "setupChatroomList: setting up chatroom listview")
        mAdapter = ChatroomListAdapter(this@ChatActivity, R.layout.layout_chatroom_listitem, mChatrooms)
        mListView.adapter = mAdapter
        mListView.setOnItemClickListener { _, _, i, _ ->
            Log.d(TAG, "onItemClick: selected chatroom: " + mChatrooms[i].toString())
            val intent = Intent(this@ChatActivity, ChatroomActivity::class.java)
            intent.putExtra(getString(R.string.intent_chatroom), mChatrooms[i])
            startActivity(intent)
        }
    }

    private fun getChatrooms() {
        Log.d(TAG, "getChatrooms: retrieving chatrooms from firebase database.")
        mChatrooms = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference

        val query = reference.child(getString(R.string.dbnode_chatrooms))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    Log.d(TAG, "onDataChange: found chatroom: " + singleSnapshot.value)

                    val objectMap = singleSnapshot.value as HashMap<String, Any>
                    val chatroom = Chatroom(
                        chatroom_id = objectMap[getString(R.string.field_chatroom_id)].toString(),
                        chatroom_name = objectMap[getString(R.string.field_chatroom_name)].toString(),
                        creator_id = objectMap[getString(R.string.field_creator_id)].toString(),
                        security_level = objectMap[getString(R.string.field_security_level)].toString()
                    )

                    // Get the chatrooms messages
                    val messagesList = ArrayList<ChatMessage>()
                    for (snapshot in singleSnapshot.child(getString(R.string.field_chatroom_messages)).children) {
                        val message = snapshot.getValue(ChatMessage::class.java)!!
                        messagesList.add(message)
                    }
                    chatroom.chatroom_messages = messagesList
                    mChatrooms.add(chatroom)
                }
                setupChatroomList()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "onCancelled: query cancelled.${databaseError.message}")
            }
        })
    }

    fun showDeleteChatroomDialog(chatroomId: String) {
        val dialog = DeleteChatroomDialog()
        val args = Bundle()
        args.putString(getString(R.string.field_chatroom_id), chatroomId)
        dialog.arguments = args
        dialog.show(supportFragmentManager, getString(R.string.dialog_delete_chatroom))
    }

    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.")

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.")
            val intent = Intent(this@ChatActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated.")
        }
    }
}
