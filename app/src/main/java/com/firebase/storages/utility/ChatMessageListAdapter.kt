package com.firebase.storages.utility


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.firebase.storages.R
import com.firebase.storages.model.ChatMessage
import com.nostra13.universalimageloader.core.ImageLoader


class ChatMessageListAdapter(
    context: Context,
    @LayoutRes private val resource: Int,
    objects: List<ChatMessage>
) : ArrayAdapter<ChatMessage>(context, resource, objects) {

    private val mContext: Context = context

    class ViewHolder {
        var name: TextView? = null
        var message: TextView? = null
        var profileImage: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        var view = convertView

        if (view == null) {
            val inflater = LayoutInflater.from(mContext)
            view = inflater.inflate(resource, parent, false)
            holder = ViewHolder()
            holder.name = view.findViewById(R.id.name)
            holder.message = view.findViewById(R.id.message)
            holder.profileImage = view.findViewById(R.id.profile_image)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
            holder.name?.text = ""
            holder.message?.text = ""
        }

        try {
            val chatMessage = getItem(position)
            holder.message?.text = chatMessage?.message
            ImageLoader.getInstance().displayImage(chatMessage?.profile_image, holder.profileImage)
            holder.name?.text = chatMessage?.name
        } catch (e: NullPointerException) {
            Log.e("ChatMessageListAdapter", "getView: NullPointerException: ", e.cause)
        }

        return view!!
    }
}
