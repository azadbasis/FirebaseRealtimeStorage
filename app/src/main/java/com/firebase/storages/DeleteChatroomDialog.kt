package com.firebase.storages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DeleteChatroomDialog : DialogFragment() {

    private val TAG = "DeleteChatroomDialog"

    // Create a new bundle and set the arguments to avoid a null pointer
    init {
        arguments = Bundle()
    }

    private var mChatroomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: started")
        mChatroomId = arguments?.getString(getString(R.string.field_chatroom_id))
        if (mChatroomId != null) {
            Log.d(TAG, "onCreate: got the chatroom id: $mChatroomId")
        }
    }

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_delete_chatroom, container, false)

        val delete = view.findViewById<TextView>(R.id.confirm_delete)
        delete.setOnClickListener {
            if (mChatroomId != null) {
                Log.d(TAG, "onClick: deleting chatroom: $mChatroomId")

                val reference: DatabaseReference = FirebaseDatabase.getInstance().reference
                reference.child(getString(R.string.dbnode_chatrooms))
                    .child(mChatroomId!!)
                    .removeValue()
                dialog?.dismiss()
                (activity as? ChatActivity)?.init()
            }
        }

        val cancel = view.findViewById<TextView>(R.id.cancel)
        cancel.setOnClickListener {
            Log.d(TAG, "onClick: canceling deletion of chatroom")
            dialog?.dismiss()
        }

        return view
    }
}
