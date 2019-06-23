package com.transporter.streetglide.models

import io.reactivex.Observable


interface Paginatee<out T, Query, Cond> {
    fun filter(expr: Cond?): Query
    fun andExpr(lhs: Cond, rhs: Cond): Cond
    fun orExpr(lhs: Cond, rhs: Cond): Cond
    fun condition(condition: Condition<Any?>): Cond
    fun sort(query: Query, sortBy: SortBy): Query
    fun limit(query: Query, limit: Int): Query
    fun offset(query: Query, offset: Int): Query
    fun run(query: Query): Observable<out List<T>>
}