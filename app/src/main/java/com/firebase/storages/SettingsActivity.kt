package com.firebase.storages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.storages.databinding.ActivitySettingsBinding
import com.firebase.storages.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener


class SettingsActivity : AppCompatActivity() {

    private val TAG = "SettingsActivity"

    //    private val DOMAIN_NAME = "tabian.ca"
    private val DOMAIN_NAME = "gmail.com"

    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener

   private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebaseAuth()
        setCurrentEmail()
        setupListener()

        hideSoftKeyboard()
    }

    private fun getUserAccountsData() {
        Log.d(TAG, "getUserAccountsData: getting the users account information")

        val reference = FirebaseDatabase.getInstance().reference

        /*
            --------- Query method 1 ----------
         */
        val query1: Query = reference.child(getString(R.string.dbnode_users))
            .orderByKey()
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)

        query1.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(
                        User::class.java
                    )
                    Log.d(TAG, "onDataChange: (QUERY METHOD 1) found user: " + user.toString())

                    binding.inputName.setText(user!!.name)
                    binding.inputPhone.setText(user!!.phone)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "onCancelled: "+databaseError.message)
            }
        })

        /*
            --------- Query method 2 ----------
         */
        val query2: Query = reference.child(getString(R.string.dbnode_users))
            .orderByKey()
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)

        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(
                        User::class.java
                    )
                    Log.d(TAG, "onDataChange: (QUERY METHOD 2) found user: " + user.toString())

                    binding.inputName.setText(user!!.name)
                    binding.inputPhone.setText(user!!.phone)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

        binding.inputEmail.setText(FirebaseAuth.getInstance().currentUser!!.email)
    }
    private fun setupListener() {
        getUserAccountsData()
        binding.btnSave.setOnClickListener {
            Log.d(TAG, "onClick: attempting to save settings.")

            // Check if they changed the email
            if (binding.inputEmail.text.toString() != FirebaseAuth.getInstance().currentUser?.email) {
                // Make sure email and current password fields are filled
                if (!isEmpty(binding.inputEmail.text.toString()) && !isEmpty(binding.inputPassword.text.toString())) {
                    // Verify that the user is changing to a company email address
                    if (isValidDomain(binding.inputEmail.text.toString())) {
                        editUserEmail()
                    } else {
                        Toast.makeText(this, "Invalid Domain", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Email and Current Password Fields Must be Filled to Save", Toast.LENGTH_SHORT).show()
                }
            }

            /*
            ------ METHOD 1 for changing database data (proper way in this scenario) -----
             */
            val reference = FirebaseDatabase.getInstance().getReference()

            /*
            ------ Change Name -----
             */
            if (!binding.inputName.text.toString().isEmpty()) {
                reference.child(getString(R.string.dbnode_users))
                    .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    .child(getString(R.string.field_name))
                    .setValue(binding.inputName.text.toString())
            }

            /*
            ------ Change Phone Number -----
             */
            if (!binding.inputPhone.text.toString().isEmpty()) {
                reference.child(getString(R.string.dbnode_users))
                    .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    .child(getString(R.string.field_phone))
                    .setValue(binding.inputPhone.text.toString())
            }

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }


        setResetPasswordListener();
    }

    private fun setResetPasswordListener() {
        binding.changePassword.setOnClickListener {
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
            EmailAuthProvider.getCredential(currentUser.email!!, binding.inputPassword.text.toString())
        Log.d(
            TAG,
            "editUserEmail: reauthenticating with:  \n email ${currentUser.email}" + " \n password: ${binding.inputPassword.text}"
        )

        currentUser.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "onComplete: reauthenticate success.")
                val newEmail = binding.inputEmail.text.toString()
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
                                                    Log.d(
                                                        TAG,
                                                        "onComplete: Verification email sent."
                                                    )
                                                    Toast.makeText(
                                                        this,
                                                        "Verification email sent",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    FirebaseAuth.getInstance().signOut();
                                                } else {
                                                    Log.w(
                                                        TAG,
                                                        "onComplete: Failed to send verification email.",
                                                        task.exception
                                                    )
                                                    Toast.makeText(
                                                        this,
                                                        "Failed to send verification email",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    hideDialog() // Assuming you have a progress dialog
                                                }
                                            }
                                    } else {
                                        Log.w(TAG, "updateUserEmail: No user is signed in")
                                        Toast.makeText(
                                            this,
                                            "Please sign in first",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
            binding.inputEmail.setText(email)
        }
    }

    private fun isValidDomain(email: String): Boolean {
        Log.d(TAG, "isValidDomain: verifying email has correct domain: $email")
        val domain = email.substring(email.indexOf("@") + 1).toLowerCase()
        Log.d(TAG, "isValidDomain: users domain: $domain")
        return domain == DOMAIN_NAME
    }

    private fun showDialog() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        if (binding.progressBar.visibility == View.VISIBLE) {
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private fun isEmpty(string: String): Boolean {
        return string == ""
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