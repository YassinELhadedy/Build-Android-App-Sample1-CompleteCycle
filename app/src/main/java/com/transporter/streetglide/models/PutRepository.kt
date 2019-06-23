package com.transporter.streetglide.models

import io.reactivex.Observable

/**
 * PutRepository
 */
interface PutRepository<in T> {
    fun update(entity: T): Observable<Unit>
}