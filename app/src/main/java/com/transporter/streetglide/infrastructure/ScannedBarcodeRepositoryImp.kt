package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toScannedBarcode
import com.transporter.streetglide.infrastructure.dao.DaoScannedBarcode
import com.transporter.streetglide.infrastructure.dao.DaoScannedBarcodeDao
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import org.greenrobot.greendao.query.QueryBuilder
import org.greenrobot.greendao.query.WhereCondition


open class ScannedBarcodeRepositoryImp(private val daoScannedBarcode: DaoScannedBarcodeDao) : ScannedBarcodeRepository<ScannedBarcode, ScannedBarcode> {

    override fun getCount(): Observable<Long> = Observable.fromCallable {
        daoScannedBarcode.count()
    }

    override fun getAll(pagination: Pagination): Observable<out List<ScannedBarcode>> =
            Paginator(
                    pagination,
                    object : Paginatee<ScannedBarcode, QueryBuilder<DaoScannedBarcode>, WhereCondition> {
                        val qb = daoScannedBarcode.queryBuilder()

                        override fun filter(expr: WhereCondition?): QueryBuilder<DaoScannedBarcode> {
                            return if (expr == null) {
                                qb
                            } else {
                                qb.where(expr)
                            }
                        }

                        override fun andExpr(lhs: WhereCondition, rhs: WhereCondition): WhereCondition =
                                qb.and(lhs, rhs)

                        override fun orExpr(lhs: WhereCondition, rhs: WhereCondition): WhereCondition =
                                qb.or(lhs, rhs)

                        override fun condition(condition: Condition<Any?>): WhereCondition =
                                when (condition.field) {
                                    "barcode" -> when (condition.operator) {
                                        Operator.Equal -> when (condition.constant) {
                                            is String -> DaoScannedBarcodeDao.Properties.Barcode.eq(condition.constant)
                                            else -> throw IllegalArgumentException(UNSUPPORTED_CONSTANT)
                                        }
                                        else -> throw IllegalArgumentException(UNSUPPORTED_OPERATION)
                                    }
                                    "found" -> when (condition.constant) {
                                        is Boolean -> when (condition.operator) {
                                            Operator.Equal -> DaoScannedBarcodeDao.Properties.Found.eq(condition.constant)
                                            else -> throw IllegalArgumentException(UNSUPPORTED_OPERATION)
                                        }
                                        else -> throw IllegalArgumentException(UNSUPPORTED_CONSTANT)
                                    }
                                    else -> {
                                        throw IllegalArgumentException(UNSUPPORTED_FIELD)
                                    }
                                }

                        override fun sort(query: QueryBuilder<DaoScannedBarcode>, sortBy: SortBy): QueryBuilder<DaoScannedBarcode> =
                                sortBy.let {
                                    throw IllegalArgumentException(UNSUPPORTED_FIELD)
                                }

                        override fun limit(query: QueryBuilder<DaoScannedBarcode>, limit: Int): QueryBuilder<DaoScannedBarcode> =
                                query.limit(limit)

                        override fun offset(query: QueryBuilder<DaoScannedBarcode>, offset: Int): QueryBuilder<DaoScannedBarcode> =
                                query.offset(offset)

                        override fun run(query: QueryBuilder<DaoScannedBarcode>): Observable<List<ScannedBarcode>> =
                                Observable.fromCallable {
                                    query.build().list().map { it.toScannedBarcode() }
                                }
                    }
            ).run()

    override fun update(entity: ScannedBarcode): Observable<Unit> = Observable.fromCallable {
        try {
            daoScannedBarcode.update(entity.toDaoScannedBarcode())
        } catch (e: Exception) {
            throw InfrastructureException(e)
        }
    }

    override fun delete(id: Int): Observable<Unit> = Observable.fromCallable {
        try {
            val scannedBarcode = daoScannedBarcode.load(id.toLong())
            daoScannedBarcode.delete(scannedBarcode)
        } catch (e: Exception) {
            throw InfrastructureException(e)
        }
    }

    override fun insert(entity: ScannedBarcode): Observable<out ScannedBarcode> = Observable.fromCallable {
        try {
            daoScannedBarcode.insert(entity.toDaoScannedBarcode())
        } catch (e: Exception) {
            throw InfrastructureException(e)
        }
        entity
    }

    override fun insertOrUpdate(entity: ScannedBarcode): Observable<out ScannedBarcode> = Observable.fromCallable {
        try {
            daoScannedBarcode.insertOrReplace(entity.toDaoScannedBarcode())
        } catch (e: Exception) {
            throw InfrastructureException(e)
        }
        entity
    }
}