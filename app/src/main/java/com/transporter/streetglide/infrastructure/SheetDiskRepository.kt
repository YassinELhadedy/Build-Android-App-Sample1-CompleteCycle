package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.SheetMapper.toSheet
import com.transporter.streetglide.infrastructure.ShipmentMapper.toDaoShipments
import com.transporter.streetglide.infrastructure.dao.DaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoSheetDao
import com.transporter.streetglide.infrastructure.dao.DaoShipment
import com.transporter.streetglide.infrastructure.dao.DaoShipmentDao
import com.transporter.streetglide.models.*
import io.reactivex.Observable
import org.greenrobot.greendao.query.QueryBuilder
import org.greenrobot.greendao.query.WhereCondition
import java.util.*

/**
 * Sheet Disk Repository
 * This class is open for Mockito to be able to mock it.
 */
open class SheetDiskRepository(private val sheetDao: DaoSheetDao,
                               private val shipmentDao: DaoShipmentDao, private val configurationRepository: ConfigurationRepository) : Repository<Sheet, Sheet>, SupportTransaction {

    override fun <T> doInTransaction(operation: () -> T): T =
            try {
                sheetDao.database.beginTransaction()

                val result = operation()

                sheetDao.database.setTransactionSuccessful()
                result
            } catch (e: Exception) {
                throw InfrastructureException(e)
            } finally {
                sheetDao.database.endTransaction()
            }

    override fun get(id: Int): Observable<Sheet> {
        return configurationRepository.get(13).flatMap {
            Observable.fromCallable {
                sheetDao.load(it.sheetId.toLong())
            }.onErrorResumeNext { throwable: Throwable ->
                if (throwable is NullPointerException) {
                    Observable.empty()
                } else {
                    Observable.error(InfrastructureException(throwable))
                }
            }.map { it.toSheet() }
        }
    }

    override fun getAll(pagination: Pagination): Observable<out List<Sheet>> =
            Paginator(
                    pagination,
                    object : Paginatee<Sheet, QueryBuilder<DaoSheet>, WhereCondition> {
                        val qb = sheetDao.queryBuilder()

                        override fun filter(expr: WhereCondition?): QueryBuilder<DaoSheet> {
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
                                    "barcode", "deliveryRunSheet.barcode" -> when (condition.operator) {
                                        Operator.Like -> when (condition.constant) {
                                            is String -> DaoSheetDao.Properties.Barcode.like(condition.constant)
                                            else -> throw IllegalArgumentException(UNSUPPORTED_CONSTANT)
                                        }
                                        else -> throw IllegalArgumentException(UNSUPPORTED_OPERATION)
                                    }
                                    "deliveryRunSheet.dateTime" -> when (condition.constant) {
                                        is Date -> when (condition.operator) {
                                            Operator.LessThanOrEqual -> DaoSheetDao.Properties.DateTime.le(condition.constant)
                                            Operator.GreaterThanOrEqual -> DaoSheetDao.Properties.DateTime.ge(condition.constant)
                                            else -> throw IllegalArgumentException(UNSUPPORTED_OPERATION)
                                        }
                                        else -> throw IllegalArgumentException(UNSUPPORTED_CONSTANT)
                                    }
                                    else -> {
                                        throw IllegalArgumentException(UNSUPPORTED_FIELD)
                                    }
                                }

                        override fun sort(query: QueryBuilder<DaoSheet>, sortBy: SortBy): QueryBuilder<DaoSheet> {
                            val field = when (sortBy.sortExpression) {
                                "deliveryRunSheet.dateTime" -> DaoSheetDao.Properties.DateTime
                                else -> throw IllegalArgumentException(UNSUPPORTED_FIELD)
                            }
                            return when (sortBy.direction) {
                                is Ascending -> query.orderAsc(field)
                                is Descending -> query.orderDesc(field)
                            }
                        }

                        override fun limit(query: QueryBuilder<DaoSheet>, limit: Int): QueryBuilder<DaoSheet> =
                                query.limit(limit)

                        override fun offset(query: QueryBuilder<DaoSheet>, offset: Int): QueryBuilder<DaoSheet> =
                                query.offset(offset)

                        override fun run(query: QueryBuilder<DaoSheet>): Observable<List<Sheet>> =
                                /* This query could be parameterized and we could use
                                 * forCurrentThread() but because we are going to use
                                 * dynamic queries it will not work out.
                                 */
                                Observable.fromCallable {
                                    query.build().list().map { it.toSheet() }
                                }
                    }
            ).run()

    override fun insert(entity: Sheet): Observable<out Sheet> = Observable.fromCallable {
        doInTransaction {
            sheetDao.insertInTx(entity.toDaoSheet())
            // Insert shipments.
            shipmentDao.insertInTx(entity.toDaoShipments(entity.id))
            entity
        }
    }

    override fun update(entity: Sheet): Observable<Unit> = Observable.fromCallable {
        doInTransaction {
            sheetDao.updateInTx(entity.toDaoSheet())
            deleteOldShipments()
            // Insert new shipments.
            shipmentDao.insertInTx(entity.toDaoShipments(entity.id))
        }
    }

    override fun insertOrUpdate(entity: Sheet): Observable<out Sheet> =
            Observable.fromCallable {
                doInTransaction {
                    deleteOldShipments()

                    sheetDao.insertOrReplaceInTx(entity.toDaoSheet())
                    // Insert shipments.
                    shipmentDao.insertInTx(entity.toDaoShipments(entity.id))
                    entity
                }
            }

    override fun delete(id: Int): Observable<Unit> {
        return configurationRepository.get(13).flatMap {
            Observable.fromCallable {
                doInTransaction {
                    val sheet = sheetDao.load(it.sheetId.toLong())
                    sheetDao.delete(sheet)
                    deleteOldShipments()
                }
            }
        }
    }

    private fun deleteOldShipments() {
        val query = shipmentDao.session.queryBuilder(DaoShipment::class.java)
                .where(DaoShipmentDao.Properties.SheetId.eq(configurationRepository))
                .buildDelete()
                .forCurrentThread()
        query.executeDeleteWithoutDetachingEntities()
        shipmentDao.detachAll()
    }
}