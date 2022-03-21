package hr.project.hynt

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private val PERMISSION_REQUEST_CODE_LOCATION =  102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main_map)
        mapView = findViewById(R.id.mapView) as MapView
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

        //ask for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE_LOCATION)
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
            findViewById<View>(R.id.back_to_camera_tracking_mode).setOnClickListener {
                if (!isInTrackingMode) {
                    isInTrackingMode = true
                    val position = CameraPosition.Builder()
                            .zoom(16.0) // Sets the zoom
                            .build()
                    mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 7000)

                    locationComponent!!.cameraMode = CameraMode.TRACKING
                    locationComponent!!.zoomWhileTracking(16.0)
                }
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