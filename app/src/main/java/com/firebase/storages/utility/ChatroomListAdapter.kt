package com.firebase.storages.utility

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import com.firebase.storages.ChatActivity
import com.firebase.storages.R
import com.firebase.storages.model.Chatroom
import com.firebase.storages.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nostra13.universalimageloader.core.ImageLoader

class ChatroomListAdapter(
    context: Context,
    @LayoutRes private val mLayoutResource: Int,
    objects: List<Chatroom>
) : ArrayAdapter<Chatroom>(context, mLayoutResource, objects) {

    private val TAG = "ChatroomListAdapter"
    private val mContext: Context = context
    private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    class ViewHolder {
        var name: TextView? = null
        var creatorName: TextView? = null
        var numberMessages: TextView? = null
        var mProfileImage: ImageView? = null
        var mTrash: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var view = convertView

        if (view == null) {
            view = mInflater.inflate(mLayoutResource, parent, false)
            holder = ViewHolder()

            holder.name = view.findViewById(R.id.name)
            holder.creatorName = view.findViewById(R.id.creator_name)
            holder.numberMessages = view.findViewById(R.id.number_chatmessages)
            holder.mProfileImage = view.findViewById(R.id.profile_image)
            holder.mTrash = view.findViewById(R.id.icon_trash)

            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        try {
            // Set the chatroom name
            val chatroom = getItem(position)
            holder.name?.text = chatroom?.chatroom_name

            // Set the number of chat messages
            val chatMessagesString = "${chatroom?.chatroom_messages?.size ?: 0} messages"
            holder.numberMessages?.text = chatMessagesString

            // Get the user's details who created the chatroom
            val reference = FirebaseDatabase.getInstance().reference
            val query = reference.child(mContext.getString(R.string.dbnode_users))
                .orderByKey()
                .equalTo(chatroom?.creator_id)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (singleSnapshot in dataSnapshot.children) {
                        Log.d(TAG, "onDataChange: Found chat room creator: " + singleSnapshot.getValue(
                            User::class.java).toString())
                        val user = singleSnapshot.getValue(User::class.java)
                        val createdBy = "created by ${user?.name}"
                        holder.creatorName?.text = createdBy
                        ImageLoader.getInstance().displayImage(user?.profile_image, holder.mProfileImage)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors
                }
            })

            holder.mTrash?.setOnClickListener {
                if (chatroom?.creator_id == FirebaseAuth.getInstance().currentUser?.uid) {
                    Log.d(TAG, "onClick: asking for permission to delete icon.")
                    (mContext as? ChatActivity)?.showDeleteChatroomDialog(chatroom?.chatroom_id ?: "")
                } else {
                    Toast.makeText(mContext, "You didn't create this chatroom", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: NullPointerException) {
            Log.e(TAG, "getView: NullPointerException: ", e)
        }

        return view!!
    }
}
