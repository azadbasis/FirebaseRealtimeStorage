package com.firebase.storages.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Chatroom(
    var chatroom_name: String? = null,
    var creator_id: String? = null,
    var security_level: String? = null,
    var chatroom_id: String? = null,
    var chatroom_messages: List<ChatMessage>? = null
) : Parcelable

