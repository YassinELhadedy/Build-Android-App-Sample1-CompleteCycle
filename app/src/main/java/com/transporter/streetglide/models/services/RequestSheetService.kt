package com.transporter.streetglide.models.services

import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.SheetDiskRepository
import com.transporter.streetglide.infrastructure.SheetSuperGlideRepository
import com.transporter.streetglide.models.Configuration
import com.transporter.streetglide.models.ModelException
import com.transporter.streetglide.models.Pagination
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.exception.SheetNotFoundException
import com.transporter.streetglide.models.exception.TokenExpiredException
import io.reactivex.Observable
import retrofit2.HttpException

/**
 *Created by yassin on 4/10/18.
 */
class RequestSheetService(private val sheetSuperGlideRepository: SheetSuperGlideRepository, private val sheetDiskRepository: SheetDiskRepository, private val configurationRepository: ConfigurationRepository) {

    fun requestSheet(pagination: Pagination): Observable<out Boolean> {
        return sheetSuperGlideRepository.getAll(pagination).flatMap {
            if (it[0].isReturned) {
                Observable.fromCallable { false }
            } else {
                configurationRepository.update(Configuration(SignedToken(""), it[0].id, "23"))
                sheetSuperGlideRepository.get(it[0].id).flatMap {
                    sheetDiskRepository.insertOrUpdate(it)
                            .map { _ -> true }
                            .onErrorReturn { _ -> false }
                }
            }
        }.map { it }.onErrorResumeNext { e: Throwable ->
            // when sheet not found in superglide what's exception coming?
            if (e is IndexOutOfBoundsException) {
                Observable.error(SheetNotFoundException(e))
            } else if (e.cause is HttpException && (e.cause as HttpException).response().code() == 401) {
                Observable.error(TokenExpiredException(e))
            } else {
                Observable.error(ModelException(e))
            }
        }
    }
}