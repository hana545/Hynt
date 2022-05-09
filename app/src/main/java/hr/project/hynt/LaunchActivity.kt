package hr.project.hynt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class LaunchActivity : AppCompatActivity() {

    var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            val intent = Intent(this, MainMapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent)
            db.getReference("roles").child(authUser!!.uid).addValueEventListener(object:
                ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val role = snapshot.getValue<String>()
                        val shPref = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                        val editor = shPref.edit()
                        editor.putString("Role", role.toString()).apply()
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })

        }
        supportActionBar!!.hide()
        val btn_map = findViewById<Button>(R.id.btn_guest)
        val btn_login = findViewById<Button>(R.id.btn_login)
        val btn_register = findViewById<Button>(R.id.btn_register)
        btn_map.setOnClickListener {
            val intent = Intent(this, MainMapActivity::class.java)
            startActivity(intent)
        }
        btn_login.setOnClickListener {
            val intent = Intent(this.applicationContext, AuthorizationActivity::class.java)
            intent.putExtra("fragment","login")
            startActivity(intent)
        }
        btn_register.setOnClickListener {
            val intent = Intent(this.applicationContext, AuthorizationActivity::class.java)
            intent.putExtra("fragment","register")
            startActivity(intent)
        }
    }

    override fun onRestart() {
        super.onRestart()
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}