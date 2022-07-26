package hr.project.hynt.FirebaseDatabase

import java.util.*
data class User(val timestamp: Date =  Calendar.getInstance().time,
                val username: String = "",
                val email: String = "",
                val reviews : HashMap<String, Review> = HashMap<String, Review>(),
                val addresses : HashMap<String, Address> = HashMap<String, Address>()){


}