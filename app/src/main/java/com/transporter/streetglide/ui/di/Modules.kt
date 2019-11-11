package com.transporter.streetglide.ui.di

import android.content.Context
import com.transporter.streetglide.infrastructure.*
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dao.DaoSession
import com.transporter.streetglide.infrastructure.dao.DaoSheetDao
import com.transporter.streetglide.infrastructure.dao.DaoShipmentDao
import com.transporter.streetglide.models.services.GetRunnerIdService
import com.transporter.streetglide.models.services.LoginService
import com.transporter.streetglide.models.services.RequestSheetService
import com.transporter.streetglide.ui.runnerlogin.LoginContract
import com.transporter.streetglide.ui.runnerlogin.LoginPresenter
import com.transporter.streetglide.ui.util.ProductionDbOpenHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

private const val BASE_URL = "https://172.17.8.102/api/v1/"

val applicationModule = module {
     lateinit var daoSession: DaoSession

     fun setupGreenDao() {
        val openHelper = ProductionDbOpenHelper(androidContext(), "streetglide-db")
        daoSession = DaoMaster(openHelper.writableDb).newSession()
    }

    fun provideSheetDao(): DaoSheetDao {
        setupGreenDao()
        return daoSession.daoSheetDao
    }
    fun provideShipmentDao(): DaoShipmentDao {
        setupGreenDao()
        return daoSession.daoShipmentDao
    }
    single { provideSheetDao() }
    single { provideShipmentDao() }

    single { SuperGlideRestServiceFactory(BASE_URL).service }
    single{ androidContext().getSharedPreferences("streetglide.preference", Context.MODE_PRIVATE) }

//    single {
//        val openHelper = ProductionDbOpenHelper(androidContext(), "streetglide-db")
//
//        DaoMaster(openHelper.writableDb).newSession()
//
//    }

    // Simple Presenter Factory
    single { TokenSuperGlideRepository(get()) }
    single { ConfigurationRepository(get()) }
    single{ SheetSuperGlideRepository(get(),get()) }
    single { SheetDiskRepository(get(),get(),get()) }
    single { UserSuperGlideRepository(get(),get()) }

    single { LoginService(get(),get()) }
    single { RequestSheetService(get(),get(),get()) }
    single { GetRunnerIdService(get(),get()) }

    factory<LoginContract.Presenter> { LoginPresenter(get(),get(),get()) }

//    factory { LoginPresenter(loginService = get(),requestSheetService =  get(),getRunnerIdService =  get()) }
}