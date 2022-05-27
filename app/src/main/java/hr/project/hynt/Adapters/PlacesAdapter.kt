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

class PlacesAdapter (private val mList: List<Place>, private val mList_id: List<String>, val mItemClickListener: ItemClickListener) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>() {

    interface ItemClickListener{
        fun onItemClick(id: String)
    }
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_place_card, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val place = mList[position]

        holder.title.text = place.title
        holder.address.text = place.address
        holder.category.text = place.category
        val opened = checkWorkhour(place.workhours)
        if (opened == 0){
            holder.workhour.setImageResource(R.drawable.ic_closed_label)
        } else if (opened == 1){
            holder.workhour.setImageResource(R.drawable.ic_open_label)
        } else {
            holder.workhour.visibility = View.INVISIBLE
        }
        holder.card.setOnClickListener{
            mItemClickListener.onItemClick(mList_id[position])
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
        val workhour: ImageView = itemView.findViewById(R.id.workhour_icon)
        val card: CardView = itemView.findViewById(R.id.place_card)

    }

    private fun checkWorkhour(workhours : Workhour) : Int {
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        var day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        Log.d("PlaceAdapter", "day: "+day)
        val current_workhours = workhours[day]
        if (current_workhours.isEmpty()){
            return 2
        } else if (current_workhours == "Closed") {
            return 0
        } else {
            val range1 = current_workhours.substringBefore('\n')
            Log.d("PlaceAdapter", "range1: "+range1)
            val start1 = range1.substring(0,2).toInt() * 100 + range1.substring(5,7).toInt()
            Log.d("PlaceAdapter", "start1: "+start1)
            val end1 = range1.substring(10,12).toInt() * 100 + range1.substring(15,17).toInt()
            Log.d("PlaceAdapter", "end1: "+end1)
            if (checkHours(start1, end1)) return 1
            if (current_workhours.length > 20){
                val range2 = current_workhours.substringAfter('\n')
                val start2 = range2.substring(0,2).toInt() * 100 + range2.substring(5,7).toInt()
                val end2 = range2.substring(10,12).toInt() * 100 + range2.substring(15,17).toInt()
                if (checkHours(start2, end2)) return 1
                Log.d("PlaceAdapter", "end2: "+end2)
                Log.d("PlaceAdapter", "range2: "+range2)
                Log.d("PlaceAdapter", "start2: "+start2)
            }
        }
        return 0
    }

    private fun checkHours(start: Int, end: Int) : Boolean {
        var hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 100 + Calendar.getInstance().get(Calendar.MINUTE)
        if (start < hour && end > hour){
           return true
        } else {
            return false
        }
        return true
    }


}