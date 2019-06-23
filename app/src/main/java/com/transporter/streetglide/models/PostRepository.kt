package com.transporter.streetglide.models

import io.reactivex.Observable

/**
 * PostRepository
 */
interface PostRepository<in T, out U> {
    fun insert(entity: T): Observable<out U>
}