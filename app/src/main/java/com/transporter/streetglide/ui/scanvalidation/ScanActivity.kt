package com.transporter.streetglide.ui.scanvalidation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.Result
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog
import com.transporter.streetglide.R
import com.transporter.streetglide.infrastructure.ScannedBarcodeRepositoryImp
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.models.services.ValidationService
import com.transporter.streetglide.ui.app.StreetglideApp
import com.transporter.streetglide.ui.discrepancyreport.DiscrepancyActivity
import com.transporter.streetglide.ui.runsheet.StartTripActivity
import com.transporter.streetglide.ui.util.BeepManager
import com.transporter.streetglide.ui.util.ErrorDialog
import com.transporter.streetglide.ui.util.SchedulerProvider
import me.dm7.barcodescanner.zxing.ZXingScannerView


open class ScanActivity : AppCompatActivity(), ZXingScannerView.ResultHandler, ScanValidationContract.View {

    private lateinit var zXingScannerView: ZXingScannerView
    private lateinit var btnDone: Button
    private lateinit var tvCountShipments: TextView
    private lateinit var beepManager: BeepManager
    private lateinit var presenter: ScanValidationContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        initView()
        initPresenter()
        presenter.setView(this)
        beepManager = BeepManager(this)
    }

    private fun initPresenter() {
        val daoSession = (application as StreetglideApp).getDaoSession()
        val configurationRepository = (application as StreetglideApp).getConfigRepoInstance()
        val sheetDiskRepo = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)
        val scannedBarcodeRepo = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)
        val validationService = ValidationService(sheetDiskRepo, scannedBarcodeRepo)
        presenter = ScanValidationPresenter(validationService, SchedulerProvider)
    }

    override fun handleResult(result: Result) {
        Toast.makeText(this, result.text.toString(), Toast.LENGTH_SHORT).show()
        presenter.verifyBarcode(result.text)
        beepManager.playBeepSoundAndVibrate()
        zXingScannerView.resumeCameraPreview(this)
    }

    override fun initView() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_scan_toolbar_custom)
        setSupportActionBar(toolbar)
        zXingScannerView = findViewById(R.id.zxingscanner_scan_scanner_view)
        btnDone = findViewById(R.id.btn_scan_done)
        tvCountShipments = findViewById(R.id.tv_scan_count_shipment)
        checkPermissionOrToScan()
        btnDone.setOnClickListener {
            presenter.checkDiscrepancy()
        }
    }

    override fun startScan() {
        zXingScannerView.setResultHandler(this) // Register ourselves as a handler for scan results.
        zXingScannerView.startCamera()
    }

    override fun showShipmentsCount(count: Int) {
        tvCountShipments.text = count.toString()
    }

    override fun navigateToDiscrepancyReport() {
        startActivity(Intent(this, DiscrepancyActivity::class.java))
    }

    override fun navigateToStartTrip() {
        startActivity(Intent(this, StartTripActivity::class.java))
    }

    override fun showErrorMsgShipmentNotInSheet() {
        ErrorDialog.show(this, getString(R.string.error_msg_shipment_not_in_sheet))
    }

    override fun showErrorMsgShipmentAlreadyScanned() {
        ErrorDialog.show(this, getString(R.string.error_msg_shipment_already_scanned))
    }

    private fun checkPermissionOrToScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                val sweetAlertDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                sweetAlertDialog.setTitleText("إذن ضرورى")
                        .setContentText("لا يمكنك التحقق من الشحنات من فضلك اختار ALLOW / السماح")
                        .setConfirmClickListener {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
                            sweetAlertDialog.dismissWithAnimation()
                        }.show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            }
        } else {
            startScan()
        }
    }

    public override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
        zXingScannerView.stopCamera()
        beepManager.close()
    }

    public override fun onResume() {
        super.onResume()
        checkPermissionOrToScan()
        presenter.subscribe()
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
    }
}