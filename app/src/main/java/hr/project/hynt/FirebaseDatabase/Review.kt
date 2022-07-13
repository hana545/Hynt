package hr.project.hynt.FirebaseDatabase

import java.text.SimpleDateFormat
import java.util.*


data class Review(val timestamp: Date =  Calendar.getInstance().time, val date: String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), val txt: String = "", val stars: Int = 0, val refName: String = "", val refId: String = "") {

}