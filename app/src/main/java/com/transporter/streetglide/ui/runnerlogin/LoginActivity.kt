package com.transporter.streetglide.ui.runnerlogin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.transporter.streetglide.R

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportFragmentManager.beginTransaction().add(R.id.login_fragment, LoginFragment()).commit()
    }
}