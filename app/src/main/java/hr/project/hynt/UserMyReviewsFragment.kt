package hr.project.hynt

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import hr.project.hynt.Adapters.ReviewsAdapter
import hr.project.hynt.FirebaseDatabase.Review
import java.text.SimpleDateFormat
import java.util.*

class UserMyReviewsFragment : Fragment(), ReviewsAdapter.ItemClickListener {
    var allReviews= ArrayList<Review>()
    var allReviewsId = ArrayList<String>()

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {

        val view = inflater.inflate(R.layout.fragment_user_my_reviews, container, false)
        val recyclerview = view.findViewById<RecyclerView>(R.id.review_recyclerView)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        val adapter = ReviewsAdapter(allReviews, allReviewsId, "myReviews", this)
        recyclerview.adapter = adapter
        getAllReviews(adapter)
        // Inflate the layout for this fragment
        return view
    }

    override fun onItemClick(review: Review, reviewID: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_show_my_review)
        dialog.findViewById<ImageView>(R.id.close_dialog).setOnClickListener { dialog.dismiss() }

        val place_title : TextView = dialog.findViewById<TextView>(R.id.show_review_title_data)
        place_title.text = review.refName
        val review_text : TextView = dialog.findViewById<TextView>(R.id.show_review_myReviewText)
        review_text.text = review.txt
        val review_score : TextView = dialog.findViewById<TextView>(R.id.show_review_myReviewScore_summary)
        review_score.text = "You rated this place with "+review.stars
        review_score.text = review_score.text.toString() + if (review.stars > 1)  " stars" else " star"
        val myStar1: ImageView = dialog.findViewById<ImageView>(R.id.my_star1)
        val myStar2: ImageView = dialog.findViewById<ImageView>(R.id.my_star2)
        val myStar3: ImageView = dialog.findViewById<ImageView>(R.id.my_star3)
        val myStar4: ImageView = dialog.findViewById<ImageView>(R.id.my_star4)
        val myStar5: ImageView = dialog.findViewById<ImageView>(R.id.my_star5)

        check_stars(review.stars - 1, listOf(myStar1, myStar2, myStar3, myStar4, myStar5))


        var checked_stars: Int = review.stars
        myStar1.setOnClickListener {
            checked_stars = 1
            check_stars(0, listOf(myStar1, myStar2, myStar3, myStar4, myStar5))
            review_score.text  = "Thanks you are rating this with 1 star"
        }
        myStar2.setOnClickListener {
            checked_stars = 2
            check_stars(1, listOf(myStar1, myStar2, myStar3, myStar4, myStar5))
            review_score.text  = "Thanks you are rating this with 2 stars"
        }
        myStar3.setOnClickListener {
            checked_stars = 3
            check_stars(2, listOf(myStar1, myStar2, myStar3, myStar4, myStar5))
            review_score.text  = "Thanks you are rating this with 3 stars"
        }
        myStar4.setOnClickListener {
            checked_stars = 4
            check_stars(3, listOf(myStar1, myStar2, myStar3, myStar4, myStar5))
            review_score.text  = "Thanks you are rating this with 4 stars"
        }
        myStar5.setOnClickListener {
            checked_stars = 5
            check_stars(4, listOf(myStar1, myStar2, myStar3, myStar4, myStar5))
            review_score.text  = "Thanks you are rating this with 5 stars"
        }
        val btn_saveReview : Button = dialog.findViewById<Button>(R.id.btn_save_review)
        val btn_deleteReview : Button = dialog.findViewById<Button>(R.id.btn_delete_review)
        btn_saveReview.setOnClickListener{
            var  text :String = review_text.text.toString()
            text = text.replace("\\s+".toRegex(), " ").trim()
            val reviewU = Review(Calendar.getInstance().time, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), text, checked_stars, review.refName, review.refId)
            val reviewP = Review(Calendar.getInstance().time, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), text, checked_stars, authUser!!.displayName!!, authUser.uid)

            db.getReference("places").child(review.refId).child("reviews").child(authUser.uid).setValue(reviewP)
            db.getReference("users").child(authUser.uid).child("reviews").child(review.refId).setValue(reviewU)
            dialog.dismiss()


        }
        btn_deleteReview.setOnClickListener {
            AlertDialog.Builder(activity)
                    .setTitle("Delete review")
                    .setMessage("Are you sure you want to delete this review?")
                    .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { delete_dialog, which ->
                        db.getReference("places").child(review.refId).child("reviews").child(authUser!!.uid).removeValue()
                        db.getReference("users").child(authUser.uid).child("reviews").child(review.refId).removeValue()
                        dialog.dismiss()
                        delete_dialog.dismiss()
                        show_info_dialog("Review deleted", true)
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_remove)
                    .setCancelable(false)
                    .show()
        }


        dialog.show()
    }
    private fun check_stars(n: Int, list_stars: MutableList<ImageView>) {
        for (i in 0..4) {
            if (i > n) {
                list_stars[i].setImageResource(R.drawable.ic_star_review_off)
                continue
            }
            list_stars[i].setImageResource(R.drawable.ic_star_review_on)
        }
    }

    fun getAllReviews(adapter: ReviewsAdapter) {
        val reviews_query = db.getReference("users").child(authUser!!.uid).child("reviews").orderByChild("timestamp/time")
        reviews_query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allReviews.clear()
                allReviewsId.clear()
                if (snapshot.exists()) {
                    for (reviews: DataSnapshot in snapshot.children) {
                        val review: Review? = reviews.getValue<Review>()
                        if (review != null) {
                            allReviews.add(review)
                            allReviewsId.add(reviews.key.toString())
                        }
                    }
                }
                allReviews.reverse()
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }
    private fun show_info_dialog(text : String, succes : Boolean){
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        if (succes) {
            dialog.setContentView(R.layout.dialog_info_success)
        } else {
            dialog.setContentView(R.layout.dialog_info_failed)
        }
        dialog.findViewById<TextView>(R.id.info_text).text = text
        dialog.findViewById<Button>(R.id.btn_continue).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

}










