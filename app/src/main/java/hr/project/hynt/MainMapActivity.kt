package hr.project.hynt

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
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
import hr.project.hynt.Adapters.ReviewsAdapter
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Review
import java.util.*
import java.text.SimpleDateFormat


class MainMapActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, OnCameraTrackingChangedListener, PlacesAdapter.ItemClickListener, ReviewsAdapter.ItemClickListener {

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

        Log.w("MapActivity", "onCreate")
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main_map)

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
                R.layout.layout_action_bar, LinearLayout(
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
            val intent = Intent(this, UserOptionsActivity::class.java)
            intent.putExtra("fragment", "addresses")
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })
        val my_reviews = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_reviews)
        my_reviews!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, UserOptionsActivity::class.java)
            intent.putExtra("fragment", "reviews")
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })
        val my_places = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_places)
        my_places!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, UserOptionsActivity::class.java)
            intent.putExtra("fragment", "places")
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })
        val settings = bottomSheetDialog.findViewById<LinearLayout>(R.id.settings)
        settings!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, UserSettingsActivity::class.java)
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })
        bottomSheetDialog.show()
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

    fun getAllPlaces(adapter: PlacesAdapter, dialog : Dialog) {
        val places_query = db.getReference("places").orderByChild("approved").equalTo(true)
        places_query.addValueEventListener(object: ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlaces.clear()
                allPlacesId.clear()
                if (snapshot.exists()) {
                    Log.w("MapActivity", "Loading all places")
                    for (places : DataSnapshot in snapshot.children) {
                        val place : Place? = places.getValue<Place>()
                        if (place != null) {
                            allPlaces.add(place)
                            allPlacesId.add(places.key.toString())
                        }
                    }

                    Log.w("MapActivity", "Loading all places --done")
                    showMarkers()
                }
                dialog.dismiss()
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }
    private fun showMarkers() {
        for (place : Place in allPlaces) {
            Log.w("MapActivity", "Showing markers - "+place.title)
            mapboxMap?.addMarker(
                    MarkerOptions()
                            .position(LatLng(place.lat, place.lng))
                            .title(place.title))
        }
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
        val loading_dialog = Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loading_dialog.setContentView(R.layout.activity_splash_screen)
        loading_dialog.show()
        if (!isNetworkAvailable(this)) loading_dialog.findViewById<TextView>(R.id.splash_internet_conn).visibility = View.VISIBLE

        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
                Style.MAPBOX_STREETS
        ) { style -> enableLocationComponent(style) }

        ///recyler view for places
        val recyclerview = findViewById<RecyclerView>(R.id.places_recycler_view)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val adapter = PlacesAdapter(allPlaces, allPlacesId, this)
        recyclerview.adapter = adapter
        getAllPlaces(adapter, loading_dialog)

        Log.w("MapActivity", "Map ready")


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
        Log.w("MapActivity", "onStart")
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {

        Log.w("MapActivity", "onResume")
        setCustomActionBar()
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

        Log.w("MapActivity", "onDestroy")
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onItemClick(position: Int, place: Place, id: String, score: Int, allReviews : List<Review>, hasRev: Boolean, reviewID : String, review : Review){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_show_place)

        val place_title : TextView = dialog.findViewById<TextView>(R.id.show_place_title_data)
        place_title.text = place.title
        val place_description : TextView = dialog.findViewById<TextView>(R.id.show_place_description)
        place_description.text = place.desc
        if (place.desc.isEmpty()) place_description.visibility = View.GONE
        dialog.findViewById<ImageView>(R.id.close_dialog).setOnClickListener { dialog.dismiss() }

        //for reviewing
        if (authUser != null) {
            val myReview: LinearLayout = dialog.findViewById<LinearLayout>(R.id.myReview_layout)
            val btn_leaveReview : Button = dialog.findViewById<Button>(R.id.btn_review)
            val btn_saveReview: Button = dialog.findViewById<Button>(R.id.btn_save_review)
            val btn_cancelReview: Button = dialog.findViewById<Button>(R.id.btn_cancel_review)
            btn_leaveReview.visibility = View.VISIBLE

            val review_text: EditText = dialog.findViewById<EditText>(R.id.show_place_myReviewText)
            val review_score: TextView = dialog.findViewById<TextView>(R.id.show_place_myReviewScore_summary)
            val list_stars = ArrayList<ImageView>()
            val myStar1: ImageView = dialog.findViewById<ImageView>(R.id.my_star1)
            val myStar2: ImageView = dialog.findViewById<ImageView>(R.id.my_star2)
            val myStar3: ImageView = dialog.findViewById<ImageView>(R.id.my_star3)
            val myStar4: ImageView = dialog.findViewById<ImageView>(R.id.my_star4)
            val myStar5: ImageView = dialog.findViewById<ImageView>(R.id.my_star5)
            list_stars.add(myStar1)
            list_stars.add(myStar2)
            list_stars.add(myStar3)
            list_stars.add(myStar4)
            list_stars.add(myStar5)
            var checked_stars: Int = 0

            if (hasRev){
                btn_leaveReview.text = "Edit your review"
                review_text.setText(review.txt)
                checked_stars = review.stars
                review_score.text = "You rated this place with "+checked_stars
                review_score.text = review_score.text.toString() + if (checked_stars > 1)  " stars" else " star"
                check_stars(review.stars-1, list_stars)
                btn_saveReview.text = "Update review"
            }

            myStar1.setOnClickListener {
                checked_stars = 1
                check_stars(0, list_stars)
                review_score.text  = "Thanks you are rating this with 1 star"
            }
            myStar2.setOnClickListener {
                checked_stars = 2
                check_stars(1, list_stars)
                review_score.text  = "Thanks you are rating this with 2 stars"
            }
            myStar3.setOnClickListener {
                checked_stars = 3
                check_stars(2, list_stars)
                review_score.text  = "Thanks you are rating this with 3 stars"
            }
            myStar4.setOnClickListener {
                checked_stars = 4
                check_stars(3, list_stars)
                review_score.text  = "Thanks you are rating this with 4 stars"
            }
            myStar5.setOnClickListener {
                checked_stars = 5
                check_stars(4, list_stars)
                review_score.text  = "Thanks you are rating this with 5 stars"
            }
            btn_leaveReview.setOnClickListener {
                myReview.visibility = View.VISIBLE
                btn_leaveReview.visibility = View.GONE

            }
            btn_cancelReview.setOnClickListener {
                myReview.visibility = View.GONE
                btn_leaveReview.visibility = View.VISIBLE
                if (hasRev){
                    btn_leaveReview.text = "Edit your review"
                    review_text.setText(review.txt)
                    checked_stars = review.stars
                    review_score.text = "You rated this place with "+checked_stars
                    review_score.text = review_score.text.toString() + if (checked_stars > 1)  " stars" else " star"
                    check_stars(review.stars-1, list_stars)
                    btn_saveReview.text = "Update review"
                } else {
                    btn_leaveReview.text = "Leave a review"
                    check_stars(-1, list_stars)
                    review_score.text  = ""
                    review_text.setText("")
                    btn_saveReview.text = "Send review"
                }
            }


            btn_saveReview.setOnClickListener {
                if (checked_stars > 0 && checked_stars < 6) {
                    val reviewU = Review(Calendar.getInstance().time, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), review_text.text.toString(), checked_stars, place.title, id)
                    val reviewP = Review( Calendar.getInstance().time, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), review_text.text.toString(), checked_stars, authUser.displayName!!, authUser.uid)
                    db.getReference("places").child(id).child("reviews").child(authUser!!.uid).setValue(reviewP)
                    db.getReference("users").child(authUser.uid).child("reviews").child(id).setValue(reviewU)
                    dialog.dismiss()
                    show_info_dialog("Thank you for reviewing "+place.title+"!", true)


                }
            }
        }

        //for Review
        val list_stars = ArrayList<ImageView>()
        val star1: ImageView = dialog.findViewById<ImageView>(R.id.star1)
        val star2: ImageView = dialog.findViewById<ImageView>(R.id.star2)
        val star3: ImageView = dialog.findViewById<ImageView>(R.id.star3)
        val star4: ImageView = dialog.findViewById<ImageView>(R.id.star4)
        val star5: ImageView = dialog.findViewById<ImageView>(R.id.star5)
        list_stars.add(star1)
        list_stars.add(star2)
        list_stars.add(star3)
        list_stars.add(star4)
        list_stars.add(star5)

        check_stars(score-1, list_stars)

        //for Addres and Category
        val place_address : TextView = dialog.findViewById<TextView>(R.id.show_place_address_data)
        place_address.text = place.address
        dialog.findViewById<ImageButton>(R.id.show_place_btn_show_on_map).setOnClickListener {
            setFocusOnMap(place.lat, place.lng)
            dialog.dismiss()
        }
        val place_category : TextView = dialog.findViewById<TextView>(R.id.show_place_category)
        place_category.text = place.category

        // for Tags
        for (tag in place.tags){
            val chip = this.layoutInflater.inflate(R.layout.view_chips_buttons, null, false) as Chip
            chip.text = tag
            chip.isEnabled = false
            dialog.findViewById<FlexboxLayout>(R.id.show_place_tags_chip_group).addView(chip)
        }
        //for Contacts
        val place_phone : TextView = dialog.findViewById<TextView>(R.id.show_place_phone)
        val place_email : TextView = dialog.findViewById<TextView>(R.id.show_place_email)
        val place_web : TextView = dialog.findViewById<TextView>(R.id.show_place_web)

        val phone_layout =  dialog.findViewById<LinearLayout>(R.id.show_place_contacts_phone)
        if (!place.phone1.isEmpty() && !place.phone2.isEmpty()) {
            place_phone.text = place.phone1 + '\n' + place.phone2
            phone_layout.visibility = View.VISIBLE
        } else if (!place.phone1.isEmpty() || !place.phone2.isEmpty()){
            place_phone.text = place.phone1 + place.phone2
            phone_layout.visibility = View.VISIBLE
        }
        val email_layout =  dialog.findViewById<LinearLayout>(R.id.show_place_contacts_email)
        if (!place.email1.isEmpty() && !place.email2.isEmpty()) {
            place_email.text = place.email1 + '\n' + place.email2
            email_layout.visibility = View.VISIBLE
        } else if (!place.email1.isEmpty() || !place.email2.isEmpty()) {
            place_email.text = place.email1 + place.email2
            email_layout.visibility = View.VISIBLE
        }
        val web_layout =  dialog.findViewById<LinearLayout>(R.id.show_place_contacts_web)
        if (!place.website1.isEmpty() && !place.website2.isEmpty()) {
            place_web.text = place.website1 + '\n' + place.website2
            web_layout.visibility = View.VISIBLE
        } else  if (!place.website1.isEmpty() || !place.website2.isEmpty()) {
            place_web.text = place.website1 + place.website2
            web_layout.visibility = View.VISIBLE
        }
        if (phone_layout.visibility.equals(View.VISIBLE) || email_layout.visibility.equals(View.VISIBLE) || web_layout.visibility.equals(View.VISIBLE)){
            dialog.findViewById<LinearLayout>(R.id.show_place_contacts).visibility = View.VISIBLE
        } else {
            dialog.findViewById<LinearLayout>(R.id.show_place_contacts).visibility = View.GONE
        }

        //for Workhours
        var any = false
        for(i in 1..7){
            if (!place.workhours[i].isEmpty()) any = true
        }
        if (!any){
            dialog.findViewById<LinearLayout>(R.id.show_place_workhours).visibility = View.GONE
        } else {
            dialog.findViewById<TextView>(R.id.monday_hours).text = place.workhours.monday
            dialog.findViewById<TextView>(R.id.tuesday_hours).text = place.workhours.tuesday
            dialog.findViewById<TextView>(R.id.wednesday_hours).text = place.workhours.wednesday
            dialog.findViewById<TextView>(R.id.thursday_hours).text = place.workhours.thursday
            dialog.findViewById<TextView>(R.id.friday_hours).text = place.workhours.friday
            dialog.findViewById<TextView>(R.id.saturday_hours).text = place.workhours.saturday
            dialog.findViewById<TextView>(R.id.sunday_hours).text = place.workhours.sunday
        }

        ///for Reviews
        val review_container = dialog.findViewById<RecyclerView>(R.id.show_place_review_list)
        review_container.layoutManager = LinearLayoutManager(baseContext)

        val adapter = ReviewsAdapter(allReviews, null,"other", this)
        review_container.adapter = adapter

        dialog.show()
    }

    private fun setFocusOnMap(lat: Double, lng: Double) {
        val position = CameraPosition.Builder()
                .target(LatLng(lat, lng))
                .zoom(16.0) // Sets the zoom
                .build()
        mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 200)
       // mapboxMap!!.addMarker(MarkerOptions().position(LatLng(lat, lng)))
    }

    private fun show_info_dialog(text : String, success : Boolean){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        if (success) {
            dialog.setContentView(R.layout.dialog_info_success)
        } else {
            dialog.setContentView(R.layout.dialog_info_failed)
        }
        dialog.findViewById<TextView>(R.id.info_text).text = text
        dialog.findViewById<Button>(R.id.btn_continue).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun check_stars(n: Int, list_stars : ArrayList<ImageView>) {
        for (i in 0..4) {
            if (i > n) {
                list_stars[i].setImageResource(R.drawable.ic_star_review_off)
                continue
            }
            list_stars[i].setImageResource(R.drawable.ic_star_review_on)
        }
    }
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    override fun onItemClick(review: Review, reviewID : String) {
        //do nothing
    }

}