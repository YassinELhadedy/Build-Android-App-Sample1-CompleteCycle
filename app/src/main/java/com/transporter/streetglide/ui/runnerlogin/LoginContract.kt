package com.transporter.streetglide.ui.runnerlogin

import com.transporter.streetglide.models.Pagination
import com.transporter.streetglide.ui.BasePresenter
import com.transporter.streetglide.ui.BaseView

interface LoginContract {

    interface View : BaseView {
        fun showLoading()

        fun hideLoading()

        fun showError(error: Throwable)

        fun showValid(messageKey: Int)
    }

    interface Presenter : BasePresenter<View> {
        fun login(username: String, password: String, pagination: Pagination)
    }
}