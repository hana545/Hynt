package hr.project.hynt

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import hr.project.hynt.Adapters.ImagesAdapter
import hr.project.hynt.Adapters.PlacesAdapter
import hr.project.hynt.Adapters.PlacesManageAdapter
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Review
import hr.project.hynt.FirebaseDatabase.TagCategory
import hr.project.hynt.FirebaseDatabase.User
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UserMyPlacesFragment : Fragment(), PlacesManageAdapter.ItemClickListener, ImagesAdapter.ItemClickListener {
    var allPlaces = ArrayList<Place>()
    var allPlacesId = ArrayList<String>()

    var filter = 0
    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    lateinit var text_info : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_user_my_places, container, false)
        text_info = view.findViewById(R.id.fragment_info)
        val recyclerview = view.findViewById<RecyclerView>(R.id.place_recyclerView)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = GridLayoutManager(requireContext(), 2)

        val adapter = PlacesManageAdapter(allPlaces, allPlacesId, "user",this)
        recyclerview.adapter = adapter
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar_UserPlaces)
        getAllPlaces(adapter, progressBar)
        view.findViewById<FloatingActionButton>(R.id.fragment_btn_add_new_place).setOnClickListener {
            val intent = Intent(context, AddNewPlaceActivity::class.java)
            startActivity(intent)
        }
        view.findViewById<TextView>(R.id.fil_approved).setOnClickListener {
            filter = 1
            progressBar.visibility = View.VISIBLE
            getAllPlaces(adapter, progressBar)
        }
        view.findViewById<TextView>(R.id.fil_pending).setOnClickListener {
            filter = 2
            progressBar.visibility = View.VISIBLE
            getAllPlaces(adapter, progressBar)
        }
        view.findViewById<TextView>(R.id.fil_notapproved).setOnClickListener {
            filter = 3
            progressBar.visibility = View.VISIBLE
            getAllPlaces(adapter, progressBar)
        }
        // Inflate the layout for this fragment
        return view
    }

    fun getAllPlaces(adapter: PlacesManageAdapter, progressBar: ProgressBar) {
        val places_query = db.getReference("places").orderByChild("timestamp/time")
        places_query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlaces.clear()
                allPlacesId.clear()
                if (snapshot.exists()) {
                    for (places: DataSnapshot in snapshot.children) {
                        val place: Place? = places.getValue<Place>()
                        if (place != null && place.authorID.equals(authUser!!.uid.toString())) {
                            when (filter) {
                                0 -> {
                                    allPlaces.add(place)
                                    allPlacesId.add(places.key.toString())
                                }
                                1 -> {
                                    if (place.approved && !place.pending) {
                                        allPlaces.add(place)
                                        allPlacesId.add(places.key.toString())
                                    }
                                }
                                2 -> {
                                    if (!place.approved && place.pending) {
                                        allPlaces.add(place)
                                        allPlacesId.add(places.key.toString())
                                    }
                                }
                                3 -> {
                                    if (!place.approved && !place.pending) {
                                        allPlaces.add(place)
                                        allPlacesId.add(places.key.toString())
                                    }
                                }
                            }
                        }
                    }
                    allPlaces.reverse()
                    allPlacesId.reverse()
                }
                text_info.visibility = if (allPlaces.isEmpty()) View.VISIBLE else View.GONE
                progressBar.visibility = View.GONE
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }

    override fun onPositiveButtonClick(id: String, place: Place) {
        val intent = Intent(requireContext(), AddNewPlaceActivity::class.java)
        intent.putExtra("new", false)
        intent.putExtra("place_id", id)
        startActivity(intent)
    }

    override fun onNegativeButtonClick(id: String, placeName: String) {
        AlertDialog.Builder(activity)
                .setTitle("Delete place")
                .setMessage("Are you sure you want to delete this place: " + placeName + "?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                    db.getReference("places").child(id).removeValue()
                    db.getReference("users").addValueEventListener(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                           if(snapshot.exists()){
                               for (users : DataSnapshot in snapshot.children){
                                   db.getReference("users").child(users.key.toString()).child("reviews").child(id).removeValue()
                               }
                           }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })
                    show_info_dialog("Place deleted", true)
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_remove)
                .setCancelable(false)
                .show()
    }


    override fun onItemClick(place: Place, placeId: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_show_place)

        dialog.findViewById<ImageView>(R.id.close_dialog).setOnClickListener { dialog.dismiss() }

        val place_title: TextView = dialog.findViewById<TextView>(R.id.show_place_title_data)
        val place_address: TextView = dialog.findViewById<TextView>(R.id.show_place_address_data)
        val place_description: TextView = dialog.findViewById<TextView>(R.id.show_place_description)
        val place_category: TextView = dialog.findViewById<TextView>(R.id.show_place_category)

        place_title.text = place.title
        place_address.text = place.address
        place_description.text = place.desc
        dialog.findViewById<ImageButton>(R.id.show_place_btn_show_on_map).visibility = View.GONE
        if (place.desc.isEmpty()) place_description.visibility = View.GONE
        place_category.text = place.category
        // for Tags
        for (tag in place.tags) {
            val chip = this.layoutInflater.inflate(R.layout.view_chips_buttons, null, false) as Chip
            chip.text = tag
            chip.isEnabled = false
            dialog.findViewById<FlexboxLayout>(R.id.show_place_tags_chip_group).addView(chip)
        }
        //for Contacts
        val place_phone1 : TextView = dialog.findViewById<TextView>(R.id.show_place_phone1)
        val place_email1 : TextView = dialog.findViewById<TextView>(R.id.show_place_email1)
        val place_web1 : TextView = dialog.findViewById<TextView>(R.id.show_place_web1)
        val place_phone2 : TextView = dialog.findViewById<TextView>(R.id.show_place_phone2)
        val place_email2 : TextView = dialog.findViewById<TextView>(R.id.show_place_email2)
        val place_web2 : TextView = dialog.findViewById<TextView>(R.id.show_place_web2)

        val phone_layout =  dialog.findViewById<LinearLayout>(R.id.show_place_contacts_phone)
        place_phone1.text = place.phone1
        place_phone2.text = place.phone2
        if (!place.phone1.isEmpty() && !place.phone2.isEmpty()) {
            phone_layout.visibility = View.VISIBLE
        } else if (!place.phone1.isEmpty() || !place.phone2.isEmpty()){
            phone_layout.visibility = View.VISIBLE
            if (place.phone1.isEmpty()) place_phone1.visibility = View.GONE else place_phone1.visibility = View.VISIBLE
            if (place.phone2.isEmpty()) place_phone2.visibility = View.GONE else place_phone2.visibility = View.VISIBLE
        }
        val email_layout =  dialog.findViewById<LinearLayout>(R.id.show_place_contacts_email)
        place_email1.text = place.email1
        place_email2.text = place.email2
        if (!place.email1.isEmpty() && !place.email2.isEmpty()) {
            email_layout.visibility = View.VISIBLE
        } else if (!place.email1.isEmpty() || !place.email2.isEmpty()) {
            email_layout.visibility = View.VISIBLE
            if (place.email1.isEmpty()) place_email1.visibility = View.GONE else place_email1.visibility = View.VISIBLE
            if (place.email2.isEmpty()) place_email2.visibility = View.GONE else place_email2.visibility = View.VISIBLE
        }
        val web_layout =  dialog.findViewById<LinearLayout>(R.id.show_place_contacts_web)
        place_web1.text = place.website1
        place_web2.text = place.website2
        if (!place.website1.isEmpty() && !place.website2.isEmpty()) {
            web_layout.visibility = View.VISIBLE
        } else  if (!place.website1.isEmpty() || !place.website2.isEmpty()) {
            web_layout.visibility = View.VISIBLE
            if (place.website1.isEmpty()) {
                place_web1.visibility = View.GONE
            } else {
                place_web1.visibility = View.VISIBLE
                place_web1.setOnLongClickListener{
                    openURL(Uri.parse(place.website1))
                    true
                }
            }
            if (place.website2.isEmpty()) {
                place_web2.visibility = View.GONE
            } else {
                place_web2.visibility = View.VISIBLE
                place_web2.setOnLongClickListener{
                    openURL(Uri.parse(place.website2))
                    true
                }
            }
        }

        if (phone_layout.visibility.equals(View.VISIBLE) || email_layout.visibility.equals(View.VISIBLE) || web_layout.visibility.equals(View.VISIBLE)) {
            dialog.findViewById<LinearLayout>(R.id.show_place_contacts).visibility = View.VISIBLE
        } else {
            dialog.findViewById<LinearLayout>(R.id.show_place_contacts).visibility = View.GONE
        }

        //for Workhours
        var any = false
        for (i in 1..7) {
            if (!place.workhours[i].isEmpty()) any = true
        }
        if (any) {
            dialog.findViewById<LinearLayout>(R.id.show_place_workhours).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.monday_hours).text = place.workhours.monday
            dialog.findViewById<TextView>(R.id.tuesday_hours).text = place.workhours.tuesday
            dialog.findViewById<TextView>(R.id.wednesday_hours).text = place.workhours.wednesday
            dialog.findViewById<TextView>(R.id.thursday_hours).text = place.workhours.thursday
            dialog.findViewById<TextView>(R.id.friday_hours).text = place.workhours.friday
            dialog.findViewById<TextView>(R.id.saturday_hours).text = place.workhours.saturday
            dialog.findViewById<TextView>(R.id.sunday_hours).text = place.workhours.sunday
        }

        if (place.images.isNotEmpty()) dialog.findViewById<LinearLayout>(R.id.show_place_images).visibility = View.VISIBLE
        val image_container = dialog.findViewById<RecyclerView>(R.id.recyler_view_images)
        image_container.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImagesAdapter(ArrayList(place.images.values), false,this)
        image_container.adapter = imageAdapter

        if(!place.approved) {
            if(place.desc.isEmpty()) dialog.findViewById<LinearLayout>(R.id.show_place_intro).visibility = View.GONE
            dialog.findViewById<LinearLayout>(R.id.show_place_review_score).visibility = View.GONE
            dialog.findViewById<LinearLayout>(R.id.show_place_reviews).visibility = View.GONE
        }
        dialog.show()
    }
    private fun openURL(uri : Uri){
        AlertDialog.Builder(activity)
            .setTitle("Open link")
            .setMessage("Are you sure you want to go to this link?")
            .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            })
            .setNegativeButton(android.R.string.no, null)
            .setIcon(R.drawable.ic_place_info_website_link)
            .setCancelable(false)
            .show()
    }

    override fun onItemLongClick(place: Place, placeId: String) {
        AlertDialog.Builder(activity)
            .setTitle("Make duplicate")
            .setMessage("Do you want to duplicate this place: "+place.title)
            .setPositiveButton("Yes", DialogInterface.OnClickListener { _, _ ->
                val intent = Intent(requireContext(), AddNewPlaceActivity::class.java)
                intent.putExtra("copy", true)
                intent.putExtra("place_id", placeId)
                startActivity(intent)
            })
            .setNegativeButton("No", null)
            .setIcon(R.drawable.ic_buildings_town)
            .setCancelable(false)
            .show()

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

    override fun onImageClick(position: Int, allImages: ArrayList<String>) {
        TODO("Not yet implemented")
    }

    override fun onBtnRemove(position: Int) {
        TODO("Not yet implemented")
    }
}










