package hr.project.hynt

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import hr.project.hynt.Adapters.TagCategoryAdapter
import hr.project.hynt.FirebaseDatabase.TagCategory
import java.util.*

class AdminManageCategoriesFragment : Fragment(), TagCategoryAdapter.ItemClickListener {

    var allCategories_id = ArrayList<String>()
    var allCategories = ArrayList<String>()
    var collectiveCategories = hashMapOf<String, String>()

    lateinit var text_info : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_manage_categories, container, false)
        text_info = view.findViewById(R.id.fragment_info)
        val btn_addCategory = view.findViewById<FloatingActionButton>(R.id.btn_add_new_category)
        btn_addCategory.setOnClickListener(View.OnClickListener {
            showAddCategoryDialog()
        })
        val recyclerview = view.findViewById<RecyclerView>(R.id.category_recyclerView)
        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(context)

        val adapter = TagCategoryAdapter(allCategories, allCategories_id, this)
        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
        getallCategories(adapter)
        return view
    }


    private fun getallCategories(adapter: TagCategoryAdapter) {
        var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
        val places_query = db.getReference("categories")

        places_query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    allCategories.clear()
                    allCategories_id.clear()
                    collectiveCategories.clear()
                    for (categories: DataSnapshot in snapshot.children) {
                        val category = categories.getValue<String>()
                        val category_id = categories.key.toString()
                        if (category != null) {
                            collectiveCategories.put(category_id,category)

                        }
                    }
                } else {
                    text_info.visibility = View.VISIBLE
                }
                val result = collectiveCategories.toList().sortedBy { (_, value) -> value}.toMap()
                allCategories.addAll(result.values)
                allCategories_id.addAll(result.keys)
                adapter.notifyDataSetChanged()
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }


    private fun showAddCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_add_new_category)

        val category_name : EditText = dialog.findViewById<EditText>(R.id.add_category_data)

        val btn_add_category : Button = dialog.findViewById(R.id.btn_add_category)
        btn_add_category.setOnClickListener {
            val category_name_text = category_name.text.toString()
            if (category_name_text.isEmpty()) {
                category_name.requestFocus()
                category_name.error = "Name cannot be empty"

            } else {
                TagCategory().addTagCategory(TagCategory(category_name_text), "categories")

                dialog.dismiss()
            }
        }

        dialog.show()
    }
    override fun onItemClick(id: String) {
        AlertDialog.Builder(activity)
                .setTitle("Delete category")
                .setMessage("Are you sure you want to delete this category?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                    Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app").getReference("categories").child(id).removeValue()
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_category)
                .setCancelable(false)
                .show()
    }
}