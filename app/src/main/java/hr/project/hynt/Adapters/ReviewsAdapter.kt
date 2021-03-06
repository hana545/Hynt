package hr.project.hynt.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import hr.project.hynt.FirebaseDatabase.Review
import hr.project.hynt.R
import java.util.*
import java.util.Collections.emptyList

class ReviewsAdapter(private val mList: List<Review>, private val mList_id: List<String>? = emptyList(), private val type: String, val mItemClickListener: ItemClickListener) : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {

    interface ItemClickListener{
        fun onItemClick(review : Review, reviewID : String)
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        var view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_review_card, parent, false)
        if (type ==  "myReviews") {
            view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_my_review_card, parent, false)
        }

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val review = mList[position]

        if (type == "myReviews"){
            holder.reviewAuthor.text = review.refName
            holder.card!!.setOnClickListener{
                mItemClickListener.onItemClick(review, mList_id!!.get(position))
            }

        } else {
            holder.reviewAuthor.text = review.refName + " said:"
        }
        holder.reviewDate.text = review.date
        if (!review.txt.isEmpty()) {
            holder.reviewText.text = "\""+review.txt+"\""
        } else {
            holder.reviewText.visibility = View.GONE
        }


        val list_stars = ArrayList<ImageView>()
        list_stars.add(holder.star1)
        list_stars.add(holder.star2)
        list_stars.add(holder.star3)
        list_stars.add(holder.star4)
        list_stars.add(holder.star5)
        check_stars(review.stars, list_stars)



    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val card : CardView? = itemView.findViewById(R.id.review_card)
        val reviewAuthor: TextView = itemView.findViewById(R.id.review_author)
        val reviewDate: TextView = itemView.findViewById(R.id.review_date)
        val reviewText: TextView = itemView.findViewById(R.id.review_txt)

        val star1: ImageView = itemView.findViewById(R.id.review_star1)
        val star2: ImageView = itemView.findViewById(R.id.review_star2)
        val star3: ImageView = itemView.findViewById(R.id.review_star3)
        val star4: ImageView = itemView.findViewById(R.id.review_star4)
        val star5: ImageView = itemView.findViewById(R.id.review_star5)


    }
    
    private fun check_stars(n: Int, list_stars : ArrayList<ImageView>) {
        for (i in 0..4) {
            if (i >= n) {
                list_stars[i].setImageResource(R.drawable.ic_star_review_off)
                continue
            }
            list_stars[i].setImageResource(R.drawable.ic_star_review_on)
        }

    }


}