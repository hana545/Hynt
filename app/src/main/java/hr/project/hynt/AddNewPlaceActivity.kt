package hr.project.hynt

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import hr.project.hynt.Adapters.ImagesAdapter
import hr.project.hynt.FirebaseDatabase.Place
import hr.project.hynt.FirebaseDatabase.Review
import hr.project.hynt.FirebaseDatabase.Workhour
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class AddNewPlaceActivity: AppCompatActivity(), View.OnTouchListener,ViewTreeObserver.OnScrollChangedListener, ImagesAdapter.ItemClickListener {

    var allCategories = ArrayList<String>()

    private var placeAddressAutocompleteResult : EditText? = null

    private var coordinates : LatLng = LatLng(0.0,0.0)
    private lateinit var scrollView: ScrollView
    private lateinit var btn_addPlace: Button


    private val REQUEST_CODE = 200
    private val PERMISSION_REQUEST_CODE_STORAGE =  102

    val allReviews = HashMap<String, Review>()
    val allImages = ArrayList<String>()
    val existingImages = HashMap<String, String>()
    val storageReference = FirebaseStorage.getInstance().reference

    lateinit var imagesRecylerView : RecyclerView
    val imagesAdapter = ImagesAdapter(allImages,true, this)

    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_place)

        if (authUser == null) {
            finish()
        }
        setCustomActionBar()
        scrollView = findViewById(R.id.add_place_scroll_view)
        scrollView.setOnTouchListener(this)
        scrollView.viewTreeObserver.addOnScrollChangedListener(this)
        btn_addPlace = findViewById(R.id.btn_add_place)

        ////For Tags
        if (intent.getBooleanExtra("new", true) &&  !intent.getBooleanExtra("copy", false)) getAllTags(ArrayList<String>())

        ////For Categories
        val categorySpinner = findViewById<Spinner>(R.id.categories_spinner)
        val catAdapter : ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, allCategories)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.adapter = catAdapter
        getAllCategories(catAdapter)

        ////////////////////////WIP
        //slide (hide|show) cards
        val btn_add_basicInfo : Button = findViewById<Button>(R.id.btn_add_place_basicInfo_label)
        val btn_add_contactInfo : Button = findViewById<Button>(R.id.btn_add_place_contactInfo_label)
        val btn_add_workhours : Button = findViewById<Button>(R.id.btn_add_place_workhours_label)
        val btn_add_images : Button = findViewById<Button>(R.id.btn_add_place_images_label)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        btn_add_basicInfo.setOnClickListener {
            val basicInfo = findViewById<RelativeLayout>(R.id.add_place_basicInfo)
            if (basicInfo.visibility == View.VISIBLE){
                basicInfo.visibility = View.GONE
                btn_add_basicInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_informations, 0, R.drawable.ic_expand_more, 0)
            } else {
                basicInfo.visibility = View.VISIBLE
                btn_add_basicInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_informations, 0, R.drawable.ic_expand_less, 0)
            }
        }
        btn_add_contactInfo.setOnClickListener {
            val contactInfo = findViewById<RelativeLayout>(R.id.add_place_contactInfo)
            if (contactInfo.visibility == View.VISIBLE){
                contactInfo.visibility = View.GONE
                btn_add_contactInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contact, 0, R.drawable.ic_expand_more, 0)
            } else {
                contactInfo.visibility = View.VISIBLE
                btn_add_contactInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contact, 0, R.drawable.ic_expand_less, 0)
            }
        }
        btn_add_workhours.setOnClickListener {
            val workhours = findViewById<LinearLayout>(R.id.add_place_workhours)
            if (workhours.visibility == View.VISIBLE){
                workhours.visibility = View.GONE
                btn_add_workhours.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock_hour, 0, R.drawable.ic_expand_more, 0)
            } else {
                workhours.visibility = View.VISIBLE
                btn_add_workhours.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock_hour, 0, R.drawable.ic_expand_less, 0)
            }
        }
        btn_add_images.setOnClickListener {
            val images = findViewById<RelativeLayout>(R.id.add_place_images)
            if (images.visibility == View.VISIBLE){
                images.visibility = View.GONE
                btn_add_images.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pictures, 0, R.drawable.ic_expand_more, 0)
            } else {
                images.visibility = View.VISIBLE
                btn_add_images.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pictures, 0, R.drawable.ic_expand_less, 0)
            }
        }



        //for workhour data input
        val monday_hour = findViewById<TextView>(R.id.monday_hours)
        val tuesday_hour = findViewById<TextView>(R.id.tuesday_hours)
        val wednesday_hour = findViewById<TextView>(R.id.wednesday_hours)
        val thursday_hour = findViewById<TextView>(R.id.thursday_hours)
        val friday_hour = findViewById<TextView>(R.id.friday_hours)
        val saturday_hour = findViewById<TextView>(R.id.saturday_hours)
        val sunday_hour = findViewById<TextView>(R.id.sunday_hours)

        val mondayWorkhour = findViewById<ImageButton>(R.id.btn_edit_monday_hour)
        mondayWorkhour.setOnClickListener {
            workhoursDialog(mondayWorkhour.id, monday_hour.text.toString())
        }
        val tuesdayWorkhour = findViewById<ImageButton>(R.id.btn_edit_tuesday_hour)
        tuesdayWorkhour.setOnClickListener {
            workhoursDialog(tuesdayWorkhour.id, tuesday_hour.text.toString())
        }
        val wednesdayWorkhour = findViewById<ImageButton>(R.id.btn_edit_wednesday_hour)
        wednesdayWorkhour.setOnClickListener {
            workhoursDialog(wednesdayWorkhour.id, wednesday_hour.text.toString())
        }
        val thursdayWorkhour = findViewById<ImageButton>(R.id.btn_edit_thursday_hour)
        thursdayWorkhour.setOnClickListener {
            workhoursDialog(thursdayWorkhour.id, thursday_hour.text.toString())
        }
        val fridayWorkhour = findViewById<ImageButton>(R.id.btn_edit_friday_hour)
        fridayWorkhour.setOnClickListener {
            workhoursDialog(fridayWorkhour.id, friday_hour.text.toString())
        }
        val saturdayWorkhour = findViewById<ImageButton>(R.id.btn_edit_saturday_hour)
        saturdayWorkhour.setOnClickListener {
            workhoursDialog(saturdayWorkhour.id, saturday_hour.text.toString())
        }
        val sundayWorkhour = findViewById<ImageButton>(R.id.btn_edit_sunday_hour)
        sundayWorkhour.setOnClickListener {
            workhoursDialog(sundayWorkhour.id, sunday_hour.text.toString())
        }

        findViewById<Button>(R.id.add_place_btn_add_images).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE_STORAGE
                )
            } else {
                uploadImages()
            }
        }
        ///recycler view for images
        imagesRecylerView = findViewById<RecyclerView>(R.id.recyler_view_images)
        // this creates a horizontal linear layout Manager
        imagesRecylerView.layoutManager = GridLayoutManager(applicationContext, 2)
        imagesRecylerView.adapter = imagesAdapter

        //////////////////////////
        ///Init data
        val place_name : EditText = findViewById<EditText>(R.id.add_place_name_data)
        val place_address : EditText = findViewById<EditText>(R.id.add_place_address_data)
        val place_description : EditText = findViewById<EditText>(R.id.add_place_description_data)
        val place_phone1 : EditText = findViewById<EditText>(R.id.add_place_phone1)
        val place_phone2 : EditText = findViewById<EditText>(R.id.add_place_phone2)
        val place_email1 : EditText = findViewById<EditText>(R.id.add_place_email1)
        val place_email2 : EditText = findViewById<EditText>(R.id.add_place_email2)
        val place_web1 : EditText = findViewById<EditText>(R.id.add_place_website1)
        val place_web2 : EditText = findViewById<EditText>(R.id.add_place_website2)
        val btn_autocomplete : ImageButton = findViewById(R.id.btn_autocomplete_address)
        val btn_findOnMap : ImageButton = findViewById(R.id.btn_map_address)

        placeAddressAutocompleteResult = place_address

        ///Autocomplete for address
        btn_autocomplete.setOnClickListener {
            openAutocomplete()

        }
        var resultLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // parse result and perform action
                val lat = result.data!!.extras!!.get("Lat") as Double
                val lng = result.data!!.extras!!.get("Lng") as Double
                coordinates = LatLng(lat,lng)
                geocoderRev(lat, lng)

            }
        }
        btn_findOnMap.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            intent.putExtra("coordinates", coordinates)
            resultLauncher.launch(intent)
        }

        ///if the place is not new, if the place is in editing mode or in copy mode
        if(intent.getBooleanExtra("new", true) == false || intent.getBooleanExtra("copy", false) == true){
            val place_id = intent.getStringExtra("place_id")!!
            var place : Place
            db.getReference("places").child(place_id).addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        place = snapshot.getValue<Place>()!!
                        place_name.setText(place.title)
                        place_address.setText(place.address)
                        coordinates = LatLng(place.lat,place.lng)
                        place_description.setText(place.desc)
                        place_phone1.setText(place.phone1)
                        place_email1.setText(place.email1)
                        place_web1.setText(place.website1)
                        place_phone2.setText(place.phone2)
                        place_email2.setText(place.email2)
                        place_web2.setText(place.website2)

                        categorySpinner.setSelection(allCategories.indexOf(place.category))

                        getAllTags(place.tags)

                        monday_hour.text = place.workhours.monday
                        tuesday_hour.text = place.workhours.tuesday
                        wednesday_hour.text = place.workhours.wednesday
                        thursday_hour.text = place.workhours.thursday
                        friday_hour.text = place.workhours.friday
                        saturday_hour.text = place.workhours.saturday
                        sunday_hour.text = place.workhours.sunday

                        allImages.clear()
                        allImages.addAll(place.images.values)
                        existingImages.clear()
                        existingImages.putAll(place.images)

                        if (!intent.getBooleanExtra("copy", false)) allReviews.putAll(place.reviews)
                        setContactButtonListeners(place_phone2.text.isNotEmpty(), place_email2.text.isNotEmpty(), place_web2.text.isNotEmpty())
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w("Database Error", "Failed to read value.", error.toException())
                }

            })
        } else {
            setContactButtonListeners(false, false, false)

        }


        btn_addPlace.setOnClickListener {
            var error = false
            if (place_name.text.toString().isEmpty()) {
                place_name.error = "Name is needed!"
                error = true
            }
            if (place_address.text.toString().isEmpty()) {
                place_address.error = "Place needs to have address."
                error = true
            }
            if (error == false){
                ///getTags
                var selectedTags = ArrayList<String>()
                val chipTagGroup = findViewById<ChipGroup>(R.id.add_place_tags_chip_group)
                val tagId : List<Int> = chipTagGroup.checkedChipIds
                for (id in tagId){
                    val chip = chipTagGroup.findViewById(id) as Chip
                    selectedTags.add(chip.text.toString())
                }
                ///getCategory
                val category = categorySpinner.selectedItem.toString()
                ///getWorkhours
                val workhours = Workhour(monday_hour.text.toString(), tuesday_hour.text.toString(), wednesday_hour.text.toString(), thursday_hour.text.toString(), friday_hour.text.toString(), saturday_hour.text.toString(), sunday_hour.text.toString())

                addPlace(place_name.text.toString(), place_address.text.toString(), place_description.text.toString(), place_phone1.text.toString(), place_email1.text.toString(), place_web1.text.toString(),
                        place_phone2.text.toString(), place_email2.text.toString(), place_web2.text.toString(), workhours, category, selectedTags)
            }
        }

    }

    private fun uploadImages() {
        var intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Choose Pictures")
            , REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                uploadImages()
            } else {
                // Permission Denied
                AlertDialog.Builder(this)
                    .setTitle("Denied access to storage")
                    .setMessage("You have not granted permission to access your storage! You need to change that for uploading pictures. ")
                    .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { d, _ ->
                        d.dismiss()
                    })
                    .setIcon(R.drawable.ic_settings)
                    .setCancelable(false)
                    .show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkAndAddZero(number : Int) : String {
        return if (number < 10) "0$number" else number.toString()
    }

    private fun workhoursDialog(id : Int, hours : String){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_edit_workhours)

        //check and disable chip for picked day
        val idEN = getResources().getResourceEntryName(id) //btn_edit_monday_hour
        val checkChipId = "chipDay_" + idEN.substring(0, idEN.length-"_hour".length).drop("btn_edit_".length) //chipDay_monday
        dialog.findViewById<Chip>(getResources().getIdentifier(checkChipId, "id", packageName)).isChecked = true
        dialog.findViewById<Chip>(getResources().getIdentifier(checkChipId, "id", packageName)).isEnabled = false

        //removing and adding hours
        val btn_add_hour = dialog.findViewById<ImageButton>(R.id.btn_add_hour)
        val firstH_view = dialog.findViewById<LinearLayout>(R.id.work_hour_picker)
        val secondH_view = dialog.findViewById<LinearLayout>(R.id.work_hour_picker2)
        val btn_remove_firstH = dialog.findViewById<ImageButton>(R.id.btn_remove_hour)
        val btn_remove_secondH = dialog.findViewById<ImageButton>(R.id.btn_remove_hour2)
        val checkbox_closed = dialog.findViewById<CheckBox>(R.id.checkbox_closed)
        val checkbox_open24 = dialog.findViewById<CheckBox>(R.id.checkbox_open24)

        btn_add_hour.setOnClickListener {
            checkbox_closed.isChecked = false
            checkbox_open24.isChecked = false
            if (secondH_view.visibility == View.VISIBLE){
                firstH_view.visibility = View.VISIBLE
            } else if ( firstH_view.visibility == View.VISIBLE){
                secondH_view.visibility = View.VISIBLE
            } else {
                firstH_view.visibility = View.VISIBLE
            }
            if (secondH_view.visibility == View.VISIBLE && firstH_view.visibility == View.VISIBLE){
                btn_add_hour.visibility = View.GONE
            }
        }
        btn_remove_firstH.setOnClickListener {
            firstH_view.visibility = View.GONE
            btn_add_hour.visibility = View.VISIBLE
            if (secondH_view.visibility != View.VISIBLE){
                checkbox_closed.isChecked = true
                checkbox_open24.isChecked = false
            }
        }
        btn_remove_secondH.setOnClickListener {
            secondH_view.visibility = View.GONE
            btn_add_hour.visibility = View.VISIBLE
            if (firstH_view.visibility != View.VISIBLE){
                checkbox_closed.isChecked = true
                checkbox_open24.isChecked = false
            }
        }
        checkbox_closed.setOnClickListener {
            if (checkbox_closed.isChecked){
                firstH_view.visibility = View.GONE
                secondH_view.visibility = View.GONE
                btn_add_hour.visibility = View.VISIBLE
                checkbox_open24.isChecked = false
            } else {
                firstH_view.visibility = View.VISIBLE
                btn_add_hour.visibility = View.VISIBLE
            }
        }
        checkbox_open24.setOnClickListener {
            if (checkbox_open24.isChecked){
                firstH_view.visibility = View.GONE
                secondH_view.visibility = View.GONE
                btn_add_hour.visibility = View.VISIBLE
                checkbox_closed.isChecked = false
            } else {
                firstH_view.visibility = View.VISIBLE
                btn_add_hour.visibility = View.VISIBLE
            }
        }

        ///////hour picker
        var timeStart1 = ""
        var timeEnd1 = ""
        var timeStart2 = ""
        var timeEnd2 = ""

        if (hours.isNotEmpty()) {
            if(hours == "Closed"){
                firstH_view.visibility = View.GONE
                checkbox_closed.isChecked = true
            } else if(hours == "Open 24h"){
                firstH_view.visibility = View.GONE
                checkbox_open24.isChecked = true
            } else {
                //17 : 40 - 17 : 40
                val hStart1 = Integer.parseInt(hours.substring(0, 2))
                val mStart1 = Integer.parseInt(hours.substring(5, 7))
                timeStart1 =  "${checkAndAddZero(hStart1)} : ${checkAndAddZero(mStart1)}"
                val hEnd1 = Integer.parseInt(hours.substring(10, 12))
                val mEnd1 = Integer.parseInt(hours.substring(15, 17))
                timeEnd1 =  "${checkAndAddZero(hEnd1)} : ${checkAndAddZero(mEnd1)}"
                if (hours.length > 17) {
                    //17 : 40 - 17 : 40\n17 : 40 - 17 : 40
                    secondH_view.visibility = View.VISIBLE
                    btn_add_hour.visibility = View.GONE
                    val hStart2 = Integer.parseInt(hours.substring(18, 20))
                    val mStart2 = Integer.parseInt(hours.substring(23, 25))
                    timeStart2 = "${checkAndAddZero(hStart2)} : ${checkAndAddZero(mStart2)}"
                    val hEnd2 = Integer.parseInt(hours.substring(28, 30))
                    val mEnd2 = Integer.parseInt(hours.substring(33, 35))
                    timeEnd2 = "${checkAndAddZero(hEnd2)} : ${checkAndAddZero(mEnd2)}"
                }
            }
        }


        val start1 = dialog.findViewById<TextView>(R.id.hour_start)
        start1.text = timeStart1
        start1.setOnClickListener {
            val picker : TimePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val h = checkAndAddZero(hour)
                val m = checkAndAddZero(minute)
                timeStart1 =  "$h : $m"
                start1.text = timeStart1
            }, 12, 0, true)
            picker.show()
        }

        val end1 = dialog.findViewById<TextView>(R.id.hour_end)
        end1.text = timeEnd1
        end1.setOnClickListener {
            val picker : TimePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val h = checkAndAddZero(hour)
                val m = checkAndAddZero(minute)
                timeEnd1 = "$h : $m"
                end1.text = timeEnd1
            }, 12, 0, true)
            picker.show()
        }
        val start2 = dialog.findViewById<TextView>(R.id.hour_start2)
        start2.text = timeStart2
        start2.setOnClickListener {
            val picker : TimePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val h = checkAndAddZero(hour)
                val m = checkAndAddZero(minute)
                timeStart2 =  "$h : $m"
                start2.text = timeStart2
            },12, 0,  true)
            picker.show()
        }

        val end2 = dialog.findViewById<TextView>(R.id.hour_end2)
        end2.text = timeEnd2
        end2.setOnClickListener {
            val picker : TimePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val h = checkAndAddZero(hour)
                val m = checkAndAddZero(minute)
                timeEnd2 = "$h : $m"
                end2.text = timeEnd2
            }, 12, 0, true)
            picker.show()
        }


        val save = dialog.findViewById<TextView>(R.id.btn_save)
        save.setOnClickListener {
            val chipgroup = dialog.findViewById<ChipGroup>(R.id.days_of_the_week_picker)
            val daysId : List<Int> = chipgroup.checkedChipIds
            var chipID: String
            var targetID: String
            if ((( (start1.text.isEmpty() || end1.text.isEmpty()) && firstH_view.visibility == View.VISIBLE ) || ((start2.text.isEmpty() && end2.text.isEmpty()) && secondH_view.visibility == View.VISIBLE)) && !checkbox_closed.isChecked && !checkbox_open24.isChecked) {
                Toast.makeText(this, "Both, start and end hours, are needed!", Toast.LENGTH_SHORT).show()
            } else if ((start1.text == end1.text && firstH_view.visibility == View.VISIBLE ) || ((start2.text == end2.text && secondH_view.visibility == View.VISIBLE)) && !checkbox_closed.isChecked) {
                Toast.makeText(this, "Workhour can't start and end on the same hour, check Open 24h instead", Toast.LENGTH_SHORT).show()
            } else {
               for (dayId in daysId){
                    val chip = chipgroup.findViewById(dayId) as Chip
                    chipID = getResources().getResourceEntryName(chip.id) //chipDay_tuesday
                    targetID = chipID.drop("chipDay_".length) + "_hours" //tuesday_hours
                    if (checkbox_closed.isChecked){
                        findViewById<TextView>(getResources().getIdentifier(targetID, "id", packageName)).text = resources.getString(R.string.closed)
                    } else if (checkbox_open24.isChecked){
                       findViewById<TextView>(getResources().getIdentifier(targetID, "id", packageName)).text = resources.getString(R.string.open24)
                   } else {
                        var time =  ""
                        if (firstH_view.visibility.equals(View.VISIBLE)) time = time + start1.text + " - " + end1.text
                        if (firstH_view.visibility.equals(View.VISIBLE) && secondH_view.visibility.equals(View.VISIBLE)) time += "\n"
                        if (secondH_view.visibility.equals(View.VISIBLE)) time = time + start2.text + " - " + end2.text

                        findViewById<TextView>(getResources().getIdentifier(targetID, "id", packageName)).text = time
                    }

                }
                dialog.dismiss()
            }

        }
        dialog.show()
    }

    private fun setContactButtonListeners(phone : Boolean, email : Boolean, web : Boolean ) {
        val phone2_data = findViewById<EditText>(R.id.add_place_phone2)
        val phone2_layout = findViewById<LinearLayout>(R.id.add_place_contacts_phone2)
        val place_add_phone = findViewById<ImageButton>(R.id.btn_add_place_phone)
        val place_remove_phone = findViewById<ImageButton>(R.id.btn_remove_place_phone)
        val email2_data = findViewById<EditText>(R.id.add_place_email2)
        val email2_layout = findViewById<LinearLayout>(R.id.add_place_contacts_email2)
        val place_add_email = findViewById<ImageButton>(R.id.btn_add_place_email)
        val place_remove_email = findViewById<ImageButton>(R.id.btn_remove_place_email)
        val web2_data = findViewById<EditText>(R.id.add_place_website2)
        val web2_layout = findViewById<LinearLayout>(R.id.add_place_contacts_website2)
        val place_add_website = findViewById<ImageButton>(R.id.btn_add_place_website)
        val place_remove_website = findViewById<ImageButton>(R.id.btn_remove_place_website)

        place_add_phone.setOnClickListener {
            phone2_layout.visibility = View.VISIBLE
            place_add_phone.visibility = View.GONE
        }
        place_remove_phone.setOnClickListener {
            phone2_layout.visibility = View.GONE
            place_add_phone.visibility = View.VISIBLE
            phone2_data.setText("")
        }
        place_add_email.setOnClickListener {
            email2_layout.visibility = View.VISIBLE
            place_add_email.visibility = View.GONE
        }
        place_remove_email.setOnClickListener {
            email2_layout.visibility = View.GONE
            place_add_email.visibility = View.VISIBLE
            email2_data.setText("")
        }
        place_add_website.setOnClickListener {
            web2_layout.visibility = View.VISIBLE
            place_add_website.visibility = View.GONE
        }
        place_remove_website.setOnClickListener {
            web2_layout.visibility = View.GONE
            place_add_website.visibility = View.VISIBLE
            web2_data.setText("")
        }

        if (phone) place_add_phone.performClick()
        if (email) place_add_email.performClick()
        if (web) place_add_website.performClick()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setCustomActionBar() {
        this.supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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

        val btn_home : LinearLayout = customView.findViewById<View>(R.id.btn_home) as LinearLayout
        btn_home.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MainMapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        })
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
        bottomSheetDialog.findViewById<LinearLayout>(R.id.home)!!.visibility = View.VISIBLE
        val home = bottomSheetDialog.findViewById<LinearLayout>(R.id.homeMap)
        home!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MainMapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })
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

    private fun getAllTags(placeTags: ArrayList<String>) {
        var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
        val places_query = db.getReference("tags")
        places_query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var allTags = ArrayList<String>()
                if (snapshot.exists()) {
                    for (tags : DataSnapshot in snapshot.children) {
                        val tag = tags.getValue<String>()
                        if (tag != null) {
                            allTags.add(tag)
                        }
                    }
                    allTags.sort()
                    for (tag : String in allTags) {
                        if (intent.getBooleanExtra("new", true) && !intent.getBooleanExtra("copy", false)){
                            addChip(tag, ArrayList<String>())
                        } else {
                            addChip(tag, placeTags)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }

    private fun addChip(tag: String, placeTags: ArrayList<String>){
        val chip = this.layoutInflater.inflate(R.layout.view_chips_buttons, null, false) as Chip
        chip.text = tag
        for (placeTag in placeTags){
            if (placeTag == tag) chip.isChecked = true
        }
        chip.id = View.generateViewId()
        findViewById<ChipGroup>(R.id.add_place_tags_chip_group).addView(chip)
    }

    private fun getAllCategories(adapter : ArrayAdapter<String>) {
        var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
        val places_query = db.getReference("categories")
        places_query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allCategories.clear()
                if (snapshot.exists()) {
                    for (categories: DataSnapshot in snapshot.children) {
                        val category = categories.getValue<String>()
                        if (category != null) {
                            allCategories.add(category)

                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                allCategories.sort()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }

    private fun openAutocomplete() {
        val intent = PlaceAutocomplete.IntentBuilder()
            .accessToken(getString(R.string.access_token))
            .placeOptions(
                PlaceOptions.builder()
                    .backgroundColor(Color.parseColor("#EEEEEE"))
                    .limit(10)
                    .build(PlaceOptions.MODE_CARDS))
            .build(this)
        startActivityForResult(intent, 1)
    }

    override fun onImageClick(position: Int, allImages: ArrayList<String>) {
        TODO("Not yet implemented")
    }

    override fun onBtnRemove(position: Int){
        if(allImages.get(position) in existingImages.values){
            FirebaseStorage.getInstance().getReferenceFromUrl(allImages[position]).delete().addOnCompleteListener(
                OnCompleteListener<Void> { task ->
                    if (task.isSuccessful) {
                        Log.w("showImages", "deleteImage:success ")
                    } else {
                        Log.w("showImages", "deleteImage:failure "+task.exception)
                    }
                })

        }
        allImages.removeAt(position)
        imagesAdapter.notifyDataSetChanged()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1) {
            val feature = PlaceAutocomplete.getPlace(data)
            placeAddressAutocompleteResult?.setText(feature.placeName())
            coordinates = LatLng((feature.geometry() as Point).latitude(), (feature.geometry() as Point).longitude())

        }
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE){
            // if multiple images are selected
            if (data?.getClipData() != null) {
                var count = data.clipData?.itemCount
                for (i in 0..count!! - 1) {
                    var imageUri: Uri = data.clipData?.getItemAt(i)!!.uri
                    allImages.add(imageUri.toString())
                    imagesAdapter.notifyDataSetChanged()

                }
            } else if (data?.getData() != null) {
                // if single image is selected
                var imageUri: Uri = data.data!!
                allImages.add(imageUri.toString())
                imagesAdapter.notifyDataSetChanged()
            }
        }
    }


    private fun addPlace(place_name_text: String, place_address_text: String, place_description_text: String, place_phone1_text: String, place_email1_text: String, place_website1_text: String, place_phone2_text: String, place_email2_text: String, place_website2_text: String, place_workhours: Workhour, category: String, selectedTags: ArrayList<String>) {
        val progressDialog = ProgressDialog(this@AddNewPlaceActivity)
        progressDialog.setMessage("Creating your place")
        progressDialog.show()
        if (FirebaseAuth.getInstance().currentUser != null) {
            val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
            val key: String
            if(intent.getBooleanExtra("new", true) == false){
                key = intent.getStringExtra("place_id")!!
            } else {
                key = db.getReference("places").push().key.toString()
            }
            val imageNames = HashMap<String, String>()
            val nPlace = Place(key, Calendar.getInstance().time, place_name_text,place_address_text,coordinates.latitude, coordinates.longitude,  place_description_text.replace("\\s+".toRegex(), " ").trim(), place_phone1_text, place_phone2_text, place_email1_text, place_email2_text, place_website1_text, place_website2_text, place_workhours, category, selectedTags, allReviews, imageNames, FirebaseAuth.getInstance().currentUser?.uid.toString(), false)
            db.getReference("places").child(key).setValue(nPlace).addOnSuccessListener {
                show_info_dialog("Successfully added place " + nPlace.title + "!", "It is waiting for approval but you can check it out in your list of places", true)
                progressDialog.dismiss()
                for(imageUri in allImages) {
                    if(imageUri in existingImages.values){
                        for((name, uri) in existingImages)
                            if (uri.equals(imageUri)) imageNames.put(name, imageUri)
                        db.getReference("places").child(key).child("images").setValue(imageNames)
                        continue
                    } else {
                        val imageName = UUID.randomUUID().toString()
                        val ref = storageReference.child("myImages/").child(key).child(imageName)
                        ref.putFile(imageUri.toUri()).addOnCompleteListener(
                            OnCompleteListener<UploadTask.TaskSnapshot> { task ->
                                if (task.isSuccessful) {
                                    ref.downloadUrl.addOnSuccessListener { uri ->
                                        imageNames.put(imageName, uri.toString())
                                        db.getReference("places").child(key).child("images").setValue(imageNames)
                                    }
                                    Log.w("showImages", "uploadImage:success")
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w("showImages", "uploadImage:failure", task.exception)

                                }
                            })
                    }
                }
            }
        } else {
            Toast.makeText(this@AddNewPlaceActivity, "Error occurred, try again", Toast.LENGTH_SHORT).show()
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

    // Member function to detect Scroll,
    // when detected 0, it means bottom is reached
    override fun onScrollChanged() {
        val view = scrollView.getChildAt(scrollView.childCount - 1)
        //val topDetector = scrollView.scrollY
        val bottomDetector: Int = view.bottom - (scrollView.height + scrollView.scrollY)
        if (bottomDetector == 0) {
            btn_addPlace.setBackgroundColor(ContextCompat.getColor(this, R.color.black_blue))
            btn_addPlace.isEnabled = true
        }
    }
    private fun show_info_dialog(text : String, subtext : String, success : Boolean){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        if (success) {
            dialog.setContentView(R.layout.dialog_info_success)
        } else {
            dialog.setContentView(R.layout.dialog_info_failed)
        }
        dialog.findViewById<TextView>(R.id.info_text).text = text
        dialog.findViewById<TextView>(R.id.info_subtext).text = subtext
        dialog.findViewById<Button>(R.id.btn_continue).setOnClickListener {
            dialog.dismiss()
            finish() }
        dialog.show()
    }

}

