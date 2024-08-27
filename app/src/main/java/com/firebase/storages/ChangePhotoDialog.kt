package com.firebase.storages

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class ChangePhotoDialog : DialogFragment() {

    private val TAG = "ChangePhotoDialog"
    private val CAMERA_REQUEST_CODE = 5467 // random number
    private val PICKFILE_REQUEST_CODE = 8352 // random number

    interface OnPhotoReceivedListener {
        fun getImagePath(imagePath: Uri)
        fun getImageBitmap(bitmap: Bitmap)
    }

    private lateinit var mOnPhotoReceived: OnPhotoReceivedListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_changephoto, container, false)

        // Initialize the TextView for choosing an image from memory
        val selectPhoto = view.findViewById<TextView>(R.id.dialogChoosePhoto)
        selectPhoto.setOnClickListener {
            Log.d(TAG, "onClick: accessing phone's memory.")
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICKFILE_REQUEST_CODE)
        }

        // Initialize the TextView for choosing an image from memory
        val takePhoto = view.findViewById<TextView>(R.id.dialogOpenCamera)
        takePhoto.setOnClickListener {
            Log.d(TAG, "onClick: starting camera")
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*
        Results when selecting a new image from phone memory
         */
        if (requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            Log.d(TAG, "onActivityResult: image: $selectedImageUri")

            // Send the bitmap and fragment to the interface
            selectedImageUri?.let {
                mOnPhotoReceived.getImagePath(it)
            }
            dialog?.dismiss()
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "onActivityResult: done taking a photo.")

            val bitmap: Bitmap? = data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                mOnPhotoReceived.getImageBitmap(it)
            }
            dialog?.dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mOnPhotoReceived = activity as OnPhotoReceivedListener
        } catch (e: ClassCastException) {
            Log.e(TAG, "onAttach: ClassCastException", e.cause)
        }
    }
}
