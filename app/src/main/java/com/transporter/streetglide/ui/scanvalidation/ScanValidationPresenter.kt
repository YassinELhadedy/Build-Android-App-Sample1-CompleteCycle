package com.transporter.streetglide.ui.scanvalidation

import com.transporter.streetglide.models.services.ValidationService
import com.transporter.streetglide.ui.util.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable


open class ScanValidationPresenter(private val validationService: ValidationService,
                                   private val scheduleProvider: BaseSchedulerProvider) : ScanValidationContract.Presenter {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var scannedShipmentsCount = 0
    private lateinit var view: ScanValidationContract.View

    override fun verifyBarcode(barcode: String) {
        val disposable = this.validationService.verifyBarcode(barcode)
                .subscribeOn(scheduleProvider.io())
                .observeOn(scheduleProvider.ui())
                .subscribe({
                    if (it.first) {
                        view.showErrorMsgShipmentAlreadyScanned()
                    } else if (!it.second) {
                        view.showErrorMsgShipmentNotInSheet()
                        view.showShipmentsCount(scannedShipmentsCount.inc())
                    } else {
                        view.showShipmentsCount(scannedShipmentsCount.inc())
                    }
                })

        compositeDisposable.add(disposable)
    }

    /* this function not actual implementation because it's depend on shipments consildation story,
       it's created only for testing navigation */
    override fun checkDiscrepancy() {
        val disposable = this.validationService.isThereAnyScannedShipmentNotFoundInSheet()
                .subscribeOn(scheduleProvider.io())
                .observeOn(scheduleProvider.ui())
                .subscribe {
                    if (it) {
                        view.navigateToDiscrepancyReport()
                    } else {
                        view.navigateToStartTrip()
                    }
                }
        compositeDisposable.add(disposable)
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    override fun setView(view: ScanValidationContract.View) {
        this.view = view
    }

    override fun subscribe() {
        validationService.getScannedShipmentsCount()
                .subscribeOn(scheduleProvider.io())
                .observeOn(scheduleProvider.ui())
                .subscribe {
                    scannedShipmentsCount = it.toInt()
                    view.showShipmentsCount(scannedShipmentsCount)
                }
    }
}