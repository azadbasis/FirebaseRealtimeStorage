package com.firebase.storages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firebase.storages.databinding.ActivitySettingsBinding
import com.firebase.storages.model.User
import com.firebase.storages.utility.FilePaths
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.nostra13.universalimageloader.core.ImageLoader
import java.io.ByteArrayOutputStream
import java.io.IOException


class SettingsActivity : AppCompatActivity(), ChangePhotoDialog.OnPhotoReceivedListener {

    private val TAG = "SettingsActivity"


    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener

    private lateinit var binding: ActivitySettingsBinding

    //    private val DOMAIN_NAME = "tabian.ca"
    private val DOMAIN_NAME = "gmail.com"
    private val REQUEST_CODE = 1234
    private val MB_THRESHOLD = 5.0
    private val MB = 1000000.0

    // Vars
    private var mStoragePermissions: Boolean = false
    private var mSelectedImageUri: Uri? = null
    private var mSelectedImageBitmap: Bitmap? = null
    private var mBytes: ByteArray? = null
    private var progress: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        verifyStoragePermissions()
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

                    binding.inputName.setText(user?.name)
                    binding.inputPhone.setText(user?.phone)
                    ImageLoader.getInstance().displayImage(user?.profile_image, binding.profileImage);
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.message)
            }
        })

        /*
            --------- Query method 2 ----------
         */
 /*       val query2: Query = reference.child(getString(R.string.dbnode_users))
            .orderByKey()
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)

        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(
                        User::class.java
                    )
                    Log.d(TAG, "onDataChange: (QUERY METHOD 2) found user: " + user.toString())

                    binding.inputName.setText(user?.name)
                    binding.inputPhone.setText(user?.phone)
                    ImageLoader.getInstance().displayImage(user?.profile_image, binding.profileImage);
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })*/

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
                    Toast.makeText(
                        this,
                        "Email and Current Password Fields Must be Filled to Save",
                        Toast.LENGTH_SHORT
                    ).show()
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

            /*
              ------ Upload the New Photo -----
               */
            if(mSelectedImageUri != null){
                uploadNewPhoto(mSelectedImageUri);
            }else if(mSelectedImageBitmap  != null){
                uploadNewPhoto(mSelectedImageBitmap);
            }
            
        }
        setResetPasswordListener();
        setProfileImageListener()
    }

    /**
     * Uploads a new profile photo to Firebase Storage using a @param ***imageUri***
     * @param imageUri
     */
    fun uploadNewPhoto(imageUri: Uri?) {
        /*
            upload a new profile photo to firebase storage
         */
        Log.d(TAG, "uploadNewPhoto: uploading new profile photo to firebase storage.")

        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        val resize: BackgroundImageResize = BackgroundImageResize(null)
        resize.execute(imageUri)
    }

    /**
     * Uploads a new profile photo to Firebase Storage using a @param ***imageBitmap***
     * @param imageBitmap
     */
    fun uploadNewPhoto(imageBitmap: Bitmap?) {
        /*
            upload a new profile photo to firebase storage
         */
        Log.d(TAG, "uploadNewPhoto: uploading new profile photo to firebase storage.")

        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        val resize: BackgroundImageResize = BackgroundImageResize(imageBitmap)
        val uri: Uri? = null
        resize.execute(uri)
    }

    private fun setProfileImageListener() {
        binding.profileImage.setOnClickListener(View.OnClickListener {
            if (mStoragePermissions) {
                val dialog = ChangePhotoDialog()
                dialog.show(supportFragmentManager, getString(R.string.dialog_change_photo))
            } else {
                verifyStoragePermissions()
            }
        })
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
            EmailAuthProvider.getCredential(
                currentUser.email!!,
                binding.inputPassword.text.toString()
            )
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

    override fun getImagePath(imagePath: Uri) {
        if (!imagePath.toString().isEmpty()) {
            mSelectedImageBitmap = null
            mSelectedImageUri = imagePath
            Log.d(TAG, "getImagePath: got the image uri: $mSelectedImageUri")
            ImageLoader.getInstance().displayImage(imagePath.toString(), binding.profileImage)
        }
    }

    override fun getImageBitmap(bitmap: Bitmap) {
        if (bitmap != null) {
            mSelectedImageUri = null
            mSelectedImageBitmap = bitmap
            Log.d(TAG, "getImageBitmap: got the image bitmap: $mSelectedImageBitmap")
            binding.profileImage.setImageBitmap(bitmap)
        }
    }

    /**
     * 1) doinBackground takes an imageUri and returns the byte array after compression
     * 2) onPostExecute will print the % compression to the log once finished
     */

    /**
     * 1) doInBackground takes an imageUri and returns the byte array after compression
     * 2) onPostExecute will print the % compression to the log once finished
     */
    inner class BackgroundImageResize(private val mBitmap: Bitmap?) : AsyncTask<Uri, Int, ByteArray?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            showDialog()
            Toast.makeText(this@SettingsActivity, "compressing image", Toast.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: Uri?): ByteArray? {
            Log.d(TAG, "doInBackground: started.")

            var bitmap = mBitmap
            if (bitmap == null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this@SettingsActivity.contentResolver, params[0])
                    Log.d(TAG, "doInBackground: bitmap size: megabytes: ${bitmap?.byteCount?.div(MB)} MB")
                } catch (e: IOException) {
                    Log.e(TAG, "doInBackground: IOException: ", e.cause)
                }
            }

            var bytes: ByteArray? = null
            for (i in 1..10) {
                if (i == 10) {
                    Toast.makeText(this@SettingsActivity, "That image is too large.", Toast.LENGTH_SHORT).show()
                    break
                }
                bytes = getBytesFromBitmap(bitmap!!, 100 / i)
                Log.d(TAG, "doInBackground: megabytes: (${11 - i}0%) ${bytes.size / MB} MB")
                if (bytes.size / MB < MB_THRESHOLD) {
                    return bytes
                }
            }
            return bytes
        }

        override fun onPostExecute(bytes: ByteArray?) {
            super.onPostExecute(bytes)
            hideDialog()
            mBytes = bytes
            // Execute the upload
            executeUploadTask()
        }
    }

    // Convert from bitmap to byte array
    fun getBytesFromBitmap(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun executeUploadTask() {
        showDialog()
        val filePaths = FilePaths()
        // Specify where the photo will be stored
        val storageReference = FirebaseStorage.getInstance().getReference()
            .child("${filePaths.FIREBASE_IMAGE_STORAGE}/${FirebaseAuth.getInstance().currentUser?.uid}/profile_image")
       mBytes?.let {
           if (it.size / MB < MB_THRESHOLD) {
               // Create file metadata including the content type
               val metadata = StorageMetadata.Builder()
                   .setContentType("image/jpg")
                   .setContentLanguage("en") // See nodes below
                   /*
                   Make sure to use proper language code ("English" will cause a crash)
                   I actually submitted this as a bug to the Firebase GitHub page so it might be
                   fixed by the time you watch this video. You can check it out at https://github.com/firebase/quickstart-unity/issues/116
                    */
                   .setCustomMetadata("Mitch's special meta data", "JK nothing special here")
                   .setCustomMetadata("location", "Iceland")
                   .build()

               // If the image size is valid, then we can submit to the database
               val uploadTask = storageReference.putBytes(it, metadata)
               // uploadTask = storageReference.putBytes(mBytes) // Without metadata


               uploadTask.addOnSuccessListener { taskSnapshot ->
                   // Now insert the download URL into the Firebase database
                   storageReference.downloadUrl.addOnSuccessListener { uri ->
                       val firebaseURL = uri.toString()
                       Toast.makeText(this, "Upload Success", Toast.LENGTH_SHORT).show()
                       Log.d(TAG, "onSuccess: Firebase download URL: $firebaseURL")

                       FirebaseDatabase.getInstance().reference
                           .child(getString(R.string.dbnode_users))
                           .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                           .child(getString(R.string.field_profile_image))
                           .setValue(firebaseURL)
                   }.addOnFailureListener { exception ->
                       Toast.makeText(this, "Failed to retrieve download URL", Toast.LENGTH_SHORT).show()
                       Log.e(TAG, "onFailure: Could not retrieve download URL", exception)
                   }

                   hideDialog()
               }.addOnFailureListener { exception ->
                   Toast.makeText(this, "Could not upload photo", Toast.LENGTH_SHORT).show()
                   hideDialog()
               }.addOnProgressListener { taskSnapshot ->
                   val currentProgress = (100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                   if (currentProgress > (progress + 15)) {
                       progress = ((100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toDouble()
                       Log.d(TAG, "onProgress: Upload is $progress% done")
                       Toast.makeText(this, "$progress%", Toast.LENGTH_SHORT).show()
                   }
               }
           } else {
               Toast.makeText(this, "Image is too large", Toast.LENGTH_SHORT).show()
           }
       }

    }

    /**
     * Generalized method for asking permission. Can pass any array of permissions
     */
    fun verifyStoragePermissions() {
        Log.d(TAG, "verifyPermissions: asking user for permissions.")
        val permissions = arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        if ((ContextCompat.checkSelfPermission(
                this.applicationContext,
                permissions[0]
            ) == PackageManager.PERMISSION_GRANTED) && ContextCompat.checkSelfPermission(
                this.applicationContext,
                permissions[1]
            ) == PackageManager.PERMISSION_GRANTED && (ContextCompat.checkSelfPermission(
                this.applicationContext,
                permissions[2]
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            mStoragePermissions = true
        } else {
            ActivityCompat.requestPermissions(
                this@SettingsActivity,
                permissions,
                REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionsResult: requestCode: $requestCode")
        when (requestCode) {
            REQUEST_CODE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(
                    TAG,
                    "onRequestPermissionsResult: User has allowed permission to access: " + permissions[0]
                )
            }
        }
    }

}