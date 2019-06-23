package com.transporter.streetglide.models

import io.reactivex.Observable

/**
 * GetRepository
 */
interface GetRepository<out T> {
    fun get(id: Int): Observable<out T>
}