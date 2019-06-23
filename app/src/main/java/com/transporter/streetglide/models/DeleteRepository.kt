package com.transporter.streetglide.models

import io.reactivex.Observable

/**
 * DeleteRepository
 */
interface DeleteRepository {
    fun delete(id: Int): Observable<Unit>
}