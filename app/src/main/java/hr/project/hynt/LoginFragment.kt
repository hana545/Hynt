package hr.project.hynt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    private val mAuth = FirebaseAuth.getInstance()

    private var email_edittext: EditText? = null
    private var email_error_view: TextView? = null
    private var password_edittext: EditText? = null
    private var password_error_view: TextView? = null
    private var btnForgotPassword: TextView? = null
    private var btnLogin: Button? = null
    private var btnGoogleSignin: Button? = null
    private var progresBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)

    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        email_edittext = requireActivity().findViewById<View>(R.id.login_email) as EditText
        email_error_view = requireActivity().findViewById<View>(R.id.login_email_error) as TextView
        password_edittext = requireActivity().findViewById<View>(R.id.login_password) as EditText
        password_error_view = requireActivity().findViewById<View>(R.id.login_password_error) as TextView
        btnLogin = requireActivity().findViewById<View>(R.id.btn_login) as Button
        progresBar = requireActivity().findViewById<View>(R.id.login_progress_bar) as ProgressBar
        clearErrors()
        btnLogin!!.setOnClickListener {
            closeKeyboard()
            clearErrors()
            loginUser()
        }
    }

    private fun clearErrors() {
        email_error_view?.setText("")
        email_error_view?.setVisibility(View.GONE)
        password_error_view?.setText("")
        password_error_view?.setVisibility(View.GONE)
    }


    private fun loginUser() {
        val email = email_edittext!!.text.toString()
        val password = password_edittext!!.text.toString()
        var email_error = ""
        var password_error = ""
        var error = false
        if (email.isEmpty()) {
            email_edittext!!.requestFocus()
            email_error = "Email can not be empty"
            email_error_view?.setText(email_error)
            email_error_view?.setVisibility(View.VISIBLE)
            error = true

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_edittext!!.requestFocus()
            email_error = "Enter valid email address"
            email_error_view?.setText(email_error)
            email_error_view?.setVisibility(View.VISIBLE)
            error = true
        }
        if (password.isEmpty()) {
            password_edittext!!.requestFocus()
            password_error = "Password can not be empty"
            password_error_view?.setText(password_error)
            password_error_view?.setVisibility(View.VISIBLE)
            error = true
        }
        if (error) return
        progresBar!!.visibility = View.VISIBLE
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = Intent(activity, MainMapActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent)
                db.getReference("roles").child(FirebaseAuth.getInstance().currentUser!!.uid).addValueEventListener(object: ValueEventListener {
                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val role = snapshot.getValue<String>()

                            val shPref = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                            val editor = shPref.edit()
                            editor.putString("Role", role.toString()).apply()
                            Log.d("LOGIN-Role", "puts " + role.toString())

                            requireActivity().finish()
                        } else {
                            Log.d("LOGIN-Role", "no snapshot: ")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            } else {
                progresBar!!.visibility = View.INVISIBLE
                Toast.makeText(activity,"Failed to login! Check you credentials", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun closeKeyboard() {
        val view: View? = requireActivity().currentFocus
        if (view != null) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }



}