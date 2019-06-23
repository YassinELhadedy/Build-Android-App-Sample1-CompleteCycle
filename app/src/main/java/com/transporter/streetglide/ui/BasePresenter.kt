package com.transporter.streetglide.ui


interface BasePresenter<in T> {

    /**
     * Subscribe again after it has been disposed
     * it should be called to resume view if it needed
     */
    fun subscribe()

    /**
     * Dispose all subscriptions when the values are not interesting any more
     * it should be called when view is paused or destroyed
     */
    fun unsubscribe()

    fun setView(view: T)
}