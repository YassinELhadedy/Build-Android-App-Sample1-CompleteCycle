package com.transporter.streetglide.models


interface Repository<in T, out U> : ReadRepository<U>, WriteRepository<T, U> {

    // TODO: Uncomment if needed
    /*fun isCached(): Boolean*/

    // TODO: Uncomment if needed
    /*fun evict(): Observable<Unit>*/

}