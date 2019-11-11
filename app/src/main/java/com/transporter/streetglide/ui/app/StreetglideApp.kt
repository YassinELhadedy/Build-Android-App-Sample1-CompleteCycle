package com.transporter.streetglide.ui.app

import android.app.Application
import android.content.Context
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.SuperGlideRestService
import com.transporter.streetglide.infrastructure.SuperGlideRestServiceFactory
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dao.DaoSession
import com.transporter.streetglide.ui.di.applicationModule
import com.transporter.streetglide.ui.util.ProductionDbOpenHelper
import org.koin.android.ext.android.startKoin


private const val BASE_URL = "https://172.17.8.102/api/v1/"

class StreetglideApp : Application() {
    private lateinit var daoSession: DaoSession

    override fun onCreate() {
        super.onCreate()
        setupGreenDao()
        getConfigRepoInstance()
        getServiceInstance()
        startKoin(this, listOf(applicationModule))
    }

    private fun setupGreenDao() {
        val openHelper = ProductionDbOpenHelper(this, "streetglide-db")
        daoSession = DaoMaster(openHelper.writableDb).newSession()
    }

    fun getDaoSession(): DaoSession {
        return daoSession
    }

    fun getConfigRepoInstance(): ConfigurationRepository {
        return ConfigurationRepository(getSharedPreferences("streetglide.preference", Context.MODE_PRIVATE))
    }

    fun getServiceInstance(): SuperGlideRestService {
        return SuperGlideRestServiceFactory(BASE_URL).service
    }
}