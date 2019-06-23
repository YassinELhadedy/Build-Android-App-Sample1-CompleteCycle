package com.transporter.streetglide.models

import io.reactivex.Observable

/**
 * GetAllRepository
 */
interface GetAllRepository<out T> {
    fun getAll(pagination: Pagination): Observable<out List<T>>
}