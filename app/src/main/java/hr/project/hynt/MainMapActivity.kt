package hr.project.hynt

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import hr.project.hynt.Adapters.PlacesAdapter
import hr.project.hynt.FirebaseDatabase.Place
import java.util.*


class MainMapActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, OnCameraTrackingChangedListener, PlacesAdapter.ItemClickListener {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var isInTrackingMode = false

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    var allPlaces = ArrayList<Place>()
    var allPlacesId = ArrayList<String>()

    private val PERMISSION_REQUEST_CODE_LOCATION =  102

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main_map)

        val recyclerview = findViewById<RecyclerView>(R.id.places_recycler_view)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val adapter = PlacesAdapter(allPlaces, allPlacesId, this)
        recyclerview.adapter = adapter
        getAllPlaces(adapter)
        ////set action bar
        setCustomActionBar()
        ////drawer for filter
        addDrawerFilter()
        ////bottom sheet - show locations
        addBottomSheet()
        ////map
        askLocationPermission()
        mapView = findViewById(R.id.mapView) as MapView
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

    }

    private fun setCustomActionBar() {
        this.supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        val customView: View = LayoutInflater.from(this).inflate(
                R.layout.action_bar, LinearLayout(
                this
        ), false
        )
        supportActionBar!!.customView = customView
        supportActionBar?.setBackgroundDrawable(
                ColorDrawable(
                        ContextCompat.getColor(
                                this,
                                R.color.black_blue
                        )
                )
        )

        val btn_user : LinearLayout = customView.findViewById<View>(R.id.btn_user_profile) as LinearLayout
        if (authUser != null) {
            val username : TextView = btn_user.findViewById(R.id.user_username) as TextView
            username.setText(authUser.displayName)
            val sh = this.getSharedPreferences("MySharedPref",  Context.MODE_PRIVATE)
            btn_user.setOnClickListener(View.OnClickListener {
                showBottomSheetDialogOptions(sh.getString("Role", "").toString())
            })
        } else {
            btn_user.visibility = View.GONE
        }
    }
    private fun addBottomSheet() {
        val mBottomSheetLayout : ConstraintLayout = findViewById(R.id.bottomSheet);
        val sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        val btn_toggle_locations: ImageButton = findViewById<ImageButton>(R.id.bottom_sheet_header)
        btn_toggle_locations.setOnClickListener(View.OnClickListener {
            if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                btn_toggle_locations.setImageResource(R.drawable.ic_expand_more)
            } else {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                btn_toggle_locations.setImageResource(R.drawable.ic_expand_less)
            }
        })
    }
    fun showBottomSheetDialogOptions(role : String) {

        val bottomSheetDialog = BottomSheetDialog(this)
        if(role == "admin") {
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_admin_options)
            val manage_places = bottomSheetDialog.findViewById<LinearLayout>(R.id.manage_places)
            manage_places!!.setOnClickListener(View.OnClickListener {
                val intent = Intent(this, AdminOptionsActivity::class.java)
                intent.putExtra("fragment", "places")
                bottomSheetDialog.dismiss()
                startActivity(intent)
            })
            val tags = bottomSheetDialog.findViewById<LinearLayout>(R.id.tags)
            tags!!.setOnClickListener(View.OnClickListener {
                val intent = Intent(this, AdminOptionsActivity::class.java)
                intent.putExtra("fragment", "tags")
                bottomSheetDialog.dismiss()
                startActivity(intent)
            })
            val categories = bottomSheetDialog.findViewById<LinearLayout>(R.id.categories)
            categories!!.setOnClickListener(View.OnClickListener {
                val intent = Intent(this, AdminOptionsActivity::class.java)
                intent.putExtra("fragment", "categories")
                bottomSheetDialog.dismiss()
                startActivity(intent)
            })
        } else {
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_users_options)
        }
        val add_place = bottomSheetDialog.findViewById<LinearLayout>(R.id.add_place)
        add_place!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, AddNewPlaceActivity::class.java)
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })
        val my_addresses = bottomSheetDialog.findViewById<LinearLayout>(R.id.myAddresses)
        my_addresses!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val my_reviews = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_reviews)
        my_reviews!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val my_places = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_places)
        my_places!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val settings = bottomSheetDialog.findViewById<LinearLayout>(R.id.settings)
        settings!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val signout = bottomSheetDialog.findViewById<LinearLayout>(R.id.signOut)
        signout!!.setOnClickListener(View.OnClickListener {
            logOut()
            bottomSheetDialog.dismiss();
        })
        bottomSheetDialog.show()
    }
    private fun logOut(){
        FirebaseAuth.getInstance().signOut()
        getSharedPreferences("MySharedPref",  Context.MODE_PRIVATE).edit().remove("Role").apply()
        val intent = Intent(this, LaunchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent)
        finish()
    }

    private fun addDrawerFilter() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        findViewById<View>(R.id.float_btn_search_filter_locations).setOnClickListener(View.OnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        })
        drawerLayout.setDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(view: View, v: Float) {}
            override fun onDrawerOpened(view: View) {
                supportActionBar!!.hide()
            }

            override fun onDrawerClosed(view: View) {
                supportActionBar!!.show()
            }

            override fun onDrawerStateChanged(i: Int) {}
        })
    }

    fun getAllPlaces(adapter: PlacesAdapter) {
        val places_query = db.getReference("places")
        places_query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlaces.clear()
                allPlacesId.clear()
                if (snapshot.exists()) {
                    for (places : DataSnapshot in snapshot.children) {
                        val place : Place? = places.getValue<Place>()
                        if (place != null) {
                            allPlaces.add(place)
                            allPlacesId.add(places.key.toString())
                            mapboxMap?.addMarker(
                                    MarkerOptions()
                                            .position(LatLng(place.lat, place.lng))
                                            .title(place.title))
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


    private fun askLocationPermission() {
        //ask for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE_LOCATION
            )
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
                Style.MAPBOX_STREETS
        ) { style -> enableLocationComponent(style) }


    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions =
                LocationComponentOptions.builder(this)
                    .elevation(5f)
                    .accuracyAlpha(.6f)
                    .accuracyColor(Color.BLUE)
                    .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            // Get an instance of the component
            locationComponent = mapboxMap!!.locationComponent

            // Activate with options
            locationComponent!!.activateLocationComponent(locationComponentActivationOptions)

            // Enable to make component visible
            locationComponent!!.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent!!.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent!!.renderMode = RenderMode.COMPASS

            // Add the camera tracking listener. Fires if the map camera is manually moved.
            locationComponent!!.addOnCameraTrackingChangedListener(this)

            if (mapboxMap!!.locationComponent.lastKnownLocation == null){

                noLocation(false)
                AlertDialog.Builder(this)
                        .setTitle("Can't access your location")
                        .setMessage("Please turn on your device location and refresh app")
                        .setPositiveButton("Refresh", DialogInterface.OnClickListener { _, _ ->
                            finish()
                            overridePendingTransition(0,0)
                            startActivity(intent)
                            overridePendingTransition(0,0)
                            })
                        .setNegativeButton("Exit", DialogInterface.OnClickListener { _, _ ->
                            finish()
                        })
                        .setIcon(R.drawable.ic_map_alert)
                        .setCancelable(false)
                        .show()

            } else {
                noLocation(true)
            }
            //button to move back to location
            findViewById<View>(R.id.float_btn_back_to_location).setOnClickListener {
                var mylat = mapboxMap!!.locationComponent.lastKnownLocation!!.latitude
                var mylng = mapboxMap!!.locationComponent.lastKnownLocation!!.longitude
                val position = CameraPosition.Builder()
                        .target(LatLng(mylat, mylng))
                        .zoom(15.0) // Sets the zoom
                        .build()
                mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1500)
                //delay tracking so it can zoom in location
                Handler(Looper.getMainLooper()).postDelayed({
                    locationComponent!!.cameraMode = CameraMode.TRACKING
                }, 2000)
                isInTrackingMode = true
            }
        } else {
            noLocation(false)
        }
    }

    override fun onCameraTrackingDismissed() {
        isInTrackingMode = false
    }

    override fun onCameraTrackingChanged(currentMode: Int) {
        // Empty on purpose
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_LOCATION -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                enableLocationComponent(mapboxMap!!.style!!)
            } else {
                // Permission Denied
                AlertDialog.Builder(this)
                        .setTitle("Denied location")
                        .setMessage("You have not granted permission to access your location! You need to change that in settings before using this app. ")
                        .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _, _ ->
                            finish()
                        })
                        .setIcon(R.drawable.ic_map_alert)
                        .setCancelable(false)
                        .show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    fun noLocation(access : Boolean){
        if (access){
            findViewById<View>(R.id.float_btn_back_to_location).visibility = View.VISIBLE
            findViewById<View>(R.id.float_btn_search_filter_locations).visibility = View.VISIBLE
            findViewById<View>(R.id.float_btn_hint).visibility = View.VISIBLE
            findViewById<View>(R.id.bottomSheet).visibility = View.VISIBLE
        } else {
            //sets camera on the center of the world
            val position = CameraPosition.Builder()
                    .target(LatLng(40.52, 34.34))
                    .zoom(1.5) // Sets the zoom
                    .build()
            mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 200)
            findViewById<View>(R.id.float_btn_back_to_location).visibility = View.GONE
            findViewById<View>(R.id.float_btn_search_filter_locations).visibility = View.GONE
            findViewById<View>(R.id.float_btn_hint).visibility = View.GONE
            findViewById<View>(R.id.bottomSheet).visibility = View.GONE
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, "This app needs location permissions in order to show its functionality", Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(p0: Boolean) {

    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onItemClick(id: String) {
        Toast.makeText(this, "clicked place no:$id", Toast.LENGTH_SHORT).show()
    }

}