package hr.project.hynt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.project.hynt.FirebaseDatabase.TagCategory

class TagCategoryAdapter (private val mList: List<String>, private val mList_id: List<String>, private val type: String, val mItemClickListener:ItemClickListener) : RecyclerView.Adapter<TagCategoryAdapter.ViewHolder>() {

    interface ItemClickListener{
        fun onItemClick(id: String)
    }
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_tags_categories, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val name = mList[position]

        // sets the text to the textview from our itemHolder class
        holder.view_text.text = name
        holder.btn_del.setOnClickListener{
            mItemClickListener.onItemClick(mList_id[position])
        }


    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val view_text: TextView = itemView.findViewById(R.id.view_name)
        val btn_del: ImageButton = itemView.findViewById(R.id.btn_delete)

    }
}