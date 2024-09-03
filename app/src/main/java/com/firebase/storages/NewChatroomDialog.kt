package com.firebase.storages


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.firebase.storages.model.ChatMessage
import com.firebase.storages.model.Chatroom
import com.firebase.storages.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class NewChatroomDialog : DialogFragment() {

    private val TAG = "NewChatroomDialog"

    private lateinit var mSeekBar: SeekBar
    private lateinit var mChatroomName: EditText
    private lateinit var mCreateChatroom: TextView
    private lateinit var mSecurityLevel: TextView
    private var mUserSecurityLevel: Int = 0
    private var mSeekProgress: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_new_chatroom, container, false)
        mChatroomName = view.findViewById(R.id.input_chatroom_name)
        mSeekBar = view.findViewById(R.id.input_security_level)
        mCreateChatroom = view.findViewById(R.id.create_chatroom)
        mSecurityLevel = view.findViewById(R.id.security_level)

        mSecurityLevel.text = mSeekProgress.toString()
        getUserSecurityLevel()

        mCreateChatroom.setOnClickListener {
            if (mChatroomName.text.toString().isNotEmpty()) {
                Log.d(TAG, "onClick: creating new chat room")

                if (mUserSecurityLevel >= mSeekBar.progress) {

                    val reference = FirebaseDatabase.getInstance().reference
                    val chatroomId = reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .push().key

                    val chatroom = Chatroom(
                        chatroom_name = mChatroomName.text.toString(),
                        creator_id = FirebaseAuth.getInstance().currentUser?.uid,
                        security_level = mSeekBar.progress.toString(),
                        chatroom_id = chatroomId
                    )

                    reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(chatroomId!!)
                        .setValue(chatroom)

                    val messageId = reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .push().key

                    val message = ChatMessage(
                        message = "Welcome to the new chatroom!",
                        timestamp = getTimestamp()
                    )

                    reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(chatroomId)
                        .child(getString(R.string.field_chatroom_messages))
                        .child(messageId!!)
                        .setValue(message)

                    (activity as? ChatActivity)?.init()
                    dialog?.dismiss()
                } else {
                    Toast.makeText(activity, "Insufficient security level", Toast.LENGTH_SHORT).show()
                }
            }
        }

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mSeekProgress = progress
                mSecurityLevel.text = mSeekProgress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        return view
    }

    private fun getUserSecurityLevel() {
        val reference = FirebaseDatabase.getInstance().reference
        val query = reference.child(getString(R.string.dbnode_users))
            .orderByKey()
            .equalTo(FirebaseAuth.getInstance().currentUser?.uid)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(User::class.java)
                    Log.d(TAG, "onDataChange: users security level: ${user?.security_level}")
                    mUserSecurityLevel = user?.security_level?.toInt() ?: 0
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }
}
