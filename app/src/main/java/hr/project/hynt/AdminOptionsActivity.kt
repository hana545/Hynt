package hr.project.hynt

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class AdminOptionsActivity : AppCompatActivity() {


    var active_fragment = ""
    val authUser = FirebaseAuth.getInstance().currentUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_options)

        val sh = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        if (authUser == null && sh.getString("Role", "").toString() != "admin") {
            finish()
        }
        setCustomActionBar()

        if (intent.getStringExtra("fragment") == "tags") {
            loadFragment(AdminManageTagsFragment())
            active_fragment = "tags"
        } else if (intent.getStringExtra("fragment") == "categories") {
            loadFragment(AdminManageCategoriesFragment())
            active_fragment = "categories"
        } else {
            loadFragment(AdminManagePlacesFragment())
            active_fragment = "places"
        }
        val bottomNavigationView = findViewById<BottomNavigationView
                >(R.id.admin_bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.admin_manage_places -> {
                    if (active_fragment != "places") {
                        active_fragment = "places"
                        loadFragment(AdminManagePlacesFragment())
                    }
                    true
                }
                R.id.admin_tags -> {
                    if (active_fragment != "tags") {
                        active_fragment = "tags"
                        loadFragment(AdminManageTagsFragment())
                    }
                    true
                }
                R.id.admin_categories -> {
                    if (active_fragment != "categories") {
                        active_fragment = "categories"
                        loadFragment(AdminManageCategoriesFragment())
                    }
                    true
                }
                else -> false
            }

        }

    }
    private fun loadFragment(fragment: Fragment) {
        // load fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .commit()
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
        val btn_user : LinearLayout = customView.findViewById<View>(R.id.btn_user_profile) as LinearLayout
        if (authUser != null) {
            val username : TextView = btn_user.findViewById(R.id.user_username) as TextView
            username.setText(authUser.displayName)
            val sh = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            btn_user.setOnClickListener(View.OnClickListener {
                showBottomSheetDialogUserOptions(sh.getString("Role", "").toString())
            })

        } else {
            btn_user.visibility = View.GONE
        }
    }
    private fun showBottomSheetDialogUserOptions(role : String) {

        val bottomSheetDialog = BottomSheetDialog(this)
        if(role == "admin") {
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_admin_options)
            val manage_places = bottomSheetDialog.findViewById<LinearLayout>(R.id.manage_places)
            manage_places!!.setOnClickListener(View.OnClickListener {
                if (active_fragment != "places") {
                    active_fragment = "place"
                    loadFragment(AdminManagePlacesFragment())
                }
                bottomSheetDialog.dismiss()
            })
            val tags = bottomSheetDialog.findViewById<LinearLayout>(R.id.tags)
            tags!!.setOnClickListener(View.OnClickListener {
                if (active_fragment != "tags") {
                    active_fragment = "tags"
                    loadFragment(AdminManageTagsFragment())
                }
                bottomSheetDialog.dismiss()
            })
            val categories = bottomSheetDialog.findViewById<LinearLayout>(R.id.categories)
            categories!!.setOnClickListener(View.OnClickListener {
                if (active_fragment != "categories") {
                    active_fragment = "categories"
                    loadFragment(AdminManageCategoriesFragment())
                }
                bottomSheetDialog.dismiss()
            })
        } else {
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_users_options)
        }
        val add_place = bottomSheetDialog.findViewById<LinearLayout>(R.id.add_place)
        add_place!!.setOnClickListener(View.OnClickListener {
            //showAddPlaceDialog()
            bottomSheetDialog.dismiss();
        })
        val my_addresses = bottomSheetDialog.findViewById<LinearLayout>(R.id.myAddresses)
        my_addresses!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val my_reviews = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_reviews)
        my_reviews!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val my_places = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_places)
        my_places!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val settings = bottomSheetDialog.findViewById<LinearLayout>(R.id.settings)
        settings!!.setOnClickListener(View.OnClickListener {
            bottomSheetDialog.dismiss();
        })
        val signout = bottomSheetDialog.findViewById<LinearLayout>(R.id.signOut)
        signout!!.setOnClickListener(View.OnClickListener {
            signOut()
            bottomSheetDialog.dismiss();
        })
        bottomSheetDialog.show()
    }
    private fun signOut(){
        FirebaseAuth.getInstance().signOut()
        getSharedPreferences("MySharedPref",  Context.MODE_PRIVATE).edit().remove("Role").apply()
        val intent = Intent(applicationContext, LaunchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent)
        finish()
    }


}