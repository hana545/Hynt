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


class UserOptionsActivity : AppCompatActivity() {


    var active_fragment = ""
    val authUser = FirebaseAuth.getInstance().currentUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_options)

        if (authUser == null) {
            finish()
        }
        setCustomActionBar()

        if (intent.getStringExtra("fragment") == "addresses") {
            loadFragment(UserMyAddressesFragment())
            active_fragment = "addresses"
        } else if (intent.getStringExtra("fragment") == "reviews") {
            loadFragment(UserMyReviewsFragment())
            active_fragment = "reviews"
        } else {
            loadFragment(UserMyPlacesFragment())
            active_fragment = "places"
        }
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.user_bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.user_my_addresses -> {
                    if (active_fragment != "addresses") {
                        active_fragment = "addresses"
                        loadFragment(UserMyAddressesFragment())
                    }
                    true
                }
                R.id.user_my_reviews -> {
                    if (active_fragment != "reviews") {
                        active_fragment = "reviews"
                        loadFragment(UserMyReviewsFragment())
                    }
                    true
                }
                R.id.user_my_places -> {
                    if (active_fragment != "places") {
                        active_fragment = "places"
                        loadFragment(UserMyPlacesFragment())
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
                .replace(R.id.user_fragment_container, fragment)
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
        val btn_home : LinearLayout = customView.findViewById<View>(R.id.btn_home) as LinearLayout
        btn_home.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MainMapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        })
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
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            if (active_fragment != "addresses") {
                active_fragment = "addresses"
                loadFragment(UserMyAddressesFragment())
            }
            bottomSheetDialog.dismiss()
        })
        val my_reviews = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_reviews)
        my_reviews!!.setOnClickListener(View.OnClickListener {
            if (active_fragment != "reviews") {
                active_fragment = "reviews"
                loadFragment(UserMyReviewsFragment())
            }
            bottomSheetDialog.dismiss()
        })
        val my_places = bottomSheetDialog.findViewById<LinearLayout>(R.id.my_places)
        my_places!!.setOnClickListener(View.OnClickListener {
            if (active_fragment != "places") {
                active_fragment = "places"
                loadFragment(UserMyPlacesFragment())
            }
            bottomSheetDialog.dismiss()
        })
        val settings = bottomSheetDialog.findViewById<LinearLayout>(R.id.settings)
        settings!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, UserSettingsActivity::class.java)
            bottomSheetDialog.dismiss()
            startActivity(intent)
        })

        bottomSheetDialog.show()
    }

}