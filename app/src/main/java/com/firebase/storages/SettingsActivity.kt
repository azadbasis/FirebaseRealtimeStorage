package com.firebase.storages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.firebase.storages.R


class SettingsActivity : AppCompatActivity() {

    private val TAG = "SettingsActivity"
//    private val DOMAIN_NAME = "tabian.ca"
    private val DOMAIN_NAME = "gmail.com"

    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private lateinit var mEmail: EditText
    private lateinit var mCurrentPassword: EditText
    private lateinit var mSave: Button
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mResetPasswordLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        Log.d(TAG, "onCreate: started.")

        mEmail = findViewById(R.id.input_email)
        mCurrentPassword = findViewById(R.id.input_password)
        mSave = findViewById(R.id.btn_save)
        mProgressBar = findViewById(R.id.progressBar)
        mResetPasswordLink = findViewById(R.id.change_password)

        setupFirebaseAuth()
        setCurrentEmail()

        mSave.setOnClickListener {
            Log.d(TAG, "onClick: attempting to save settings.")

            val email = mEmail.text.toString()
            val password = mCurrentPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (FirebaseAuth.getInstance().currentUser?.email != email) {
                    if (isValidDomain(email)) {
                        editUserEmail()
                    } else {
                        Toast.makeText(this, "Invalid Domain", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "no changes were made", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Email and Current Password Fields Must be Filled to Save",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mResetPasswordLink.setOnClickListener {
            Log.d(TAG, "onClick: sending password reset link")
            sendResetPasswordLink()
        }
    }

    private fun sendResetPasswordLink() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        email?.let {
            FirebaseAuth.getInstance().sendPasswordResetEmail(it).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "onComplete: Password Reset Email sent.")
                        Toast.makeText(
                            this, "Sent Password Reset Link to Email", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.d(TAG, "onComplete: No user associated with that email.")
                        Toast.makeText(
                            this, "No User Associated with that Email.", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun editUserEmail() {
        showDialog()
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val credential: AuthCredential =
            EmailAuthProvider.getCredential(currentUser.email!!, mCurrentPassword.text.toString())
        Log.d(
            TAG,
            "editUserEmail: reauthenticating with:  \n email ${currentUser.email}" + " \n password: ${mCurrentPassword.text}"
        )

        currentUser.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "onComplete: reauthenticate success.")
                    val newEmail = mEmail.text.toString()
                    if (isValidDomain(newEmail)) {
                        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(newEmail)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val signInMethods =
                                        task.result?.signInMethods ?: emptyList<String>()
                                    if (signInMethods.isNotEmpty()) {
                                        Log.d(TAG, "onComplete: That email is already in use.")
                                        hideDialog()
                                        Toast.makeText(
                                            this, "That email is already in use", Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val currentUser = FirebaseAuth.getInstance().currentUser

                                        if (currentUser != null) {
                                            currentUser.verifyBeforeUpdateEmail(newEmail)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        Log.d(TAG, "onComplete: Verification email sent.")
                                                        Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                                                        FirebaseAuth.getInstance().signOut();
                                                    } else {
                                                        Log.w(TAG, "onComplete: Failed to send verification email.", task.exception)
                                                        Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
                                                        hideDialog() // Assuming you have a progress dialog
                                                    }
                                                }
                                        } else {
                                            Log.w(TAG, "updateUserEmail: No user is signed in")
                                            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
                                            hideDialog()
                                        }
                                    }
                                }
                            }.addOnFailureListener {
                                hideDialog()
                                Toast.makeText(
                                    this, "Unable to update email", Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(this, "You must use a company email", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.d(TAG, "onComplete: Incorrect Password")
                    Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    hideDialog()
                }
            }.addOnFailureListener {
                hideDialog()
                Toast.makeText(this, "Unable to update email", Toast.LENGTH_SHORT).show()
            }
    }


    fun sendVerificationEmail() {
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sent Verification Email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Couldn't Send Verification Email", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun setCurrentEmail() {
        Log.d(TAG, "setCurrentEmail: setting current email to EditText field")
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            Log.d(TAG, "setCurrentEmail: user is NOT null.")
            val email = it.email
            Log.d(TAG, "setCurrentEmail: got the email: $email")
            mEmail.setText(email)
        }
    }

    private fun isValidDomain(email: String): Boolean {
        Log.d(TAG, "isValidDomain: verifying email has correct domain: $email")
        val domain = email.substring(email.indexOf("@") + 1).toLowerCase()
        Log.d(TAG, "isValidDomain: users domain: $domain")
        return domain == DOMAIN_NAME
    }

    private fun showDialog() {
        mProgressBar.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        if (mProgressBar.visibility == View.VISIBLE) {
            mProgressBar.visibility = View.INVISIBLE
        }
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
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
            } else {
                Log.d(TAG, "onAuthStateChanged:signed_out")
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
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