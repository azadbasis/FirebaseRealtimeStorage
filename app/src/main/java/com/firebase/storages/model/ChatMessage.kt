package com.firebase.storages.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatMessage(
    var message: String? = null,
    var user_id: String? = null,
    var timestamp: String? = null,
    var profile_image: String? = null,
    var name: String? = null
): Parcelable

