package com.firebase.storages

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.firebase.storages.R

class PasswordResetDialog : DialogFragment() {

    private val TAG = "PasswordResetDialog"

    // Widgets
    private lateinit var mEmail: EditText

    // Vars
    private lateinit var mContext: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_resetpassword, container, false)
        mEmail = view.findViewById(R.id.email_password_reset)
        mContext = requireActivity()

        val confirmDialog = view.findViewById<TextView>(R.id.dialogConfirm)
        confirmDialog.setOnClickListener {
            if (!isEmpty(mEmail.text.toString())) {
                Log.d(TAG, "onClick: attempting to send reset link to: ${mEmail.text.toString()}")
                sendPasswordResetEmail(mEmail.text.toString())
                dialog?.dismiss()
            }
        }

        return view
    }

    /**
     * Send a password reset link to the email provided
     * @param email
     */
    private fun sendPasswordResetEmail(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task: Task<Void?> ->
                if (task.isSuccessful) {
                    Log.d(TAG, "onComplete: Password Reset Email sent.")
                    Toast.makeText(mContext, "Password Reset Link Sent to Email", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "onComplete: No user associated with that email.")
                    Toast.makeText(mContext, "No User is Associated with that Email", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private fun isEmpty(string: String): Boolean {
        return string.isEmpty()
    }
}
