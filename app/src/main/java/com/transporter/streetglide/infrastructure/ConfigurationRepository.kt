package com.transporter.streetglide.infrastructure

import android.content.SharedPreferences
import com.google.gson.Gson
import com.transporter.streetglide.models.Configuration
import com.transporter.streetglide.models.GetRepository
import com.transporter.streetglide.models.WriteRepository
import io.reactivex.Observable


class ConfigurationRepository(private val sharedPreferences: SharedPreferences) : WriteRepository<Configuration, Configuration>, GetRepository<Configuration> {

    companion object {
        const val KEY_CONFIG = "streetglide.configuration"
    }

    override fun get(id: Int): Observable<out Configuration> = Observable.fromCallable {
        val json = sharedPreferences.getString(KEY_CONFIG, null)
        Gson().fromJson(json, Configuration::class.java)
    }.onErrorResumeNext { throwable: Throwable ->
                when (throwable) {
                    is NullPointerException -> Observable.empty<Configuration>()
                    else -> Observable.error(InfrastructureException(RuntimeException("Data Error!")))
                }
            }

    override fun insert(entity: Configuration): Observable<out Configuration> = Observable.fromCallable {
        val json = sharedPreferences.getString(KEY_CONFIG, null)
        val config = Gson().fromJson(json, Configuration::class.java)
        if (entity != config) {
            val jsonString = Gson().toJson(entity)
            sharedPreferences.edit().putString(KEY_CONFIG, jsonString).apply()
            entity
        } else {
            throw InfrastructureException("Entity already exist")
        }
    }

    override fun update(entity: Configuration): Observable<Unit> = Observable.fromCallable {
        val jsonString = Gson().toJson(entity)
        sharedPreferences.edit().putString(KEY_CONFIG, jsonString).apply()
    }

    override fun delete(id: Int): Observable<Unit> = Observable.fromCallable {
        val json = sharedPreferences.getString(KEY_CONFIG, null)
        val config = Gson().fromJson(json, Configuration::class.java)
        if (config != null) {
            sharedPreferences.edit().remove(KEY_CONFIG).apply()
        } else {
            throw InfrastructureException("Entity may not be null")
        }

    }
}