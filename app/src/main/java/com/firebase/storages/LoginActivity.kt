package com.firebase.storages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.firebase.storages.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        setupAuthStateListener()
        setupLogin()
        navigateRegistration()
        setupVerification()
        setupForgotPassword()
        hideSoftKeyboard()
    }

    private fun setupForgotPassword() {
        binding.btnForgotPassword.setOnClickListener {
            val dialog = PasswordResetDialog()
            dialog.show(supportFragmentManager, "dialog_forgot_password")
        }
    }

    private fun setupVerification() {
        binding.btnResendVerification.setOnClickListener {
            val dialog = ResendVerificationDialog()
            dialog.show(supportFragmentManager, "dialog_resend_email_verification")
        }
    }

    private fun navigateRegistration() {

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

    }

    private fun setupLogin() {
        binding.btnLogin.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        ProgressDialogUtil.showProgressDialog(this, "Logging in")
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { _ ->
            ProgressDialogUtil.hideProgressDialog()
        }.addOnFailureListener {
            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            ProgressDialogUtil.hideProgressDialog()
        }
    }

    // AuthStateListener setup
    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                // You can handle navigation or data loading here
                // For example, start a new activity:
                if ((user.isEmailVerified)) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
                    Toast.makeText(
                        this@LoginActivity,
                        "Authenticated with: " + user.email,
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this@LoginActivity,
                        MainActivity::class.java
                    )
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    user.sendEmailVerification().addOnCompleteListener { task: Task<Void> ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Sent Verification Email",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "couldn't send email",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    auth.signOut()
                }

            } else {
                // User is signed out
                // Handle what happens when the user is signed out
                Log.d(TAG, "User is signed out ")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
}