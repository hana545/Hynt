package hr.project.hynt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
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
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"

    private var latitude : Double = 0.0
    private var longitude : Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setCustomActionBar()

        ////map
        mapView = findViewById<MapView>(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

    }

    @SuppressLint("WrongViewCast")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
                Style.MAPBOX_STREETS
        ) { style -> enableLocationPlugin(style)

        Toast.makeText(
                this@LocationPickerActivity,
                "Move to select Location", Toast.LENGTH_LONG).show()

        val hoveringMarker = ImageView(this)
        hoveringMarker.setImageResource(R.drawable.map_default_map_marker)
        val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        hoveringMarker.layoutParams = params
        mapView!!.addView(hoveringMarker)

        initDroppedMarker(style)
        val location_txt = findViewById<TextView>(R.id.selected_location_txt)
        val btn_selectLocation = findViewById<AppCompatButton>(R.id.btn_select_location)
        val btn_continue = findViewById<AppCompatButton>(R.id.btn_continue)
        btn_selectLocation.setOnClickListener {
            if(hoveringMarker.visibility == View.VISIBLE) {
                val mapTargetLatLng = mapboxMap.cameraPosition.target
                latitude = mapTargetLatLng.latitude
                longitude = mapTargetLatLng.longitude
                geocoderRev(latitude,longitude, location_txt)

                hoveringMarker.visibility = View.INVISIBLE

                btn_selectLocation.setBackgroundColor(resources.getColor(R.color.error_red))
                btn_selectLocation.text = "Drop location"
                btn_continue.isEnabled = true
                btn_continue.setBackgroundColor(resources.getColor(R.color.black_blue))

                if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                    val source: GeoJsonSource? = style.getSourceAs("dropped-marker-source-id")
                    if (source != null) {
                        source.setGeoJson(Point.fromLngLat(mapTargetLatLng.longitude, mapTargetLatLng.latitude))
                    }
                    val droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
                    if (droppedMarkerLayer != null) {
                        droppedMarkerLayer.setProperties(visibility(VISIBLE))
                    }
                }

            } else {
                btn_selectLocation.setBackgroundColor(resources.getColor(R.color.blue_light))
                btn_selectLocation.text = "Select location"
                btn_continue.isEnabled = false
                btn_continue.setBackgroundColor(resources.getColor(R.color.black_blue_low_opacity))
                location_txt.text = "Selected location: None"

                hoveringMarker.visibility = View.VISIBLE
                val droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
                if (droppedMarkerLayer != null) {
                    droppedMarkerLayer.setProperties(visibility(NONE))
                }
            }
        }

        btn_continue.setOnClickListener {
            if(hoveringMarker.visibility == View.INVISIBLE) {
                val intent = Intent(this, AddNewPlaceActivity::class.java)
                intent.putExtra("Lat", latitude)
                intent.putExtra("Lng", longitude)
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    } }

    private fun initDroppedMarker(style: Style){
        style.addImage("dropped-icon-image", BitmapFactory.decodeResource(
                getResources(), R.drawable.map_default_map_marker))
        style.addSource(GeoJsonSource("dropped-marker-source-id"))
        style.addLayer(SymbolLayer(DROPPED_MARKER_LAYER_ID, "dropped-marker-source-id").withProperties(
                iconImage("dropped-icon-image"),
                visibility(NONE),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)))
    }
    private fun enableLocationPlugin(style : Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

        // Get an instance of the component. Adding in LocationComponentOptions is also an optional parameter
            val locationComponent = mapboxMap!!.getLocationComponent();
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(
                    this, style).build());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationComponent.setLocationComponentEnabled(true)

            locationComponent.setRenderMode(RenderMode.NORMAL)
            val coord = intent.extras!!.get("coordinates") as LatLng
            if (coord.latitude != 0.0 && coord.longitude != 0.0) {
                val position = CameraPosition.Builder()
                        .target(coord)
                        .zoom(18.0) // Sets the zoom
                        .build()
                mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(position), 100)
            } else {
                locationComponent.cameraMode = CameraMode.TRACKING
            }

        } else {
            val permissionsManager = PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, "This app needs location permissions in order to show its functionality", Toast.LENGTH_LONG)
                .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted && mapboxMap != null) {
            val style = mapboxMap!!.getStyle();
            if (style != null) {
                enableLocationPlugin(style);
            }
        }
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

        val btn_user: LinearLayout = customView.findViewById<View>(R.id.btn_user_profile) as LinearLayout
        btn_user.visibility = View.GONE
    }
    private fun geocoderRev(lat : Double, lng : Double, location_text : TextView){

        val reverseGeocode = MapboxGeocoding.builder()
                .accessToken(getString(R.string.access_token))
                .query(Point.fromLngLat(lng, lat))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()

        reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
            ) {
                val results = response.body()!!.features()
                if (results.size > 0) {
                    val firstResultPoint = results[0]
                    location_text.setText("Selected location: "+firstResultPoint.text())
                } else {
                    // No result for your request were found.
                    Log.d("GeocoderResponse", "onResponse: No result found")
                }
            }
            override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                throwable.printStackTrace()
            }
        })


    }

}