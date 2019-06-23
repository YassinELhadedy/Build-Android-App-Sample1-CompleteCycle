package com.transporter.streetglide.ui.runnerlogin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog
import com.transporter.streetglide.R
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.SheetSuperGlideRepository
import com.transporter.streetglide.infrastructure.TokenSuperGlideRepository
import com.transporter.streetglide.infrastructure.UserSuperGlideRepository
import com.transporter.streetglide.models.*
import com.transporter.streetglide.models.services.GetRunnerIdService
import com.transporter.streetglide.models.services.LoginService
import com.transporter.streetglide.models.services.RequestSheetService
import com.transporter.streetglide.ui.app.StreetglideApp
import com.transporter.streetglide.ui.exception.ErrorMessageFactory
import com.transporter.streetglide.ui.helpers.Helper.Companion.isThereInternetConnection
import com.transporter.streetglide.ui.helpers.Helper.Companion.showSnackBar
import com.transporter.streetglide.ui.nocurrentsheet.NoCurrentSheetActivity
import com.transporter.streetglide.ui.scanvalidation.StartValidationActivity
import java.util.*

class LoginFragment : Fragment(), LoginContract.View {

    private lateinit var loginPresenter: LoginContract.Presenter
    private lateinit var progressDialog: SweetAlertDialog
    private lateinit var loginConstraint: View
    private lateinit var loginView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        loginView = inflater.inflate(R.layout.fragment_login, container, false)
        initView()
        initPresenter()
        loginPresenter.setView(this)
        return loginView
    }

    private fun loginUser(userName: String, pass: String, pagination: Pagination) {
        if (!isThereInternetConnection(requireContext())) {
            showSnackBar(loginConstraint, requireContext().getString(R.string.exception_message_no_connection))
        } else {
            if (userName.isEmpty() || pass.isEmpty()) {
                showSnackBar(loginConstraint, requireContext().getString(R.string.empty_login_fields))
            } else {
                loginPresenter.login(userName, pass, pagination)
            }
        }
    }

    private fun initPresenter() {
        //FIXME: Create instance of presenter
        val daoSession = (requireContext().applicationContext as StreetglideApp).getDaoSession()
        val superGlideRestService = (requireContext().applicationContext as StreetglideApp).getServiceInstance()
        val configurationRepository = (requireContext().applicationContext as StreetglideApp).getConfigRepoInstance()
        val sheetDiskRepo = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)
        val sheetSuperGlideRepository = SheetSuperGlideRepository(superGlideRestService, configurationRepository)
        val tokenSuperGlideRepository = TokenSuperGlideRepository(superGlideRestService)
        val loginService = LoginService(tokenSuperGlideRepository, configurationRepository)
        val requestSheetService = RequestSheetService(sheetSuperGlideRepository, sheetDiskRepo, configurationRepository)
        val getRunnerIdService = GetRunnerIdService(UserSuperGlideRepository(superGlideRestService, configurationRepository), configurationRepository)
        loginPresenter = LoginPresenter(loginService, requestSheetService, getRunnerIdService)
    }

    override fun initView() {
        progressDialog = SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.progressHelper.barColor = Color.parseColor("#A5DC86")
        loginConstraint = loginView.findViewById<View>(R.id.login_fragment)
        val etUsername = loginView.findViewById<EditText>(R.id.et_user_name)
        val etPassword = loginView.findViewById<EditText>(R.id.et_password)
        val btnLogin = loginView.findViewById<Button>(R.id.btn_login)
        val dateEX1 = 1486616999000L //Thu Feb 09 07:09:59 GMT+02:00 2017
//        val dateEX1 = 1463646433000L
        val expr1 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX1))
        val sort1 = SortBy("deliveryRunSheet.dateTime", Ascending())

        btnLogin.setOnClickListener {
            loginUser(etUsername.text.toString(), etPassword.text.toString(), Pagination(expr1, sort1, 0, 1))
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
        val errorMessage = ErrorMessageFactory.create(requireContext(), error)
        showSnackBar(loginConstraint, errorMessage)
    }

    override fun showValid(messageKey: Int) {
        when (messageKey) {
            1 -> {
                showSnackBar(loginConstraint, requireContext().getString(R.string.sheet_existed))
                val i = Intent(requireContext(), StartValidationActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                requireContext().startActivity(i)
                requireActivity().finish()
            }
            2 -> {
                showSnackBar(loginConstraint, requireContext().getString(R.string.sheet_not_existed))
                val i = Intent(requireContext(), NoCurrentSheetActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                requireContext().startActivity(i)
                requireActivity().finish()
            }
        }
    }
}