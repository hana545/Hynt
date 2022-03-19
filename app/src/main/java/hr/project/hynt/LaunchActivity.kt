package hr.project.hynt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        //dodat za prepoznavanje usera

        val btn_map = findViewById<Button>(R.id.btn_guest)
        val btn_login = findViewById<Button>(R.id.btn_login)
        val btn_register = findViewById<Button>(R.id.btn_register)
        btn_map.setOnClickListener {
            val intent = Intent(this, MainMapActivity::class.java)
            startActivity(intent)
        }
        btn_login.setOnClickListener {
            //val intent = Intent(applicationContext, MapActivity::class.java)
            //startActivity(intent)
        }
    }

}