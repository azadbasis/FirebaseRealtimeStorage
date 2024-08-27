package com.firebase.storages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebase.storages.databinding.ActivityMainBinding
import com.firebase.storages.utility.UniversalImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.nostra13.universalimageloader.core.ImageLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // Declare the binding variable
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var datastoreUtil: DatastoreUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar();
        setupObject()
        setupAuthStateListener()
        setUserDetails()
        getUserDetails()
    }

    private fun setUserDetails() {
        val user = auth.currentUser
          if (user != null) {
            lifecycleScope.launch {
                val displayName = datastoreUtil.getData(DatastoreUtil.NAME_KEY, "").first()
                val email = datastoreUtil.getData(DatastoreUtil.EMAIL_KEY, "").first()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .setPhotoUri(Uri.parse("https://example.com/profile.jpg"))
                    .build()
                user.updateProfile(profileUpdates)
            }

        }
    }

    private fun getUserDetails() {
        val user = auth.currentUser
        if (user != null) {
            val message =
                "Welcome ${user.displayName}, your email is ${user.email}, and your uid is ${user.uid}, \nyour phone number is ${user.phoneNumber} and your photo url is ${user.photoUrl}"
            binding.tvMessage.text = message
        }
    }

    private fun setupObject() {
        auth = FirebaseAuth.getInstance()
        datastoreUtil = DatastoreUtil.getInstance(this) // Initialize DatastoreUtil
        initImageLoader()
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
            } else {
                Log.d(TAG, "onAuthStateChanged:signed_out")
                val intent = Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                )
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
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

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = FirebaseAuth.getInstance().currentUser?.email
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    /**
     * init universal image loader
     */
    private fun initImageLoader() {
        val imageLoader = UniversalImageLoader(this@MainActivity)
        ImageLoader.getInstance().init(imageLoader.getConfig())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Handle settings action
                signOut()
                true
            }

            R.id.action_settings -> {
                // Handle settings action
                val intent = Intent(
                    this@MainActivity,
                    SettingsActivity::class.java
                )
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkFirebaseAuthState() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            // User is not logged in, navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Log.d(TAG, "checkFirebaseAuthState: user is authenticated")
        }
    }

    override fun onResume() {
        super.onResume()
        checkFirebaseAuthState()
    }


    /**
     * Sign out the current user
     */
    private fun signOut() {
        Log.d(TAG, "signOut: signing out")
        auth.signOut()
    }

    private fun getNameData(): Flow<String> {
        return datastoreUtil.getData(DatastoreUtil.NAME_KEY, "")
    }

    private fun getEmailData(): Flow<String> {
        return datastoreUtil.getData(DatastoreUtil.EMAIL_KEY, "")
    }
}