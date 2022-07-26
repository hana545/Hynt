package hr.project.hynt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import hr.project.hynt.FirebaseDatabase.User
import java.util.*

class RegisterFragment : Fragment() {

    var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    private var mAuth = FirebaseAuth.getInstance()

    private var username_edittext: EditText? = null
    private var username_error_view: TextView? = null
    private var email_edittext: EditText? = null
    private var email_error_view: TextView? = null
    private var password_edittext: EditText? = null
    private var password_error_view: TextView? = null
    private var password_confirm_edittext: EditText? = null
    private var password_confirm_error_view: TextView? = null
    private var btnForgotPassword: TextView? = null
    private var btnRegister: Button? = null
    private var btnGoogleSignin: Button? = null
    private var progresBar: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        username_edittext = requireActivity().findViewById<View>(R.id.register_username) as EditText
        username_error_view = requireActivity().findViewById<View>(R.id.register_username_error) as TextView
        email_edittext = requireActivity().findViewById<View>(R.id.register_email) as EditText
        email_error_view = requireActivity().findViewById<View>(R.id.register_email_error) as TextView
        password_edittext = requireActivity().findViewById<View>(R.id.register_password) as EditText
        password_error_view = requireActivity().findViewById<View>(R.id.register_password_error) as TextView
        password_confirm_edittext = requireActivity().findViewById<View>(R.id.register_password_confirm) as EditText
        password_confirm_error_view = requireActivity().findViewById<View>(R.id.register_password__conf_error) as TextView
        btnRegister = requireActivity().findViewById<View>(R.id.btn_register) as Button
        progresBar = requireActivity().findViewById<View>(R.id.register_progress_bar) as ProgressBar
        clearErrors()

        btnRegister!!.setOnClickListener {
            closeKeyboard()
            clearErrors()
            registerUser()
        }
    }

    private fun clearErrors() {
        username_error_view?.setText("")
        username_error_view?.setVisibility(View.GONE)
        email_error_view?.setText("")
        email_error_view?.setVisibility(View.GONE)
        password_error_view?.setText("")
        password_error_view?.setVisibility(View.GONE)
        password_confirm_error_view?.setText("")
        password_confirm_error_view?.setVisibility(View.GONE)
    }

    private fun registerUser() {
        val email = email_edittext!!.text.toString()
        val username: String = username_edittext!!.text.toString()
        val password = password_edittext!!.text.toString()
        val password_confirm = password_confirm_edittext!!.text.toString()

        var username_error = ""
        var email_error = ""
        var password_error = ""
        var password_confirm_error = ""

        var validate = false
        if (email.isEmpty()) {
            email_edittext!!.requestFocus()
            email_error = "Email can not be empty"
            email_error_view?.setText(email_error)
            email_error_view?.setVisibility(View.VISIBLE)
            validate = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_edittext!!.requestFocus()
            email_error = "Enter valid email address"
            email_error_view?.setText(email_error)
            email_error_view?.setVisibility(View.VISIBLE)
            validate = true
        }
        if (username.isEmpty()) {
            username_edittext!!.requestFocus()
            username_error = "Username can not be empty"
            username_error_view?.setText(username_error)
            username_error_view?.setVisibility(View.VISIBLE)
            validate = true

        }
        if (password.isEmpty()) {
            password_edittext!!.requestFocus()
            password_error = "Password can not be empty"
            password_error_view?.setText(password_error)
            password_error_view?.setVisibility(View.VISIBLE)
            validate = true
            return
        } else if (password.length < 6) {
            password_edittext!!.requestFocus()
            password_error = "Password must have at least 6 characters"
            password_error_view?.setText(password_error)
            password_error_view?.setVisibility(View.VISIBLE)
            validate = true
        } else {
            if (!password_confirm.equals(password)){
                password_confirm_edittext!!.requestFocus()
                password_confirm_error = "Passwords do not match"
                password_confirm_error_view?.setText(password_confirm_error)
                password_confirm_error_view?.setVisibility(View.VISIBLE)
                validate = true
            }
        }
        if (validate) return
        progresBar!!.visibility = View.VISIBLE
        //register user with auth
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity(),
                    OnCompleteListener<AuthResult?> { task ->
                        //add user to database
                        if (task.isSuccessful) {
                            val Fuser = FirebaseAuth.getInstance().currentUser
                            val user = User(Calendar.getInstance().time, username, email)
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(username).build()
                            Fuser!!.updateProfile(profileUpdates)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d("User profile", "User profile updated.")
                                            db.getReference("users").child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(user).addOnCompleteListener { task ->
                                                Log.d("User profile", "User profile created")
                                                if (task.isSuccessful) {
                                                    db.getReference("roles").child(FirebaseAuth.getInstance().currentUser!!.uid).setValue("user")
                                                    Toast.makeText(activity, "User has been registered successfully!", Toast.LENGTH_LONG).show()
                                                    val shPref = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                                                    val editor = shPref.edit()
                                                    editor.putString("Role", "user").apply()
                                                    val intent = Intent(requireActivity(), MainMapActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent)
                                                    requireActivity().finish()

                                                } else {
                                                    progresBar!!.visibility = View.INVISIBLE
                                                    Toast.makeText(
                                                            activity,
                                                            "Failed to register!.",
                                                            Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "createUserWithEmail:failure", task.exception)
                            progresBar!!.visibility = View.INVISIBLE
                            Toast.makeText(
                                    activity,
                                    "Failed to register! Try again.",
                                    Toast.LENGTH_LONG
                            ).show()
                        }
                    })
    }
    private fun closeKeyboard() {
        val view: View? = requireActivity().currentFocus
        if (view != null) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}