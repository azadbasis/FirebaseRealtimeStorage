package com.firebase.storages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebase.storages.databinding.ActivityRegistrationBinding
import com.firebase.storages.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch


class RegistrationActivity : AppCompatActivity() {

    private val TAG = "RegistrationActivity"
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var datastoreUtil: DatastoreUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        datastoreUtil = DatastoreUtil.getInstance(this) // Initialize DatastoreUtil
        setupRegistration()
    }

    private fun setupRegistration() {
        binding.btnRegister.setOnClickListener {
            persistData()
        }
    }

    private fun persistData() {
        // Use a coroutine to handle the suspend function call
        lifecycleScope.launch {
            saveNameData(binding.etName.text.toString())
            saveEmailData(binding.etEmail.text.toString())
            savePhoneData(binding.etPhone.text.toString())
        }
        createEmailRegistration(binding.etEmail.text.toString(), binding.etPassword.text.toString())
    }

    private fun createEmailRegistration(email: String, password: String) {
        showDialog();
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                Log.d(TAG, "createEmailRegistration: " + task.isSuccessful)
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success ${user?.uid}")
                    Toast.makeText(
                        baseContext,
                        "Authentication success.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    sendVerificationEmail()
                    val muser = User()
                    muser.name = binding.etName.text.toString()
                    muser.profile_image = ""
                    muser.security_level = "1"
                    muser.phone = binding.etPhone.text.toString()
                    muser.user_id = FirebaseAuth.getInstance().currentUser?.uid.toString()

                    FirebaseAuth.getInstance().currentUser?.uid?.let {
                        FirebaseDatabase.getInstance().getReference()
                            .child(getString(R.string.dbnode_users))
                            .child(it)
                            .setValue(muser)
                            .addOnCompleteListener { task ->
                                FirebaseAuth.getInstance().signOut()
                                // Redirect the user to the login screen
                                redirectToLogin()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT)
                                    .show()
                                FirebaseAuth.getInstance().signOut()
                                // Redirect the user to the login screen
                                redirectToLogin()
                            }
                    }

                    //   updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Unable to Register",
                        Toast.LENGTH_SHORT,
                    ).show()
                    // updateUI(null)
                }
                hideDialog()
            }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun sendVerificationEmail() {
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent to ${user.email}", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private suspend fun saveNameData(value: String) {
        datastoreUtil.saveData(DatastoreUtil.NAME_KEY, value)
    }

    private suspend fun saveEmailData(value: String) {
        datastoreUtil.saveData(DatastoreUtil.EMAIL_KEY, value)
    }

    private suspend fun savePhoneData(value: String) {
        datastoreUtil.saveData(DatastoreUtil.PHONE_KEY, value)
    }


    /**
     * Return true if @param 's1' matches @param 's2'
     * @param s1
     * @param s2
     * @return
     */
    private fun doStringsMatch(s1: String, s2: String): Boolean {
        return s1 == s2
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private fun isEmpty(string: String): Boolean {
        return string == ""
    }


    private fun showDialog() {
        binding.progressBar.setVisibility(View.VISIBLE)
    }

    private fun hideDialog() {
        if ( binding.progressBar.getVisibility() === View.VISIBLE) {
            binding.progressBar.setVisibility(View.INVISIBLE)
        }
    }

    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

}
