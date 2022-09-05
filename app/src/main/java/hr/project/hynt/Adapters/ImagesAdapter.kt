package hr.project.hynt.Adapters

import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import hr.project.hynt.R

class ImagesAdapter(private val mList: List<String>, val uploadImage: Boolean, val mItemClickListener: ItemClickListener) : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {


    interface ItemClickListener{
        fun onImageClick(imageUri: Uri, allImages : ArrayList<String>)
        fun onBtnRemove(position : Int)
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item

        var view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_image, parent, false)
        if (uploadImage) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_image_upload, parent, false)
        }


        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val imageUri = mList[position]

        Glide.with(holder.itemView.getContext()).load(imageUri.toUri()).into(holder.image)

        if (uploadImage){
            holder.btn_remove?.visibility = View.VISIBLE
            holder.btn_remove?.setOnClickListener {
                mItemClickListener.onBtnRemove(position)
            }
        } else {
            holder.image.setOnClickListener{
                mItemClickListener.onImageClick(imageUri.toUri(), mList as ArrayList<String>)
            }
        }

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val image : ImageView = itemView.findViewById(R.id.view_image)
        val btn_remove : ImageButton? = itemView.findViewById(R.id.btn_remove)


    }


}