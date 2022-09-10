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
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.*
import hr.project.hynt.Adapters.ImagesAdapter
import hr.project.hynt.Adapters.PlacesAdapter
import hr.project.hynt.Adapters.ReviewsAdapter
import hr.project.hynt.Adapters.ViewPagerAdapter
import hr.project.hynt.FirebaseDatabase.Address
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Review
import java.lang.Math.*
import java.text.SimpleDateFormat
import java.util.*


class MainMapActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, OnCameraTrackingChangedListener, PlacesAdapter.ItemClickListener, ReviewsAdapter.ItemClickListener, ImagesAdapter.ItemClickListener {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var isInTrackingMode = false
    private var myCords = LatLng(0.0,0.0)

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    lateinit var loadingDialog : Dialog

    var allPlaces = ArrayList<Place>()
    var allPlacesId = ArrayList<String>()
    var hint = false
    lateinit var placesRecyclerview : RecyclerView
    val placesAdapter = PlacesAdapter(allPlaces,this)

    var sort = 0
    var allCategories = ArrayList<String>()
    var allTags = ArrayList<String>()
    var allCheckedCategories = ArrayList<String>()
    var allCheckedTags = ArrayList<String>()
    var range = 10
    var workhourOptions = arrayOf(true, true, true)
    var filterCoords = LatLng(0.0,0.0)
    var aroundMyLocation = true

    var allMyAddresses = ArrayList<String>()
    var allMyAddressesCoordinates = ArrayList<LatLng>()

