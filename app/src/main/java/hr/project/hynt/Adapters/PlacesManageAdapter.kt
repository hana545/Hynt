package hr.project.hynt.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.R
import java.util.*

class PlacesManageAdapter (private val mList: List<Place>, private val type : String, val mItemClickListener: ItemClickListener) : RecyclerView.Adapter<PlacesManageAdapter.ViewHolder>(), SectionTitleProvider {

    interface ItemClickListener{
        fun onItemClick(place: Place, placeId : String)
        fun onItemLongClick(place: Place, placeId : String)
        fun onPositiveButtonClick(id: String, place: Place)
        fun onNegativeButtonClick(id: String, placeName : String)
    }
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        var view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_my_place_card, parent, false)
        if(type == "admin") {
            view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_manage_place_card, parent, false)
        }


        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val place = mList[position]

        holder.title.text = place.title
        holder.address.text = place.address.substringBefore(',')
        holder.category.text = place.category
        holder.btn_positive.setOnClickListener{
            mItemClickListener.onPositiveButtonClick(mList[position].id, place)
        }
        holder.btn_negative.setOnClickListener{
            mItemClickListener.onNegativeButtonClick(mList[position].id, place.title)
        }
        holder.card.setOnClickListener{
            mItemClickListener.onItemClick(place, mList[position].id)
        }
        holder.card.setOnLongClickListener {
            mItemClickListener.onItemLongClick(place, mList[position].id)
            true
        }
        if (type == "user"){
            if (place.approved){
                holder.card.setBackgroundColor(Color.parseColor("#DAFBD1"))
            } else {
                if (place.pending){
                    holder.card.setBackgroundColor(Color.parseColor("#B8DEFF"))
                } else {
                    holder.card.setBackgroundColor(Color.parseColor("#F8D8D8"))
                }
            }
        }


    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val card: CardView = itemView.findViewById(R.id.place_card)
        val title: TextView = itemView.findViewById(R.id.place_title)
        val address: TextView = itemView.findViewById(R.id.place_address)
        val category: TextView = itemView.findViewById(R.id.place_category)
        val btn_positive: ImageButton = itemView.findViewById(R.id.btn_positive)
        val btn_negative: ImageButton = itemView.findViewById(R.id.btn_negative)

    }

    override fun getSectionTitle(position: Int): String? {
        //this String will be shown in a bubble for specified position
        return mList[position].title.substring(0, 1)
    }



}