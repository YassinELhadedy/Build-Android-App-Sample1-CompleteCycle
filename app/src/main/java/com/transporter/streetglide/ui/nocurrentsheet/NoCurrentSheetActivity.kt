package com.transporter.streetglide.ui.nocurrentsheet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog
import com.transporter.streetglide.R
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.SheetSuperGlideRepository
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.services.RequestSheetService
import com.transporter.streetglide.ui.app.StreetglideApp
import com.transporter.streetglide.ui.exception.ErrorMessageFactory
import com.transporter.streetglide.ui.helpers.Helper.Companion.isThereInternetConnection
import com.transporter.streetglide.ui.helpers.Helper.Companion.showSnackBar
import com.transporter.streetglide.ui.scanvalidation.StartValidationActivity
import java.util.*

class NoCurrentSheetActivity : AppCompatActivity(), NoCurrentSheetContract.View {

    private lateinit var noCurrentSheetPresenter: NoCurrentSheetContract.Presenter
    private lateinit var progressDialog: SweetAlertDialog
    private lateinit var noCurrentSheetConstraint: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_current_sheet)
        initView()
        initPresenter()
        noCurrentSheetPresenter.setView(this)
    }

    private fun initPresenter() {
        //FIXME: Create instance of presenter
        val daoSession = (applicationContext as StreetglideApp).getDaoSession()
        val superGlideRestService = (applicationContext as StreetglideApp).getServiceInstance()
        val configurationRepository = (applicationContext as StreetglideApp).getConfigRepoInstance()
        val sheetDiskRepo = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)
        val sheetSuperGlideRepository = SheetSuperGlideRepository(superGlideRestService, configurationRepository)
        val requestSheetService = RequestSheetService(sheetSuperGlideRepository, sheetDiskRepo, configurationRepository)
        noCurrentSheetPresenter = NoCurrentSheetPresenter(requestSheetService)
    }

    override fun initView() {
        progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.progressHelper.barColor = Color.parseColor("#A5DC86")
        noCurrentSheetConstraint = findViewById<View>(R.id.nocurrent_sheet_view)
        val dateEX1 = 1463646433000L //Thu Feb 09 07:09:59 GMT+02:00 2017
        val expr1 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX1))
        val sort1 = SortBy("deliveryRunSheet.dateTime", Ascending())
        val btRequestSheet = findViewById<Button>(R.id.btn_nocurrentsheet_refresh)
        btRequestSheet.setOnClickListener {
            if (!isThereInternetConnection(this)) {
                showSnackBar(noCurrentSheetConstraint, getString(R.string.exception_message_no_connection))
            } else {
                noCurrentSheetPresenter.requestSheet(Pagination(expr1, sort1, 0, 1))
            }
        }
    }

    override fun showLoading() {
        progressDialog.titleText = "Loading"
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    override fun hideLoading() {
        progressDialog.dismiss()
    }

    override fun showError(error: Throwable) {
        val errorMessage = ErrorMessageFactory.create(this, error)
        showSnackBar(noCurrentSheetConstraint, errorMessage)

    }

    override fun showValid(messageKey: Int) {
        when (messageKey) {
            1 -> {
                showSnackBar(noCurrentSheetConstraint, getString(R.string.sheet_existed))
                val i = Intent(this, StartValidationActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
            2 -> {
                showSnackBar(noCurrentSheetConstraint, getString(R.string.sheet_not_existed))
//                val i = Intent(this, NoCurrentSheetActivity::class.java)
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                startActivity(i)
            }
        }
    }
}