    private var locationManager : LocationManager? = null
    private val PERMISSION_REQUEST_CODE_LOCATION =  102

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MapActivity", "onCreate")
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main_map)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_NETWORK_STATE),
                PERMISSION_REQUEST_CODE_LOCATION
            )
        }

        loadingDialog = Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingDialog.setContentView(R.layout.activity_splash_screen)
        ////set action bar
        setCustomActionBar()
        ////bottom sheet - show locations
        addBottomSheet()
        ////map
        askLocationPermission()
        // Create persistent LocationManager reference
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        mapView = findViewById(R.id.mapView) as MapView
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

    }
    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            refreshMyCoordinates(location.latitude,location.longitude)
            Log.d("MapActivity", "New location: " + location.longitude + ":" + location.latitude)
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun refreshMyCoordinates(latitude: Double, longitude: Double) {
        myCords = LatLng(latitude, longitude)
        if (aroundMyLocation) {
            if (getDistance(myCords, filterCoords) > 1) getAllPlaces()
            filterCoords = myCords
        }
        if (allMyAddresses.isNotEmpty()) allMyAddressesCoordinates[0] = myCords
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

    private fun getAllTags() {
        var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
        db.getReference("tags").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTags.clear()
                if (snapshot.exists()) {
                    for (tags : DataSnapshot in snapshot.children) {
                        val tag = tags.getValue<String>()
                        if (tag != null) {
                            allTags.add(tag)
                        }
                    }
                    allTags.sort()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }
        })
    }
    private fun getallCategories() {
        var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
        db.getReference("categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    allCategories.clear()
                    for (categories: DataSnapshot in snapshot.children) {
                        val category = categories.getValue<String>()
                        if (category != null) {
                            allCategories.add(category)
                        }
                    }
                    allCategories.sort()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }
    private fun getAllMyAddresses(adapter : ArrayAdapter<String>) {
        if (authUser != null) {
            var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
            db.getReference("users").child(authUser.uid).child("addresses")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        allMyAddresses.clear()
                        allMyAddressesCoordinates.clear()
                        allMyAddresses.add("My location")
                        allMyAddressesCoordinates.add(myCords)
                        filterCoords = allMyAddressesCoordinates[0]
                        if (snapshot.exists()) {
                            for (addresses: DataSnapshot in snapshot.children) {
                                val address = addresses.getValue<Address>()
                                if (address != null) {
                                    allMyAddresses.add(address.name)
                                    allMyAddressesCoordinates.add(LatLng(address.lat, address.lng))
                                }
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w("Database Error", "Failed to read value.", error.toException())
                    }

                })
        } else {
            allMyAddresses.add("My location")
            allMyAddressesCoordinates.add(myCords)
            filterCoords = allMyAddressesCoordinates[0]
            adapter.notifyDataSetChanged()
        }
    }

    private fun addDrawerFilter() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        findViewById<View>(R.id.float_btn_search_filter_locations).setOnClickListener(View.OnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        })
        val sortSpinner = findViewById<Spinner>(R.id.filter_sort_spinner)
        val sortAdapter : ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Distance", "Rating (High-Low)", "Rating (Low-High)", "Title (A-Z)", "Title (Z-A)"))
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.adapter = sortAdapter

        getAllTags()
        getallCategories()
        val addressSpinner = findViewById<Spinner>(R.id.filter_address_spinner)
        val addressAdapter : ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, allMyAddresses)
        addressAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressSpinner.adapter = addressAdapter
        addressSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                filterCoords = allMyAddressesCoordinates[p2]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        })
        getAllMyAddresses(addressAdapter)

        val btn_workhour_none = findViewById<CheckBox>(R.id.checkBox_none) //workhour = 2
        val btn_workhour_opened = findViewById<CheckBox>(R.id.checkBox_opened) //workhour = 1
        val btn_workhour_closed = findViewById<CheckBox>(R.id.checkBox_closed) //workhour = 0

        btn_workhour_none.setOnClickListener {
            if (!btn_workhour_opened.isChecked && !btn_workhour_closed.isChecked) {
                btn_workhour_opened.isChecked = true
                btn_workhour_closed.isChecked = true
            }
        }
        btn_workhour_opened.setOnClickListener {
            if (!btn_workhour_closed.isChecked && !btn_workhour_opened.isChecked) btn_workhour_none.isChecked = true
        }

        btn_workhour_closed.setOnClickListener {
            if (!btn_workhour_closed.isChecked && !btn_workhour_opened.isChecked) btn_workhour_none.isChecked = true
        }


        drawerLayout.setDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(view: View, v: Float) {

            }

            override fun onDrawerOpened(view: View) {
                allMyAddressesCoordinates[0] = myCords
                findViewById<Button>(R.id.filter_btn_search).setOnClickListener {
                    hint = false
                    placesAdapter.setHint(false)

                    workhourOptions[0] = btn_workhour_closed.isChecked
                    workhourOptions[1] = btn_workhour_opened.isChecked
                    workhourOptions[2] = btn_workhour_none.isChecked

                    sort = sortSpinner.selectedItemId.toInt()
                    getAllPlaces()

                    if (addressSpinner.selectedItemPosition > 0){
                        aroundMyLocation = false
                        mapboxMap!!.addMarker(MarkerOptions().position(filterCoords)
                                .title(addressSpinner.selectedItem.toString())
                                .icon(IconFactory.getInstance(this@MainMapActivity).fromResource(R.drawable.map_default_map_marker)))
                    }

                    setFocusOnMap(filterCoords.latitude, filterCoords.longitude)
                    placesRecyclerview.layoutManager?.scrollToPosition(0)
                    drawerLayout.closeDrawer(GravityCompat.END)
                }
                findViewById<Button>(R.id.filter_btn_clear_filter).setOnClickListener {
                    sort = 0
                    sortSpinner.setSelection(0)
                    findViewById<ChipGroup>(R.id.filter_category_chip_group).removeAllViews()
                    allCheckedCategories.clear()
                    findViewById<ChipGroup>(R.id.filter_tag_chip_group).removeAllViews()
                    allCheckedTags.clear()
                    addressSpinner.setSelection(0)
                    filterCoords = myCords
                    aroundMyLocation = true
                    range = 10
                    findViewById<TextView>(R.id.filter_range_data_label).text = range.toString() + " km"
                    workhourOptions[0] = true
                    workhourOptions[1] = true
                    workhourOptions[2] = true
                    btn_workhour_closed.isChecked = true
                    btn_workhour_opened.isChecked = true
                    btn_workhour_none.isChecked = true
                    hint = false
                    placesAdapter.setHint(false)
                    setFocusOnMap(myCords.latitude, myCords.longitude)
                    getAllPlaces()
                }
                findViewById<Button>(R.id.filter_category).setOnClickListener {
                    showDialogFilterAdd("Categories", allCheckedCategories, allCategories, findViewById<ChipGroup>(R.id.filter_category_chip_group))
                }
                findViewById<Button>(R.id.filter_tag).setOnClickListener {
                    showDialogFilterAdd("Tags", allCheckedTags, allTags, findViewById<ChipGroup>(R.id.filter_tag_chip_group))
                }

                findViewById<TextView>(R.id.filter_range_data_label).text = range.toString() + " km"
                findViewById<Button>(R.id.filter_range).setOnClickListener {
                    val dialog = Dialog(this@MainMapActivity)
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog.setCancelable(false)
                    dialog.setContentView(R.layout.dialog_filter_range)
                    dialog.findViewById<TextView>(R.id.filter_add_title).text = "Set range"
                    dialog.findViewById<TextView>(R.id.filter_range_data_label).text = "Range:" + range.toString() + "km"
                    dialog.findViewById<SeekBar>(R.id.filter_range_seek_bar).progress = range
                    var tmp_range = range
                    dialog.findViewById<SeekBar>(R.id.filter_range_seek_bar).setOnSeekBarChangeListener(object :
                            SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seek: SeekBar,
                                                       progress: Int, fromUser: Boolean) {
                            tmp_range = seek.progress
                            dialog.findViewById<TextView>(R.id.filter_range_data_label).text = "Range:" + tmp_range.toString() + "km"
                            // write custom code for progress is changed
                        }

                        override fun onStartTrackingTouch(seek: SeekBar) {
                            // write custom code for progress is started
                        }

                        override fun onStopTrackingTouch(seek: SeekBar) {
                            // write custom code for progress is stopped

                        }
                    })
                    dialog.findViewById<Button>(R.id.filter_btn_save_add).setOnClickListener {
                        range = tmp_range
                        findViewById<TextView>(R.id.filter_range_data_label).text = range.toString() + " km"
                        dialog.dismiss()

                    }
                    dialog.findViewById<Button>(R.id.filter_btn_cancel_add).setOnClickListener {
                        dialog.dismiss()
                    }


                    dialog.show()
                }
            }

            override fun onDrawerClosed(view: View) {

            }

            override fun onDrawerStateChanged(i: Int) {
            }
        })
    }

    fun showDialogFilterAdd(title : String, checkedData : ArrayList<String>, data : ArrayList<String>, chipGroup: ChipGroup){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_filter_add)

        val filterChipGroup = dialog.findViewById<ChipGroup>(R.id.filter_add_chip_group)
        dialog.findViewById<TextView>(R.id.filter_add_title).text = title

        val allChipsID = ArrayList<Int>()
        dialog.findViewById<Button>(R.id.filter_btn_save_add).setOnClickListener {
            chipGroup.removeAllViews()
            val chips = filterChipGroup.checkedChipIds
            checkedData.clear()
            for (id in chips){
                val chip_text = dialog.findViewById<Chip>(id).text
                checkedData.add(chip_text as String)
                val chip = this.layoutInflater.inflate(R.layout.view_chips_buttons, null, false) as Chip
                chip.text = chip_text
                chip.id = View.generateViewId()
                chip.isEnabled = false
                chipGroup.addView(chip)
            }
            dialog.dismiss()

        }
        dialog.findViewById<Button>(R.id.filter_btn_cancel_add).setOnClickListener {
            dialog.dismiss()
        }
        for (text in data){
             val chip = this.layoutInflater.inflate(R.layout.view_chips_buttons, null, false) as Chip
            chip.text = text
            for (checked in checkedData){
                if (checked == text) chip.isChecked = true
            }
            chip.id = View.generateViewId()
            allChipsID.add(chip.id)
            filterChipGroup.addView(chip)
        }
        dialog.findViewById<RadioButton>(R.id.filter_btn_check_all).setOnClickListener {
            if (dialog.findViewById<RadioButton>(R.id.filter_btn_check_all).isChecked) {
                for (id in allChipsID){
                    dialog.findViewById<Chip>(id).isChecked = true
                }

            }
        }
        dialog.findViewById<RadioButton>(R.id.filter_btn_check_none).setOnClickListener {
            if (dialog.findViewById<RadioButton>(R.id.filter_btn_check_none).isChecked) {
                for (id in allChipsID){
                    dialog.findViewById<Chip>(id).isChecked = false
                }

            }
        }
        dialog.show()
    }

    fun getAllPlaces() {

        val places_query = db.getReference("places").orderByChild("approved").equalTo(true)
        places_query.addValueEventListener(object: ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlaces.clear()
                allPlacesId.clear()
                removeMarkers()
                if (snapshot.exists()) {
                    Log.d("MapActivity", "Loading all places")
                    for (places : DataSnapshot in snapshot.children) {
                        val place : Place? = places.getValue<Place>()
                        if (place != null) {
                            Log.d("MapActivity", "Found Place : "+place.title)
                            place.distance = getDistance(filterCoords, LatLng(place.lat,place.lng))
                            var hasTag = false
                            for (tag in place.tags){
                                if (tag in allCheckedTags) hasTag = true
                            }
                            val opened = placesAdapter.checkWorkhour(place.workhours)

                            if ((place.category in allCheckedCategories || allCheckedCategories.isEmpty()) && (hasTag || allCheckedTags.isEmpty()) && place.distance <= range && ((workhourOptions[0] && opened == 0) || (workhourOptions[1] && opened == 1) || (workhourOptions[2] && opened == 2) )) {

                                if(!place.reviews.isEmpty()){
                                    var score = 0
                                    place.reviews.forEach { _, rev ->
                                        score += rev.stars
                                    }
                                    place.rating = (score.toFloat() / place.reviews.size).toDouble()
                                }
                                allPlaces.add(place)
                                allPlacesId.add(places.key.toString())
                            }
                        }
                    }
                    //sorting
                    var tmpPlaces = ArrayList<Place>()
                    when (sort){
                        //by distance
                        0 -> {
                            tmpPlaces = ArrayList(allPlaces.sortedWith(compareBy({ it.distance })))
                        }
                        //by rating (high - low)
                        1 -> {
                            tmpPlaces = ArrayList(allPlaces.sortedWith(compareBy({ it.rating })).reversed())
                        }
                        //by rating (low - high)
                        2 -> {
                            tmpPlaces = ArrayList(allPlaces.sortedWith(compareBy({ it.rating })))
                        }
                        //by A-Z
                        3 -> {
                            tmpPlaces = ArrayList(allPlaces.sortedWith(compareBy({ it.title })))
                        }
                        //by Z-A
                        4 -> {
                            tmpPlaces = ArrayList(allPlaces.sortedWith(compareBy({ it.title })).reversed())
                        }

                    }
                    //var tmpPlaces = ArrayList(allPlaces.sortedWith(compareBy({ it.distance })))
                    allPlaces.clear()
                    allPlaces.addAll(tmpPlaces)

                    //if hint clicked
                    if (hint and !allPlaces.isEmpty()){
                        val index = Random().nextInt(allPlaces.size)
                        val selected = allPlaces[index]
                        allPlaces.removeAt(index)
                        allPlaces.add(0, selected)
                        placesAdapter.setHint(true)
                        placesAdapter.setHintDialog(true)
                        setFocusOnMap(selected.lat, selected.lng)
                     }

                    findViewById<TextView>(R.id.places_txtInfo).visibility = if (!allPlaces.isEmpty()) View.GONE else View.VISIBLE


                    Log.d("MapActivity", "Loading all places --done")
                    showMarkers()
                }
                loadingDialog.dismiss()
                placesAdapter.notifyDataSetChanged()

            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }
    private fun showMarkers() {
        for (place : Place in allPlaces) {
            mapboxMap?.addMarker(
                    MarkerOptions()
                            .position(LatLng(place.lat, place.lng))
                            .title(place.title))
        }
    }
    private fun removeMarkers() {
        for (marker : Marker in mapboxMap?.markers!!) {
            if(marker.position != filterCoords) mapboxMap?.removeMarker(marker)
        }
    }

    private fun getDistance(locCoords : LatLng, placeCoords : LatLng) : Double{
        val r = 6371
        val dlat = toRadians(placeCoords.latitude-locCoords.latitude)
        val dlng = toRadians(placeCoords.longitude-locCoords.longitude)

        val a = sin(dlat/2) * sin(dlat/2) + cos(toRadians(placeCoords.latitude)) * cos(toRadians(locCoords.latitude)) * sin(dlng/2)  * sin(dlng/2)

        val c = 2 * atan2(sqrt(a), sqrt(1-a));
        val d = r * c

        return d
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
        loadingDialog.show()
        if (!isNetworkAvailable(this)) loadingDialog.findViewById<TextView>(R.id.splash_internet_conn).visibility = View.VISIBLE

        this.mapboxMap = mapboxMap
        Log.d("MapActivity", "Map ready")
        mapboxMap.setStyle(
                Style.MAPBOX_STREETS
        ) { style -> enableLocationComponent(style) }
        ///recyler view for places
        placesRecyclerview = findViewById<RecyclerView>(R.id.places_recycler_view)
        // this creates a horizontal linear layout Manager
        placesRecyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        placesRecyclerview.adapter = placesAdapter

        findViewById<View>(R.id.float_btn_hint).setOnClickListener(View.OnClickListener {
            hint = true
            getAllPlaces()
            addBottomSheet()
            placesRecyclerview.layoutManager?.scrollToPosition(0)
        })
    }


    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions =
                LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
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

                Log.d("MapActivity", "getLocation")
                var myLat = mapboxMap!!.locationComponent.lastKnownLocation!!.latitude
                var myLng = mapboxMap!!.locationComponent.lastKnownLocation!!.longitude
                myCords = LatLng(myLat, myLng)
                filterCoords = myCords
                addDrawerFilter()
                getAllPlaces()
                setFocusOnMap(myCords.latitude, myCords.longitude)
                try {
                    // Request location updates
                    locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
                } catch(ex: SecurityException) {
                    Log.d("MapActivity", "Security Exception, no location available")
                }

                //button to move back to location
                findViewById<View>(R.id.float_btn_back_to_location).setOnClickListener {
                    myLat = mapboxMap!!.locationComponent.lastKnownLocation!!.latitude
                    myLng = mapboxMap!!.locationComponent.lastKnownLocation!!.longitude
                    if (aroundMyLocation) filterCoords = LatLng(myLat, myLng)
                    myCords = LatLng(myLat, myLng)

                    val position = CameraPosition.Builder()
                        .target(myCords)
                        .zoom(15.0) // Sets the zoom
                        .build()
                    mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1500)
                    //delay tracking so it can zoom in location
                    Handler(Looper.getMainLooper()).postDelayed({
                        locationComponent!!.cameraMode = CameraMode.TRACKING
                    }, 2000)
                    isInTrackingMode = true
                }
            }

        } else {
            noLocation(false)
        }
    }

    override fun onCameraTrackingDismissed() {
        isInTrackingMode = false
    }

    override fun onCameraTrackingChanged(currentMode: Int) {
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
        Log.d("MapActivity", "onStart")
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        Log.d("MapActivity", "onResume")
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

        Log.d("MapActivity", "onDestroy")
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onItemClick(position: Int, place: Place, id: String, score: Int, allImages : ArrayList<String>, allReviews : List<Review>, hasRev: Boolean, reviewID : String, review : Review){
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
                var text = review_text.text.toString()
                text = text.replace("\\s+".toRegex(), " ").trim()
                if (checked_stars > 0 && checked_stars < 6) {
                    val reviewU = Review(Calendar.getInstance().time, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), text, checked_stars, place.title, id)
                    val reviewP = Review( Calendar.getInstance().time, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), text, checked_stars, authUser.displayName!!, authUser.uid)

                    db.getReference("places").child(id).child("reviews").child(authUser.uid).setValue(reviewP)
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
               /* place_web2.setOnClickListener{
                    openURL(place.website2)
                }*/
            }
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
        if (any){
            dialog.findViewById<LinearLayout>(R.id.show_place_workhours).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.monday_hours).text = place.workhours.monday
            dialog.findViewById<TextView>(R.id.tuesday_hours).text = place.workhours.tuesday
            dialog.findViewById<TextView>(R.id.wednesday_hours).text = place.workhours.wednesday
            dialog.findViewById<TextView>(R.id.thursday_hours).text = place.workhours.thursday
            dialog.findViewById<TextView>(R.id.friday_hours).text = place.workhours.friday
            dialog.findViewById<TextView>(R.id.saturday_hours).text = place.workhours.saturday
            dialog.findViewById<TextView>(R.id.sunday_hours).text = place.workhours.sunday
            val workhours_days = ArrayList<RelativeLayout>()
            workhours_days.add(dialog.findViewById(R.id.sunday))
            workhours_days.add(dialog.findViewById(R.id.monday))
            workhours_days.add(dialog.findViewById(R.id.tuesday))
            workhours_days.add(dialog.findViewById(R.id.wednesday))
            workhours_days.add(dialog.findViewById(R.id.thursday))
            workhours_days.add(dialog.findViewById(R.id.friday))
            workhours_days.add(dialog.findViewById(R.id.saturday))

            workhours_days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1].setBackgroundColor(Color.parseColor("#AD33689A"))

        }
        ///for Images
        if (allImages.isNotEmpty()) dialog.findViewById<LinearLayout>(R.id.show_place_images).visibility = View.VISIBLE
        val image_container = dialog.findViewById<RecyclerView>(R.id.recyler_view_images)
        image_container.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val imageAdapter = ImagesAdapter(allImages, false,this)
        image_container.adapter = imageAdapter

        ///for Reviews
        val review_container = dialog.findViewById<RecyclerView>(R.id.show_place_review_list)
        review_container.layoutManager = LinearLayoutManager(baseContext)

        val reviewAdapter = ReviewsAdapter(allReviews, null,"other", this)
        review_container.adapter = reviewAdapter


        dialog.show()
    }

    private fun openURL(uriString : String){
        AlertDialog.Builder(this)
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
    override fun showHint(position: Int, place: Place, id: String, score: Int, allImages: ArrayList<String>, allReviews : List<Review>, hasRev: Boolean, reviewID : String, review : Review) {
        val hint_dialog = Dialog(this@MainMapActivity)
        hint_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        hint_dialog.setCancelable(true)
        hint_dialog.setCanceledOnTouchOutside(true)
        hint_dialog.setContentView(R.layout.dialog_info_hint)
        hint_dialog.findViewById<TextView>(R.id.info_text_place).text = place.title
        hint_dialog.findViewById<Button>(R.id.btn_show_place).setOnClickListener {
            hint_dialog.dismiss()
            onItemClick(position, place, id, score,allImages, allReviews, hasRev, reviewID, review)}
        hint_dialog.findViewById<Button>(R.id.btn_continue).setOnClickListener { hint_dialog.dismiss() }
        hint_dialog.findViewById<Button>(R.id.btn_another_hint).setOnClickListener {
            hint_dialog.dismiss()
            hint = true
            getAllPlaces()
        }
        hint_dialog.show()
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    return true
                }
            }
        }
        return false
    }

    override fun onItemClick(review: Review, reviewID : String) {
        //do nothing
    }

    override fun onImageClick(position: Int, allImages : ArrayList<String>) {
        val imageSliderDialog = Dialog(this@MainMapActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        imageSliderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        imageSliderDialog.setCancelable(true)
        imageSliderDialog.setCanceledOnTouchOutside(true)
        imageSliderDialog.setContentView(R.layout.dialog_fullscreen_images)

        val viewPager = imageSliderDialog.findViewById<ViewPager>(R.id.ViewPager_fullscreen_images)
        val viewPagerAdapter = ViewPagerAdapter(this@MainMapActivity, allImages)
        viewPager.adapter = viewPagerAdapter
        viewPager.setCurrentItem(position)

        imageSliderDialog.findViewById<ImageButton>(R.id.btn_exit_fullscreen_images).setOnClickListener {
            imageSliderDialog.dismiss()
        }

        imageSliderDialog.show()
    }

    override fun onBtnRemove(position: Int) {
        TODO("Not yet implemented")
    }

}

