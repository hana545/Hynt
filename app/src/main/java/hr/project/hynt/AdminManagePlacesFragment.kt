package hr.project.hynt

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import hr.project.hynt.Adapters.PlacesAdapter
import hr.project.hynt.Adapters.PlacesManageAdapter
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.TagCategory
import java.util.*

class AdminManagePlacesFragment : Fragment(), PlacesManageAdapter.ItemClickListener {
    var allPlaces = ArrayList<Place>()
    var allPlacesId = ArrayList<String>()

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_admin_manage_places, container, false)
        val recyclerview = view.findViewById<RecyclerView>(R.id.place_recyclerView)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = GridLayoutManager(requireContext(),2)

        val adapter = PlacesManageAdapter(allPlaces, allPlacesId, this, this)
        recyclerview.adapter = adapter
        getAllPlaces(adapter)
        // Inflate the layout for this fragment
        return view
    }

    fun getAllPlaces(adapter: PlacesManageAdapter) {
        val places_query = db.getReference("places")
        places_query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlaces.clear()
                allPlacesId.clear()
                if (snapshot.exists()) {
                    for (places : DataSnapshot in snapshot.children) {
                        val place : Place? = places.getValue<Place>()
                        if (place != null && !place.approved) {
                            allPlaces.add(place)
                            allPlacesId.add(places.key.toString())
                        }
                    }
                    adapter.notifyDataSetChanged()

                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }

    override fun onButtonClick(id: String) {
        AlertDialog.Builder(activity)
                .setTitle("Approve place")
                .setMessage("Are you sure you want to approve this place?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                    db.getReference("places").child(id).child("approved").setValue(true)
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_buildings_town)
                .setCancelable(false)
                .show()
    }

    /////////////////WIP
    override fun onItemClick(place : Place){
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_show_place_manage)

        val place_title : TextView = dialog.findViewById<TextView>(R.id.show_place_title_data)
        val place_address : TextView = dialog.findViewById<TextView>(R.id.show_place_address_data)
        val place_description : TextView = dialog.findViewById<TextView>(R.id.show_place_description_data)

        place_title.text = place.title
        place_address.text = place.address
        place_description.text = place.desc

        val btn_approve : ImageButton = dialog.findViewById(R.id.btn_show_place_approve)
        btn_approve.setOnClickListener {
            //////////WIP
                dialog.dismiss()
            }
        dialog.show()
    }


}