package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.dto.SgArea
import com.transporter.streetglide.infrastructure.dto.SgShipmentWithPickUp
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

/**
 * SheetSuperGlideRepository
 */
private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

class SheetSuperGlideRepository(private val superGlideRestService: SuperGlideRestService, private val configurationRepository: ConfigurationRepository) : ReadRepository<Sheet> {
    data class QueryHolder(var filter: String? = null, var sort: String? = null, var limit: Int? = null, var offset: Int? = null)

    override fun get(id: Int): Observable<out Sheet> {
        return configurationRepository.get(11).flatMap { configurationRepository ->
            superGlideRestService.getDrsBySheetId(configurationRepository.signedToken.toGlider(), id)
                    .flatMap { sgDrsWithShipments ->
                        val filter = sgDrsWithShipments.shipments.joinToString("%7c") { "area.id%3D${it.shipment.consigneeAddress.areaId}" }
                        val areaMap = superGlideRestService.getAreas(configurationRepository.signedToken.toGlider(), filter)
                                .map {
                                    it.result.map {
                                        it.area
                                    }.groupBy {
                                        it.id
                                    }.mapValues {
                                        it.value.first()
                                    }
                                }
                        areaMap.map { sgAreaMap ->
                            sgDrsWithShipments.copy(shipments = sgDrsWithShipments.shipments.map {
                                val area = sgAreaMap.getOrDefault(it.shipment.consigneeAddress.areaId,
                                        SgArea(0, "", "", "")) // Should never happen
                                val consigneeAddress = it.shipment.consigneeAddress.copy(area = area)
                                SgShipmentWithPickUp(it.shipment.copy(consigneeAddress = consigneeAddress))
                            }).toSheet()
                        }
                    }
                    .onErrorResumeNext { e: Throwable ->
                        if (e is HttpException && e.response().code() == 404) {
                            Observable.empty()
                        } else {
                            // FIXME: We need to Handle Superglide errors.
                            Observable.error(InfrastructureException(e))
                        }
                    }
        }
    }

    override fun getAll(pagination: Pagination): Observable<out List<Sheet>> =
            Paginator(
                    pagination,
                    object : Paginatee<Sheet, QueryHolder, String> {

                        override fun filter(expr: String?): QueryHolder {
                            val query = QueryHolder()
                            if (expr != null) {
                                query.filter = expr
                            }
                            return query
                        }

                        override fun andExpr(lhs: String, rhs: String): String = "$lhs%26$rhs"

                        override fun orExpr(lhs: String, rhs: String): String = "%28$lhs%7c$rhs%29"

                        override fun condition(condition: Condition<Any?>): String {
                            return when (condition.field) {
                                "barcode", "deliveryRunSheet.barcode" -> when (condition.operator) {
                                    Operator.Like -> when (condition.constant) {
                                        is String -> "deliveryRunSheet.barcode~%27%25${condition.constant}%25%27"
                                        else -> throw IllegalArgumentException(UNSUPPORTED_CONSTANT)
                                    }
                                    else -> throw IllegalArgumentException(UNSUPPORTED_OPERATION)
                                }
                                "deliveryRunSheet.dateTime" -> when (condition.constant) {
                                    is Date -> when (condition.operator) {
                                        Operator.LessThanOrEqual -> "deliveryRunSheet.datetime<=#${DATE_FORMAT.format(condition.constant)}"
                                        Operator.GreaterThanOrEqual -> "deliveryRunSheet.datetime>=#${DATE_FORMAT.format(condition.constant)}"
                                        else -> throw IllegalArgumentException(UNSUPPORTED_OPERATION)
                                    }
                                    else -> throw IllegalArgumentException(UNSUPPORTED_CONSTANT)
                                }
                                else -> {
                                    throw IllegalArgumentException(UNSUPPORTED_FIELD)
                                }
                            }
                        }

                        override fun sort(query: QueryHolder, sortBy: SortBy): QueryHolder =
                                when (sortBy.sortExpression) {
                                    "deliveryRunSheet.dateTime" -> when (sortBy.direction) {
                                        is Ascending -> {
                                            query.sort = "+deliveryRunSheet.datetime"; query
                                        }
                                        is Descending -> {
                                            query.sort = "-deliveryRunSheet.datetime"; query
                                        }
                                    }
                                    else -> throw IllegalArgumentException(UNSUPPORTED_FIELD)
                                }

                        override fun limit(query: QueryHolder, limit: Int): QueryHolder {
                            query.limit = limit
                            return query
                        }

                        override fun offset(query: QueryHolder, offset: Int): QueryHolder {
                            query.offset = offset
                            return query
                        }

                        override fun run(query: QueryHolder): Observable<List<Sheet>> {
                            return configurationRepository.get(11).flatMap { configurationRepository ->
                                superGlideRestService.getDrss(configurationRepository.signedToken.toGlider(),
                                        query.filter,
                                        query.sort,
                                        query.offset,
                                        query.limit
                                ).map {
                                    it.result.map {
                                        it.toSheet()
                                    }
                                }
                            }
                        }
                    }
            ).run()
}