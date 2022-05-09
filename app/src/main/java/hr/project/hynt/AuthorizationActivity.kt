package hr.project.hynt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class AuthorizationActivity : AppCompatActivity() {

    private var text_account: TextView? = null
    private var btn_auth: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authorization)

        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            val intent = Intent(this, MainMapActivity::class.java)
            startActivity(intent)
            finish()
        }
        supportActionBar!!.hide()

        text_account = findViewById<View>(R.id.text_account) as TextView
        btn_auth = findViewById<View>(R.id.btn_auth) as Button

        var fragment = intent.getStringExtra("fragment")

        if (fragment != null) {
            if (fragment == "login"){
                goToLogin()
            } else if(fragment == "register"){
               goToRegister()
            }
        }
        btn_auth!!.setOnClickListener(View.OnClickListener {
            if (fragment == "login"){
                goToRegister()
                fragment = "register"

            } else if(fragment == "register") {
                goToLogin()
                fragment = "login"

            }

        })

    }
    private fun goToLogin(){
        val fram = supportFragmentManager.beginTransaction()
        fram.replace(R.id.fragment_main, LoginFragment())
        fram.commit()
        text_account!!.text = resources.getString(R.string.auth_txt_not_account)
        btn_auth!!.text = resources.getString(R.string.btn_register)
    }
    private fun goToRegister(){
        val fram = supportFragmentManager.beginTransaction()
        fram.replace(R.id.fragment_main, RegisterFragment())
        fram.commit()
        text_account!!.text = resources.getString(R.string.auth_txt_already_account)
        btn_auth!!.text = resources.getString(R.string.btn_login)
    }

    override fun onRestart() {
        super.onRestart()
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            val intent = Intent(this, MainMapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


}
