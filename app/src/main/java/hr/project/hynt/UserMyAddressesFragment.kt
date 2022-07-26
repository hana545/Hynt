package hr.project.hynt

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import hr.project.hynt.Adapters.AddressesAdapter
import hr.project.hynt.FirebaseDatabase.Address
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class UserMyAddressesFragment : Fragment(), AddressesAdapter.ItemClickListener {
    var allAddresses= ArrayList<Address>()
    var allAddressesId = ArrayList<String>()

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser


    private var placeAddressAutocompleteResult : EditText? = null
    private var coordinates : LatLng = LatLng(0.0,0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private var mInflater: LayoutInflater? = null
    private var mRootView: ViewGroup? = null
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {

        mInflater = inflater
        mRootView = container
        val view = inflater.inflate(R.layout.fragment_user_my_addresses, container, false)

        val recyclerview = view.findViewById<RecyclerView>(R.id.review_recyclerView)
        // this creates a horizontal linear layout Manager
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        val adapter = AddressesAdapter(allAddresses, allAddressesId, this)
        recyclerview.adapter = adapter
        getAllAddreses(adapter)

        var resultLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // parse result and perform action
                val lat = result.data!!.extras!!.get("Lat") as Double
                val lng = result.data!!.extras!!.get("Lng") as Double
                coordinates = LatLng(lat, lng)
                geocoderRev(lat, lng)
            }
        }

        val float_btn_add_address = view.findViewById<FloatingActionButton>(R.id.fragment_btn_add_new_address)
        float_btn_add_address.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_add_new_address)
            val address_name = dialog.findViewById<TextInputEditText>(R.id.add_address_name_data)
            val address_data = dialog.findViewById<TextInputEditText>(R.id.add_address_data_data)
            placeAddressAutocompleteResult = address_data
            dialog.findViewById<ImageView>(R.id.btn_autocomplete_address).setOnClickListener {
                openAutocomplete()
            }
            dialog.findViewById<ImageView>(R.id.btn_map_address).setOnClickListener {
                val intent = Intent(requireContext(), LocationPickerActivity::class.java)
                intent.putExtra("coordinates", coordinates)
                resultLauncher.launch(intent)
            }
            dialog.findViewById<Button>(R.id.btn_add_address).setOnClickListener {
                val address_category = dialog.findViewById<Chip>(dialog.findViewById<ChipGroup>(R.id.chip_group_address_category).checkedChipId)
                if (!address_name!!.text!!.isEmpty() && !address_data!!.text!!.isEmpty()){
                    val key = db.getReference("users").child(authUser!!.uid).child("addresses").push().key.toString()
                    db.getReference("users").child(authUser.uid).child("addresses").child(key).setValue(Address(address_name.text.toString(), address_data.text.toString(), address_category.text.toString(), coordinates.latitude, coordinates.longitude))
                    show_info_dialog("Address added!", true)
                    dialog.dismiss()
                } else {
                    if (address_name.text!!.isEmpty()) dialog.findViewById<TextInputLayout>(R.id.add_address_name_layout).setError("Address name is needed!")
                    if  (address_data.text!!.isEmpty()) dialog.findViewById<TextInputLayout>(R.id.add_address_data_layout).setError("Address is needed! Please select it with buttons below")
                }
            }

            dialog.show()
        }

        // Inflate the layout for this fragment
        return view
    }

    fun openAutocomplete() {
        val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(getString(R.string.access_token))
                .placeOptions(
                        PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .build(PlaceOptions.MODE_CARDS))
                .build(requireActivity())
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1) {
            val feature = PlaceAutocomplete.getPlace(data)
            placeAddressAutocompleteResult?.setText(feature.placeName())
            coordinates = LatLng((feature.geometry() as Point).latitude(), (feature.geometry() as Point).longitude())

        }
    }
    private fun geocoderRev(lat : Double, lng : Double){

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
                    // Log the first results Point.
                    val firstResultPoint = results[0]
                    placeAddressAutocompleteResult?.setText(firstResultPoint.placeName())

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

    fun getAllAddreses(adapter: AddressesAdapter) {
        db.getReference("users").child(authUser!!.uid).child("addresses").orderByChild("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allAddresses.clear()
                allAddressesId.clear()
                if (snapshot.exists()) {
                    for (addresses: DataSnapshot in snapshot.children) {
                        val address: Address? = addresses.getValue<Address>()
                        if (address != null) {
                            allAddresses.add(address)
                            allAddressesId.add(addresses.key.toString())
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

    override fun onItemClick(address: Address, addressId: String) {

        val dialog = Dialog(requireContext(),android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_show_my_address)
        val address_name = dialog.findViewById<TextInputEditText>(R.id.show_address_name_data)
        address_name.setText(address.name)

        if (address.category == "Home") {
            dialog.findViewById<Chip>(R.id.chip_address_home).isChecked = true
        } else if (address.category == "Work"){
            dialog.findViewById<Chip>(R.id.chip_address_work).isChecked = true
        } else if (address.category == "Education"){
            dialog.findViewById<Chip>(R.id.chip_address_education).isChecked = true
        } else {
            dialog.findViewById<Chip>(R.id.chip_address_other).isChecked = true
        }

        val address_data = dialog.findViewById<TextInputEditText>(R.id.show_address_data_data)
        address_data.setText(address.data)
        placeAddressAutocompleteResult = address_data
        dialog.findViewById<ImageView>(R.id.btn_autocomplete_address).setOnClickListener {
            openAutocomplete()
        }
        dialog.findViewById<ImageView>(R.id.btn_map_address).setOnClickListener {
            val intent = Intent(requireContext(), LocationPickerActivity::class.java)
            intent.putExtra("coordinates", coordinates)
            var resultLauncher = registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // parse result and perform action
                    val lat = result.data!!.extras!!.get("Lat") as Double
                    val lng = result.data!!.extras!!.get("Lng") as Double
                    coordinates = LatLng(lat, lng)
                    geocoderRev(lat, lng)
                }
            }
            resultLauncher.launch(intent)
        }
        dialog.findViewById<ImageView>(R.id.show_address_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<ImageView>(R.id.show_address_save).setOnClickListener {
            val address_category = dialog.findViewById<Chip>(dialog.findViewById<ChipGroup>(R.id.chip_group_address_category).checkedChipId)
            if (!address_name!!.text!!.isEmpty() && !address_data!!.text!!.isEmpty()){
                db.getReference("users").child(authUser!!.uid).child("addresses").child(addressId).setValue(Address(address_name.text.toString(), address_data.text.toString(), address_category.text.toString(),coordinates.latitude, coordinates.longitude))
                dialog.dismiss()
                show_info_dialog("Address changed!", true)
            } else {
                if (address_name.text!!.isEmpty()) dialog.findViewById<TextInputLayout>(R.id.add_address_name_layout).setError("Address name is needed!")
                if  (address_data.text!!.isEmpty()) dialog.findViewById<TextInputLayout>(R.id.add_address_data_layout).setError("Address is needed! Please select it with buttons below")
            }
        }
        dialog.show()
    }

    override fun onBtnDelete(address: Address, addressId: String) {
        AlertDialog.Builder(activity)
                .setTitle("Delete address")
                .setMessage("Are you sure you want to delete this address: " + address.name + "?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                    db.getReference("users").child(authUser!!.uid).child("addresses").child(addressId).removeValue()
                    show_info_dialog("Address deleted!", true)

                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_remove)
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

}










