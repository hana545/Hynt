package hr.project.hynt.FirebaseDatabase

import android.R
import android.app.AlertDialog
import android.content.DialogInterface
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import hr.project.hynt.AdminManageCategoriesFragment
import hr.project.hynt.AdminOptionsActivity


data class TagCategory(val name: String = ""){
    var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    fun addTagCategory(tag_category: TagCategory, type: String) {
        val key: String = db.getReference(type).push().key.toString()
        db.getReference(type).child(key).setValue(tag_category.name)
    }

}