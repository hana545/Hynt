package hr.project.hynt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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


class SplashScreenActivity : AppCompatActivity() {

    var db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        supportActionBar!!.hide()
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            Handler(Looper.getMainLooper()).postDelayed ({
            val intent = Intent(this, MainMapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
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
            },10)

        } else {
            val intent = Intent(this, LaunchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
        }

    }

    override fun onRestart() {
        super.onRestart()
        finish()

    }

    override fun onDestroy() {
        super.onDestroy()
    }

}