package com.firebase.storages

import android.app.ProgressDialog
import android.content.Context

object ProgressDialogUtil {

    private var progressDialog: ProgressDialog? = null

    fun showProgressDialog(context: Context, message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(context)
            progressDialog?.setMessage(message)
            progressDialog?.setCancelable(false)
        }
        progressDialog?.show()
    }

    fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}