package hr.project.hynt.FirebaseDatabase

import java.util.*
data class User(val username: String, val email: String, val reviews : ArrayList<Review> = ArrayList<Review>()) {


}