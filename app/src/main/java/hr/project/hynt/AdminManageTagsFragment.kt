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


class AdminManageTagsFragment : Fragment(), TagCategoryAdapter.ItemClickListener {

    var allTags_id = ArrayList<String>()
    var allTags = ArrayList<String>()
    var collectiveTags = hashMapOf<String, String>()

    lateinit var text_info : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_manage_tags, container, false)
        text_info = view.findViewById(R.id.fragment_info)
        val btn_addTag = view.findViewById<FloatingActionButton>(R.id.btn_add_new_tag)
        btn_addTag.setOnClickListener(View.OnClickListener {
            showAddTagDialog()
        })

        val recyclerview = view.findViewById<RecyclerView>(R.id.tag_recyclerView)
        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(context)

        val adapter = TagCategoryAdapter(allTags, allTags_id, this)
        recyclerview.adapter = adapter
        getAllTags(adapter)
        return view
    }

    private fun getAllTags(adapter: TagCategoryAdapter) {
        var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
        val places_query = db.getReference("tags")
        places_query.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTags.clear()
                allTags_id.clear()
                collectiveTags.clear()
                if (snapshot.exists()) {
                    for (tags : DataSnapshot in snapshot.children) {
                        val tag = tags.getValue<String>()
                        val tag_id = tags.key.toString()
                        if (tag != null) {
                            collectiveTags.put(tag_id,tag)
                        }
                    }
                }
                text_info.visibility = if (collectiveTags.isEmpty()) View.VISIBLE else View.GONE
                val result = collectiveTags.toList().sortedBy {  (_, value) -> value}.toMap()
                allTags.addAll(result.values)
                allTags_id.addAll(result.keys)
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }

        })
    }

    private fun showAddTagDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_add_new_tag)

        val tag_name : EditText = dialog.findViewById<EditText>(R.id.add_tag_data)

        val btn_add_tag : Button = dialog.findViewById(R.id.btn_add_tag)
        btn_add_tag.setOnClickListener {
            val tag_name_text = tag_name.text.toString()
            if (tag_name_text.isEmpty()) {
                tag_name.requestFocus()
                tag_name.error = "Name cannot be empty"

            } else {
                TagCategory().addTagCategory(TagCategory(tag_name_text), "tags")

                dialog.dismiss()
            }
        }

        dialog.show()
    }
    override fun onItemClick(id: String) {
        AlertDialog.Builder(activity)
                .setTitle("Delete tag")
                .setMessage("Are you sure you want to delete this tag?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _, _ ->
                    Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app").getReference("tags").child(id).removeValue()
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_tag)
                .setCancelable(false)
                .show()
    }
}