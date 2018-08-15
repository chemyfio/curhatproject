package com.porto.curhat.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.porto.curhat.R

class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onClick(view: View) {
        var id:Int = view.id
        if(id == R.id.btnGoToLogin){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else if(id == R.id.btnGoToRegister){
//            val intent = Intent(this, RegisterActivity::class.java)
//            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
