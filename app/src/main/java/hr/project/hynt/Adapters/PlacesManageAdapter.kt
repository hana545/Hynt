package hr.project.hynt.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.util.CollectionUtils.listOf
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Workhour
import hr.project.hynt.R
import java.util.*

class PlacesManageAdapter (private val mList: List<Place>, private val mList_id: List<String>, val mItemClickListener: ItemClickListener, val nItemClickListener: ItemClickListener) : RecyclerView.Adapter<PlacesManageAdapter.ViewHolder>() {

    interface ItemClickListener{
        fun onItemClick(place: Place)
        fun onButtonClick(id: String)
    }
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_manage_place_card, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val place = mList[position]

        holder.title.text = place.title
        holder.address.text = place.address
        holder.category.text = place.category
        holder.btn_approve.setOnClickListener{
            mItemClickListener.onButtonClick(mList_id[position])
        }
        holder.card.setOnClickListener{
            nItemClickListener.onItemClick(place)
        }


    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val title: TextView = itemView.findViewById(R.id.place_title)
        val address: TextView = itemView.findViewById(R.id.place_address)
        val category: TextView = itemView.findViewById(R.id.place_category)
        val btn_approve: ImageButton = itemView.findViewById(R.id.btn_approve_place)
        val card: CardView = itemView.findViewById(R.id.mana_place_card)

    }



}