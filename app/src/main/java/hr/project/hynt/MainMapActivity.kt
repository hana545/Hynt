package hr.project.hynt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.mapboxsdk.Mapbox
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


class MainMapActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, OnCameraTrackingChangedListener {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var isInTrackingMode = false

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    private val PERMISSION_REQUEST_CODE_LOCATION =  102

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main_map)

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

            })
        } else {
            btn_user.visibility = View.GONE
        }
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
            findViewById<View>(R.id.float_btn_back_to_camera_tracking_mode).setOnClickListener {
                var mylat = mapboxMap!!.locationComponent.lastKnownLocation!!.latitude
                var mylng = mapboxMap!!.locationComponent.lastKnownLocation!!.longitude
                val position = CameraPosition.Builder()
                    .target(LatLng(mylat, mylng))
                    .zoom(16.0) // Sets the zoom
                    .build()
                mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 2000)

                isInTrackingMode = true
            }

        } else {
            val position = CameraPosition.Builder()
                    .zoom(5.0) // Sets the zoom
                    .build()

            mapboxMap!!.animateCamera(CameraUpdateFactory
                    .newCameraPosition(position), 7000)
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
            } else {
                // Permission Denied
                Toast.makeText(this@MainMapActivity, "Location services denied!", Toast.LENGTH_SHORT)
                        .show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
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
}