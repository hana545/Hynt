package hr.project.hynt

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import hr.project.hynt.Adapters.ImagesAdapter
import hr.project.hynt.Adapters.PlacesAdapter
import hr.project.hynt.Adapters.PlacesManageAdapter
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Review
import hr.project.hynt.FirebaseDatabase.TagCategory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AdminManagePlacesFragment : Fragment(), PlacesManageAdapter.ItemClickListener, ImagesAdapter.ItemClickListener {
    var allPlaces = ArrayList<Place>()

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    lateinit var text_info : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_admin_manage_places, container, false)
        text_info = view.findViewById(R.id.fragment_info)
        val recyclerview = view.findViewById<RecyclerView>(R.id.place_recyclerView)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = GridLayoutManager(requireContext(), 2)

        val adapter = PlacesManageAdapter(allPlaces, "admin", this)
        recyclerview.adapter = adapter
        getAllPlaces(adapter)
        // Inflate the layout for this fragment
        return view
    }

    fun getAllPlaces(adapter: PlacesManageAdapter) {
        val places_query = db.getReference("places")
        places_query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlaces.clear()
                if (snapshot.exists()) {
                    for (places: DataSnapshot in snapshot.children) {
                        val place: Place? = places.getValue<Place>()
                        if (place != null && !place.approved && place.pending) {
                            allPlaces.add(place)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                text_info.visibility = if (allPlaces.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }

    override fun onPositiveButtonClick(id: String, place: Place) {
        AlertDialog.Builder(activity)
                .setTitle("Approve place")
                .setMessage("Are you sure you want to approve this place: " + place.title + "?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                    db.getReference("places").child(id).child("approved").setValue(true)
                    db.getReference("places").child(id).child("pending").setValue(false)
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_buildings_town)
                .setCancelable(false)
                .show()
    }

    override fun onNegativeButtonClick(id: String, placeName: String) {
        AlertDialog.Builder(activity)
                .setTitle("Reject place")
                .setMessage("Are you sure you want to reject this place: " + placeName + "?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                    db.getReference("places").child(id).child("approved").setValue(false)
                    db.getReference("places").child(id).child("pending").setValue(false)
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
        dialog.setContentView(R.layout.dialog_show_place_manage)

        dialog.findViewById<ImageView>(R.id.close_dialog).setOnClickListener { dialog.dismiss() }

        val place_title: TextView = dialog.findViewById<TextView>(R.id.show_place_title_data)
        val place_address: TextView = dialog.findViewById<TextView>(R.id.show_place_address_data)
        val place_description: TextView = dialog.findViewById<TextView>(R.id.show_place_description)
        val place_category: TextView = dialog.findViewById<TextView>(R.id.show_place_category)
        val place_user: TextView = dialog.findViewById<TextView>(R.id.show_place_user)

        place_title.text = place.title
        place_address.text = place.address
        place_description.text = place.desc
        if (!place.desc.isEmpty()) place_description.visibility = View.VISIBLE
        place_category.text = place.category
        db.getReference("users").child(place.authorID).child("username").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    place_user.text = snapshot.getValue<String>()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }
        })
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
        place_web1.setPaintFlags(place_web1.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
        place_web2.setPaintFlags(place_web2.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
        if (!place.website1.isEmpty() && !place.website2.isEmpty()) {
            web_layout.visibility = View.VISIBLE
            place_web1.setOnClickListener{
                openURL(place.website1)
            }
            place_web2.setOnClickListener{
                openURL(place.website2)
            }
        } else  if (!place.website1.isEmpty() || !place.website2.isEmpty()) {
            web_layout.visibility = View.VISIBLE
            if (place.website1.isEmpty()) {
                place_web1.visibility = View.GONE
            } else {
                place_web1.visibility = View.VISIBLE
                place_web1.setOnClickListener{
                    openURL(place.website1)
                }
            }
            if (place.website2.isEmpty()) {
                place_web2.visibility = View.GONE
            } else {
                place_web2.visibility = View.VISIBLE
                place_web2.setOnClickListener{
                    openURL(place.website2)
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

        ///for Images
        if (place.images.isNotEmpty()) dialog.findViewById<LinearLayout>(R.id.show_place_images).visibility = View.VISIBLE
        val image_container = dialog.findViewById<RecyclerView>(R.id.recyler_view_images)
        image_container.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImagesAdapter(ArrayList(place.images.values), false,this)
        image_container.adapter = imageAdapter


        ///buttons
        val btn_approve: ImageButton = dialog.findViewById(R.id.btn_show_place_approve)
        btn_approve.setOnClickListener {
            dialog.dismiss()
            onPositiveButtonClick(placeId, place)
        }
        val btn_delete: ImageButton = dialog.findViewById(R.id.btn_show_place_delete)
        btn_delete.setOnClickListener {
            dialog.dismiss()
            onNegativeButtonClick(placeId, place.title,)
        }
        dialog.show()
    }
    private fun openURL(uriString : String){
        AlertDialog.Builder(requireActivity())
            .setTitle("Open link")
            .setMessage("Are you sure you want open this link: "+uriString +"?")
            .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                var uri = uriString.toUri()
                if (!uriString.startsWith("http://")) uri = ("http://"+uriString).toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            })
            .setNegativeButton(android.R.string.no, null)
            .setIcon(R.drawable.ic_place_info_website_link)
            .setCancelable(false)
            .show()
    }

    override fun onItemLongClick(place: Place, placeId: String) {
        TODO("Not yet implemented")
    }

    override fun onImageClick(position: Int, allImages: ArrayList<String>) {
        TODO("Not yet implemented")
    }

    override fun onBtnRemove(position: Int) {
        TODO("Not yet implemented")
    }
}










