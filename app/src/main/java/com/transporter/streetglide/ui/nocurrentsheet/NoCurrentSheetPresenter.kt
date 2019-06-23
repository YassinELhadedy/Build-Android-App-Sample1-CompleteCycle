package com.transporter.streetglide.ui.nocurrentsheet

import com.transporter.streetglide.models.Pagination
import com.transporter.streetglide.models.exception.SheetNotFoundException
import com.transporter.streetglide.models.services.RequestSheetService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 *Created by yassin on 5/7/18.
 */
class NoCurrentSheetPresenter(private val requestSheetService: RequestSheetService) : NoCurrentSheetContract.Presenter {

    private lateinit var view: NoCurrentSheetContract.View

    override fun subscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribe() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setView(view: NoCurrentSheetContract.View) {
        this.view = view
    }

    override fun requestSheet(pagination: Pagination) {
        view.showLoading()
        requestSheetService.requestSheet(pagination)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    view.hideLoading()
                    if (it == true) {
                        view.showValid(1)
                    } else {
                        view.showValid(2)
                    }
                }, { e ->
                    view.hideLoading()
                    view.showError(e)
                    if (e is SheetNotFoundException) {
                        view.showValid(2)
                    }
                })
    }
}