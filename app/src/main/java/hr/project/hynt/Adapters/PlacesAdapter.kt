package hr.project.hynt.Adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Review
import hr.project.hynt.FirebaseDatabase.Workhour
import hr.project.hynt.R
import java.lang.Math.round
import java.util.*

class PlacesAdapter (private val mList: List<Place>, val mItemClickListener: ItemClickListener) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>() {

    val authUser = FirebaseAuth.getInstance().currentUser
    var hasRev : MutableList<Boolean> = ArrayList()
    var reviewID : MutableList<String> = ArrayList()
    var review : MutableList<Review> = ArrayList()
    var allReviews = ArrayList<ArrayList<Review>>()
    var showHint: Boolean = false
    var showHintDialog = true

    interface ItemClickListener{
        fun onItemClick(position: Int, place: Place, id: String, score: Int, allReviews : List<Review>, hasRev: Boolean, reviewID : String, review : Review)
        fun showHint(position: Int, place: Place, id: String, score: Int, allReviews : List<Review>, hasRev: Boolean, reviewID : String, review : Review)
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

        hasRev.add(false)
        reviewID.add("")
        review.add(Review())
        allReviews.add(ArrayList<Review>())

        holder.title.text = place.title
        holder.address.text = place.address.substringBefore(',')
        holder.category.text = place.category
        val opened = checkWorkhour(place.workhours)
        if (opened == 0) {
            holder.workhour.setImageResource(R.drawable.ic_closed_label)
        } else if (opened == 1) {
            holder.workhour.setImageResource(R.drawable.ic_open_label)
        } else {
            holder.workhour.visibility = View.INVISIBLE
        }

        hasRev[position] = false
        allReviews[position].clear()
        if (!place.reviews.isEmpty()) {
            place.reviews.forEach { id, rev ->
                allReviews[position].add(rev)
                if (authUser != null) {
                    if (rev.refId.equals(authUser.uid)) {
                        reviewID[position] = id
                        hasRev[position] = true
                        review[position] = rev
                    }
                }
            }
        }

        val list_stars = ArrayList<ImageView>()
        list_stars.add(holder.star1)
        list_stars.add(holder.star2)
        list_stars.add(holder.star3)
        list_stars.add(holder.star4)
        list_stars.add(holder.star5)
        check_stars(round(place.rating.toFloat()), list_stars)

        val sortedReviews = allReviews[position].sortedWith(compareBy({ it.timestamp })).reversed()
        if (position == 0){
            if (showHint) {
                holder.card.setBackgroundResource(R.drawable.bg_rounded_border_solid)
                if(showHintDialog){
                    showHintDialog = false
                    mItemClickListener.showHint(position, place, place.id, round(place.rating.toFloat()), sortedReviews, hasRev[position], reviewID[position], review[position])
                }

            } else {
                holder.card.setBackgroundColor(Color.parseColor("#FFFFFF"))
            }
        }
        holder.card.setOnClickListener{
            mItemClickListener.onItemClick(position, place, place.id, round(place.rating.toFloat()), sortedReviews, hasRev[position], reviewID[position], review[position])
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

        val star1: ImageView = itemView.findViewById(R.id.star1)
        val star2: ImageView = itemView.findViewById(R.id.star2)
        val star3: ImageView = itemView.findViewById(R.id.star3)
        val star4: ImageView = itemView.findViewById(R.id.star4)
        val star5: ImageView = itemView.findViewById(R.id.star5)


    }

    fun checkWorkhour(workhours : Workhour) : Int {
        var day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val current_workhours = workhours[day]
        if (current_workhours.isEmpty()){
            return 2
        } else if (current_workhours == "Closed") {
            return 0
        } else if (current_workhours == "Open 24h") {
            return 1
        } else {
            val range1 = current_workhours.substringBefore('\n')
            val start1 = range1.substring(0,2).toInt() * 100 + range1.substring(5,7).toInt()
            val end1 = range1.substring(10,12).toInt() * 100 + range1.substring(15,17).toInt()
            if (checkHours(start1, end1)) return 1
            if (current_workhours.length > 20){
                val range2 = current_workhours.substringAfter('\n')
                val start2 = range2.substring(0,2).toInt() * 100 + range2.substring(5,7).toInt()
                val end2 = range2.substring(10,12).toInt() * 100 + range2.substring(15,17).toInt()
                if (checkHours(start2, end2)) return 1
            }
        }
        return 0
    }

    private fun checkHours(start: Int, end: Int) : Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 100 + Calendar.getInstance().get(Calendar.MINUTE)
        return start < hour && end > hour
    }

    private fun check_stars(n: Int, list_stars: ArrayList<ImageView>) {
        for (i in 0..4) {
            if (i >= n) {
                list_stars[i].setImageResource(R.drawable.ic_star_review_off)
                continue
            }
            list_stars[i].setImageResource(R.drawable.ic_star_review_on)
        }

    }

    fun setHint(changedHint: Boolean) {
        showHint = changedHint
        notifyDataSetChanged()
    }
    fun setHintDialog(changedHint: Boolean) {
        showHintDialog = changedHint
        notifyDataSetChanged()
    }


}