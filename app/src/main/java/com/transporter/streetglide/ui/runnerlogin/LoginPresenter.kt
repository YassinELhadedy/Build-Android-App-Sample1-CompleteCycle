package com.transporter.streetglide.ui.runnerlogin

import com.transporter.streetglide.models.Pagination
import com.transporter.streetglide.models.exception.SheetNotFoundException
import com.transporter.streetglide.models.services.GetRunnerIdService
import com.transporter.streetglide.models.services.LoginService
import com.transporter.streetglide.models.services.RequestSheetService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class LoginPresenter(private val loginService: LoginService, private val requestSheetService: RequestSheetService, private val getRunnerIdService: GetRunnerIdService) : LoginContract.Presenter {

    private lateinit var view: LoginContract.View

    override fun login(username: String, password: String, pagination: Pagination) {
        view.showLoading()
        loginService.login(username, password).flatMap { signedToken ->
            getRunnerIdService.getRunnerId(signedToken.id)
                    .map {
                        it
                    }
            requestSheetService.requestSheet(pagination)
                    .map {
                        it
                    }
        }.map { it }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    view.hideLoading()
                    if (it == true) {
                        view.showValid(1)
                    } else {
                        view.showValid(2)
                    }
                }
                        , { e ->
                    view.hideLoading()
                    if (e is SheetNotFoundException) {
                        view.showValid(2)
                    } else {
                        view.showError(e)
                    }
                })
    }

    override fun setView(view: LoginContract.View) {
        this.view = view
    }

    override fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}