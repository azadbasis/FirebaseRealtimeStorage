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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.firebase.storages.R

class ResendVerificationDialog : DialogFragment() {

    private val TAG = "ResendVerificationDialog"

    private lateinit var mConfirmPassword: EditText
    private lateinit var mConfirmEmail: EditText

    private lateinit var mContext: Context


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_resend_verification, container, false)
        mConfirmPassword = view.findViewById(R.id.confirm_password)
        mConfirmEmail = view.findViewById(R.id.confirm_email)
        mContext = requireActivity()

        val confirmDialog = view.findViewById<TextView>(R.id.dialogConfirm)
        confirmDialog.setOnClickListener {
            Log.d(TAG, "onClick: attempting to resend verification email.")

            val email = mConfirmEmail.text.toString()
            val password = mConfirmPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authenticateAndResendEmail(email, password)
            } else {
                Toast.makeText(mContext, "all fields must be filled out", Toast.LENGTH_SHORT).show()
            }
        }

        val cancelDialog = view.findViewById<TextView>(R.id.dialogCancel)
        cancelDialog.setOnClickListener {
            dialog?.dismiss()
        }

        return view
    }

    private fun authenticateAndResendEmail(email: String, password: String) {
        val credential: AuthCredential = EmailAuthProvider.getCredential(email, password)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "onComplete: reauthenticate success.")
                  //  sendVerificationEmail()
                    FirebaseAuth.getInstance().signOut()
                    dialog?.dismiss()
                }
            }.addOnFailureListener { e: Exception ->
                Toast.makeText(
                    mContext,
                    "Invalid Credentials. \nReset your password and try again",
                    Toast.LENGTH_SHORT
                ).show()
                dialog?.dismiss()
            }
    }

    fun sendVerificationEmail() {
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task: Task<Void> ->
            if (task.isSuccessful) {
                Toast.makeText(mContext, "Sent Verification Email", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(mContext, "couldn't send email", Toast.LENGTH_SHORT).show()
            }
        }
    }
}