package com.transporter.streetglide.ui.scanvalidation

import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import com.transporter.streetglide.R
import com.transporter.streetglide.ui.BaseView

class StartValidationActivity : AppCompatActivity(), BaseView {

    private lateinit var btnStartValidation: ConstraintLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun initView() {
        setContentView(R.layout.activity_start_validation)
        btnStartValidation = findViewById(R.id.constraintlayout_start_validation_btn)
        btnStartValidation.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
    }
}