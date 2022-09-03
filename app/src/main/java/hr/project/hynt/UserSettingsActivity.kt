package hr.project.hynt

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import hr.project.hynt.FirebaseDatabase.Address
import hr.project.hynt.FirebaseDatabase.User
import java.text.SimpleDateFormat
import java.util.*


class UserSettingsActivity: AppCompatActivity() {


    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.setDisplayShowTitleEnabled(false)

        var user = User()
        val joined = findViewById<TextView>(R.id.user_joined)
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy")

        db.getReference("users").child(authUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue<User>()!!
                    joined.text = simpleDateFormat.format(user!!.timestamp.time).toString()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("Database Error", "Failed to read value.", error.toException())
            }
        })
        val username = findViewById<EditText>(R.id.user_settings_username)
        val email = findViewById<EditText>(R.id.user_settings_email)
        val password = findViewById<Button>(R.id.user_settings_btn_change_password)
        username.setText(authUser!!.displayName.toString())
        email.setText(authUser!!.email.toString())

        username.setOnFocusChangeListener(){ _, hasFocus ->
            if (hasFocus) {
                username.clearFocus()
                val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_editable)

                dialog.findViewById<TextInputLayout>(R.id.dialog_layout).hint = "Username"
                val newData = dialog.findViewById<TextInputEditText>(R.id.dialog_data)
                newData.setText(authUser!!.displayName.toString())
                newData.requestFocus()

                dialog.findViewById<ImageView>(R.id.dialog_cancel).setOnClickListener {
                    dialog.dismiss()
                }
                dialog.findViewById<ImageView>(R.id.dialog_save).setOnClickListener {
                    if (newData.text!!.isEmpty()){
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout).setError("Username cannot be empty!")
                    } else {
                        username.setText(newData.text.toString())
                        dialog.dismiss()
                    }

                }
                dialog.show()
            }
        }
        email.isEnabled = false
        email.setOnFocusChangeListener(){ _, hasFocus ->
            if (hasFocus) {
                email.clearFocus()
                val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_editable)
                dialog.findViewById<TextInputLayout>(R.id.dialog_layout).hint = "Email"
                val newData = dialog.findViewById<TextInputEditText>(R.id.dialog_data)
                newData.setText(authUser!!.email.toString())
                newData.requestFocus()

                dialog.findViewById<ImageView>(R.id.dialog_cancel).setOnClickListener {
                    dialog.dismiss()
                }
                dialog.findViewById<ImageView>(R.id.dialog_save).setOnClickListener {
                    if (newData.text!!.isEmpty()){
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout).setError("Email cannot be empty!")
                    } else {
                        email.setText(newData.text.toString())
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
        }
        password.setOnClickListener {
            val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_editable)

            dialog.findViewById<TextInputLayout>(R.id.dialog_layout).visibility = View.GONE
            dialog.findViewById<LinearLayout>(R.id.dialog_password_layout).visibility = View.VISIBLE

            val oldPass = dialog.findViewById<TextInputEditText>(R.id.dialog_data0)
            val newPass = dialog.findViewById<TextInputEditText>(R.id.dialog_data1)
            val newRPass = dialog.findViewById<TextInputEditText>(R.id.dialog_data2)
            oldPass.requestFocus()
            
            newPass.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,
                                               count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence, start: Int,
                                           before: Int, count: Int) {
                    if (s.length < 6 && s.length > 0) {
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout1).error = "Password must have at least 6 characters"
                    } else {
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout1).error = null
                    }
                    if (s.toString() != newRPass.text.toString()) {
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).error = "Passwords do not match"
                    } else {
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).error = null
                    }
                }
            })
            newRPass.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,
                                               count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence, start: Int,
                                           before: Int, count: Int) {
                    if (s.toString() != newPass.text.toString()) {
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).error = "Passwords do not match"
                    } else {
                        dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).error = null
                    }
                }
            })

            dialog.findViewById<TextInputLayout>(R.id.dialog_layout0).errorIconDrawable = null
            dialog.findViewById<TextInputLayout>(R.id.dialog_layout1).errorIconDrawable = null
            dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).errorIconDrawable = null
            dialog.findViewById<ImageView>(R.id.dialog_cancel).setOnClickListener {
                dialog.dismiss()
            }
            dialog.findViewById<ImageView>(R.id.dialog_save).setOnClickListener {
                var error = false
                if (oldPass.text!!.isEmpty()){
                    dialog.findViewById<TextInputLayout>(R.id.dialog_layout0).error = "Paswword is needed!"
                    error = true
                }
                if (newPass.text!!.isEmpty()){
                    dialog.findViewById<TextInputLayout>(R.id.dialog_layout1).error = "New password cannot be empty!"
                    error = true
                } else if (newPass.text!!.toString().length < 6) {
                    dialog.findViewById<TextInputLayout>(R.id.dialog_layout1).error = "Password must have at least 6 characters"
                    error = true
                }
                if (newRPass.text!!.isEmpty()){
                    dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).error = "You must repeat the password!"
                    error = true
                }
                if (newPass.text.toString()!! != newRPass.text.toString()!!) {
                    dialog.findViewById<TextInputLayout>(R.id.dialog_layout2).error = "Passwords do not match"
                    error = true
                }
                if (!error) {
                    dialog.findViewById<ProgressBar>(R.id.dialog_pass_progressBar).visibility = View.VISIBLE
                    val credentials = EmailAuthProvider.getCredential(authUser.email!!, oldPass.text.toString())
                    authUser.reauthenticate(credentials)
                            .addOnCompleteListener(OnCompleteListener<Void?> { task ->
                                dialog.findViewById<ProgressBar>(R.id.dialog_pass_progressBar).visibility = View.GONE
                                if (task.isSuccessful) {
                                    authUser.updatePassword(newPass.text.toString())
                                            .addOnCompleteListener(OnCompleteListener<Void?> { task ->
                                                if (task.isSuccessful) {
                                                    dialog.dismiss()
                                                    show_info_dialog("Password was changed successfully", true)
                                                }
                                            })
                                } else {
                                    show_info_dialog("Authentication failed, check your password", false)

                                }
                                dialog.findViewById<ProgressBar>(R.id.dialog_pass_progressBar).visibility = View.INVISIBLE
                            })
                }
            }
            dialog.show()

        }

        findViewById<Button>(R.id.btn_save_settings).setOnClickListener {
            val username_data = username.text.toString()

            val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username_data).build()
            authUser.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("User profile", "User profile updated.")
                        }
                    }
            db.getReference("users").child(authUser.uid).child("username").setValue(username_data)
            db.getReference("users").child(authUser.uid).child("reviews").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (reviews: DataSnapshot in snapshot.children) {
                            db.getReference("places").child(reviews.key.toString()).child("reviews").child(authUser.uid).child("refName").setValue(username_data)
                        }
                    }
                    show_info_dialog("Username changed successfully", true)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w("Database Error", "Failed to read value.", error.toException())
                }
            })
        }


        findViewById<Button>(R.id.user_settings_sign_out).setOnClickListener {
            logOut()
        }



    }

    private fun logOut(){
        AlertDialog.Builder(this)
                .setTitle("Sign out")
                .setMessage(authUser!!.displayName + ", are you sure you want to sign out?")
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                    FirebaseAuth.getInstance().signOut()
                    getSharedPreferences("MySharedPref", Context.MODE_PRIVATE).edit().remove("Role").apply()
                    val intent = Intent(this, LaunchActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finishAffinity()
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_log_out)
                .setCancelable(false)
                .show()

    }
    private fun show_info_dialog(text : String, success : Boolean){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        if (success) {
            dialog.setContentView(R.layout.dialog_info_success)
        } else {
            dialog.setContentView(R.layout.dialog_info_failed)
        }
        dialog.findViewById<TextView>(R.id.info_text).text = text
        dialog.findViewById<Button>(R.id.btn_continue).setOnClickListener { dialog.dismiss() }
        dialog.show()
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



}

