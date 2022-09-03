package hr.project.hynt

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.SignInMethodQueryResult
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
        progresBar = requireActivity().findViewById<View>(R.id.login_progress_bar) as ProgressBar
        val btnLogin = requireActivity().findViewById<View>(R.id.btn_login) as Button
        val btnForgotPassword = requireActivity().findViewById<View>(R.id.btn_forgot_password) as Button
        clearErrors()
        btnLogin.setOnClickListener {
            closeKeyboard()
            clearErrors()
            loginUser()
        }
        btnForgotPassword.setOnClickListener {
            val dialog = Dialog(requireActivity())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setContentView(R.layout.dialog_reset_password)
            val email = dialog.findViewById<TextInputEditText>(R.id.reset_password_email)
            val email_layout = dialog.findViewById<TextInputLayout>(R.id.reset_password_email_layout)
            email.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,
                                               count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence, start: Int,
                                           before: Int, count: Int) {
                    email_layout.error = null
                }
            })
            dialog.findViewById<Button>(R.id.btn_send_email).setOnClickListener {
                if (email.text!!.isEmpty()){
                    email_layout.error = "Enter email!"
                } else {
                    mAuth.fetchSignInMethodsForEmail(email.text.toString())
                        .addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult?> { task ->
                            val isNewUser = task.result.signInMethods?.isEmpty()
                            if (isNewUser!!) {
                                email_layout.error = "Email not recognized. Try again"
                            } else {
                                closeKeyboard()
                                mAuth.sendPasswordResetEmail(email.text.toString())
                                dialog.dismiss()
                                val noteDialog = Dialog(requireActivity())
                                noteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                noteDialog.setCancelable(true)
                                noteDialog.setCanceledOnTouchOutside(true)
                                noteDialog.setContentView(R.layout.dialog_info_success)
                                noteDialog.findViewById<TextView>(R.id.info_text).text = "Email succesfully sent. Check your inbox."
                                noteDialog.findViewById<Button>(R.id.btn_continue).setOnClickListener {
                                    noteDialog.dismiss()
                                }
                                noteDialog.show()
                            }
                        })
                }

            }
            dialog.show()
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
                val authUser = FirebaseAuth.getInstance().currentUser
                if(!authUser!!.isEmailVerified){
                    progresBar!!.visibility = View.INVISIBLE
                    FirebaseAuth.getInstance().signOut()
                    val dialog = Dialog(requireActivity())
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    dialog.setContentView(R.layout.dialog_info_failed)
                    dialog.findViewById<TextView>(R.id.info_text).text = "Please verify your email and then login."
                    dialog.findViewById<Button>(R.id.btn_continue).text = "Send verification email"
                    dialog.findViewById<Button>(R.id.btn_continue).setOnClickListener {
                        authUser.sendEmailVerification().addOnCompleteListener(
                            OnCompleteListener<Void> { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context,"Verification email sent",Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(context,"Verification email failed",Toast.LENGTH_LONG).show();
                                }
                            })
                    }
                    dialog.show()
                } else {
                    val intent = Intent(activity, MainMapActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent)
                    db.getReference("roles").child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .addValueEventListener(object : ValueEventListener {
                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val role = snapshot.getValue<String>()

                                    val shPref = requireActivity().getSharedPreferences(
                                        "MySharedPref",
                                        Context.MODE_PRIVATE
                                    )
                                    val editor = shPref.edit()
                                    editor.putString("Role", role.toString()).apply()

                                    requireActivity().finish()
                                } else {
                                    Log.d("LOGIN-Role", "no snapshot: ")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                }
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