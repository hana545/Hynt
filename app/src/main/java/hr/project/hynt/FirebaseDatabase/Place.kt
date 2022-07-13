package hr.project.hynt.FirebaseDatabase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.util.*

data class Place(val title: String = "",
                 val address: String = "",
                 val lat: Double = 0.0,
                 val lng: Double = 0.0,
                 val desc : String = "",
                 val phone1 : String = "",
                 val phone2 : String = "",
                 val email1 : String = "",
                 val email2 : String = "",
                 val website1 : String = "",
                 val website2 : String = "",
                 val workhours: Workhour = Workhour(),
                 val category : String = "",
                 val tags : ArrayList<String> = ArrayList<String>(),
                 val reviews : HashMap<String, Review> = HashMap<String, Review>(),
                 val authorID: String = "",
                 val approved: Boolean = false

) {



}