package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.dto.*
import io.reactivex.Observable
import retrofit2.http.*

private const val X_AUTH_TOKEN = "x-auth-token"

interface SuperGlideRestService {

    @POST("tokens/")
    fun login(@Body login: SgLogin): Observable<SgSignedToken>

    @GET("users/{userId}")
    fun getUser(@Header(X_AUTH_TOKEN) token: String,
                @Path("userId") userId: Int): Observable<SgUserWithAddressesAndPhonesAndAreasAndCapacities>

    @GET("drss/")
    fun getDrss(@Header(X_AUTH_TOKEN) token: String,
                @Query("filter", encoded = true) filter: String?,
                @Query("sort") sort: String?,
                @Query("offset") offset: Int?,
                @Query("pagesize") pageSize: Int?): Observable<SgPaginated<List<SgDeliveryRunSheetListing>>>

    @GET("drss/{sheetId}")
    fun getDrsBySheetId(@Header(X_AUTH_TOKEN) token: String,
                        @Path("sheetId") sheetId: Int): Observable<SgDeliveryRunSheetListing>

    @GET("areas/")
    fun getAreas(@Header(X_AUTH_TOKEN) token: String,
                 @Query("filter", encoded = true) filter: String?): Observable<SgPaginated<List<SgAreaWithRunner>>>
}