package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dto.SgDeliveryRunSheetListing
import com.transporter.streetglide.infrastructure.dto.SgDrs.Companion.toSgDrs
import com.transporter.streetglide.infrastructure.dto.SgPaginated
import com.transporter.streetglide.models.*
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.*
import java.util.Collections.emptyList
import java.util.concurrent.TimeUnit

private const val DATA_ERROR = "Unsupported field"

@Config
@RunWith(ParameterizedRobolectricTestRunner::class)
class GetAllRepositoryUnitTest(private val setupTestParameter: GetAllRepositoryUnitTest.SetupTestParameter<*>) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : GetAllRepositoryUnitTest.SetupTestParameter<Sheet> {
                    override fun setup(): GetAllRepositoryUnitTest.TestParameter<Sheet> {
                        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                        val configRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configRepository)

                        val barcode1 = "1594413926"
                        val barcode2 = "1688359452"
                        val barcode3 = "6717559108"
                        val barcode4 = "6655879410"
                        val barcode5 = "5678910879"

                        val date1 = 1466624199000L // Wed Jun 22 19:36:39 UTC 2016
                        val date2 = 1486624199000L // Thu Feb 09 07:09:59 UTC 2017
                        val date3 = 1488724199000L // Sun Mar 05 14:29:59 UTC 2017
                        val date4 = 1046524199000L // Sat Mar 01 13:09:59 UTC 2003
                        val date5 = 2546524199000L // Sun Sep 11 15:49:59 UTC 2050

                        val sheets = listOf(
                                Sheet(26304, barcode1, 4312, 1, Date(date1), false, consignees = emptyList()),
                                Sheet(26305, barcode2, 7952, 3, Date(date2), false, consignees = emptyList()),
                                Sheet(26306, barcode3, 9349, 6, Date(date3), false, consignees = emptyList()))

                        val daoSheets = sheets.map { it.toDaoSheet() }
                        daoSession.daoSheetDao.insertInTx(daoSheets)

                        /*
                         * Valid Fields and their types to be compatible with Superglide:
                         * | Field                     | Type      | Usable |
                         * |---------------------------|-----------|--------|
                         * | barcode                   | String    | Yes    |
                         * | shipment.barcode          | String    | No     |
                         * | deliveryRunSheet.barcode  | String    | Yes    |
                         * | runner.code               | String    | Yes    |
                         * | deliveryRunSheet.datetime | Timestamp | Yes    |
                         * | runner.name               | String    | Yes    |
                         */

                        val expr1 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(date2))
                        val expr2 = Condition("deliveryRunSheet.dateTime", Operator.LessThanOrEqual, Date(date2))
                        val expr3 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(date3))
                        val expr4 = Condition("deliveryRunSheet.dateTime", Operator.LessThanOrEqual, Date(date1))
                        val expr5 = Condition("deliveryRunSheet.dateTime", Operator.LessThanOrEqual, Date(date4)) // Empty
                        val expr6 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(date5)) // Empty
                        val expr7 = Condition("deliveryRunSheet.dateTime", Operator.Like, Date(date1)) // Invalid
                        val expr8 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, date1) // Invalid
                        val expr9 = Condition("barcode", Operator.Like, "%$barcode1%")
                        val expr10 = Condition("deliveryRunSheet.barcode", Operator.Like, "%$barcode2%")
                        val expr11 = Condition("barcode", Operator.Like, "%$barcode3%")
                        val expr12 = Condition("deliveryRunSheet.barcode", Operator.Like, "%$barcode4%") // Empty
                        val expr13 = Condition("barcode", Operator.Like, "%$barcode5%") // Empty
                        val expr14 = Condition("barcode", Operator.Like, 12345) // Invalid
                        val expr15 = Condition("barcode", Operator.Equal, barcode5) // Invalid

                        val expr16 = AndExpr(expr1, expr2)
                        val expr17 = OrExpr(expr2, expr3)
                        val expr18 = OrExpr(expr4, expr5)
                        val expr19 = AndExpr(expr4, expr5) // Empty
                        val expr20 = OrExpr(expr6, expr7) // Invalid

                        val expr21 = OrExpr(expr9, expr10)
                        val expr22 = AndExpr(expr9, expr10) // Empty
                        val expr23 = OrExpr(expr9, expr15) // Invalid

                        val expr24 = OrExpr(expr16, expr21)
                        val expr25 = AndExpr(expr16, expr21)
                        val expr26 = AndExpr(expr16, expr22) // Empty
                        val expr27 = OrExpr(expr19, expr23) // Invalid

                        val sort1 = SortBy("deliveryRunSheet.dateTime", Ascending())
                        val sort2 = SortBy("deliveryRunSheet.dateTime", Descending())
                        val sort3 = SortBy("bad", Ascending())

                        val paginationMap = hashMapOf(
                                Pagination(expr1, sort1, 0, 10) to sheets.subList(1, 3),
                                Pagination(expr1, sort2, 0, 10) to sheets.subList(1, 3).asReversed(),
                                Pagination(expr1, sort1, 1, 10) to sheets.subList(2, 3),
                                Pagination(expr1, sort1, 0, 1) to sheets.subList(1, 2),
                                Pagination(expr1, sort1, 0) to sheets.subList(1, 3),
                                Pagination(expr1, sort1, 3) to emptyList(),
                                Pagination(expr1, sort1) to sheets.subList(1, 3),
                                Pagination(expr1) to sheets.subList(1, 3),
                                Pagination() to sheets,
                                Pagination(expr2, sort1, 0, 10) to sheets.subList(0, 2),
                                Pagination(expr3) to sheets.subList(2, 3),
                                Pagination(expr4) to sheets.subList(0, 1),
                                Pagination(expr5) to emptyList(),
                                Pagination(expr6) to emptyList(),
                                Pagination(expr9) to sheets.subList(0, 1),
                                Pagination(expr10) to sheets.subList(1, 2),
                                Pagination(expr11) to sheets.subList(2, 3),
                                Pagination(expr12) to emptyList(),
                                Pagination(expr13) to emptyList(),
                                Pagination(expr16) to sheets.subList(1, 2),
                                Pagination(expr17) to sheets,
                                Pagination(expr18) to sheets.subList(0, 1),
                                Pagination(expr19) to emptyList(),
                                Pagination(expr21) to sheets.subList(0, 2),
                                Pagination(expr22) to emptyList(),
                                Pagination(expr24) to sheets.subList(0, 2),
                                Pagination(expr25) to sheets.subList(1, 2),
                                Pagination(expr26) to emptyList()
                        )

                        val faultyPaginationMap = hashMapOf<Pagination, Throwable>(
                                Pagination(expr1) to RuntimeException(DATA_ERROR),
                                Pagination(null, null, 0, -1) to IllegalArgumentException(PAGE_SIZE_NEGATIVE),
                                Pagination(null, null, -1) to IllegalArgumentException(OFFSET_NEGATIVE),
                                Pagination(null, sort3) to IllegalArgumentException(UNSUPPORTED_FIELD),
                                Pagination(Condition("bad", Operator.Equal, 0)) to IllegalArgumentException(UNSUPPORTED_FIELD),
                                Pagination(expr7) to IllegalArgumentException(UNSUPPORTED_OPERATION),
                                Pagination(expr8) to IllegalArgumentException(UNSUPPORTED_CONSTANT),
                                Pagination(expr14) to IllegalArgumentException(UNSUPPORTED_CONSTANT),
                                Pagination(expr15) to IllegalArgumentException(UNSUPPORTED_OPERATION),
                                Pagination(expr20) to IllegalArgumentException(UNSUPPORTED_OPERATION),
                                Pagination(expr23) to IllegalArgumentException(UNSUPPORTED_OPERATION),
                                Pagination(expr27) to IllegalArgumentException(UNSUPPORTED_OPERATION)

                        )

                        return object : GetAllRepositoryUnitTest.TestParameter<Sheet> {
                            override fun getNormalPaginations(): Set<Pagination> =
                                    paginationMap.keys

                            override fun getAllWithNormalPagination(pagination: Pagination): Triple<Observable<out List<Sheet>>, Pagination, List<Sheet>> {
                                return Triple(sheetDiskRepository.getAll(pagination),
                                        pagination,
                                        paginationMap[pagination]!!)
                            }

                            override fun getFaultyPaginations(): Set<Pagination> =
                                    faultyPaginationMap.keys

                            override fun getAllWithFaultyPagination(pagination: Pagination): Triple<Observable<out List<Sheet>>, Pagination, Throwable> {
                                return Triple(sheetDiskRepository.getAll(pagination),
                                        pagination,
                                        faultyPaginationMap[pagination]!!)
                            }
                        }
                    }

                    override fun toString(): String =
                            SheetDiskRepository::class.java.simpleName
                }),
                arrayOf(object : GetAllRepositoryUnitTest.SetupTestParameter<Sheet> {
                    override fun setup(): GetAllRepositoryUnitTest.TestParameter<Sheet> {
                        // Remove the milli-second part as it is not supported by Superglide
                        val datetime = Date(Date().time / 1000 * 1000)
                        val sheets = listOf(
                                Sheet(1, "12345", 2, 3, datetime, true, consignees = emptyList()),
                                Sheet(2, "12346", 4, 6, datetime, false, consignees = emptyList()),
                                Sheet(3, "12347", 6, 9, datetime, false, consignees = emptyList()))

                        val expr = Condition("runnerId", Operator.Equal, 2)
                        val expr1 = Condition("barcode", Operator.Like, "12345")

                        val sort = SortBy("deliveryRunSheet.dateTime", Descending())
                        val paginationMap = hashMapOf(Pagination(expr1, sort, 0, 2) to sheets.subList(0, 1),
                                Pagination(expr1, sort, 0, 0) to emptyList())

                        val faultyPaginationMap = hashMapOf<Pagination, Throwable>(Pagination(expr, sort, 0, 2) to RuntimeException(DATA_ERROR),
                                Pagination(expr, sort, 0, 0) to RuntimeException(DATA_ERROR))

                        return object : GetAllRepositoryUnitTest.TestParameter<Sheet> {
                            override fun getNormalPaginations(): Set<Pagination> =
                                    paginationMap.keys

                            override fun getAllWithNormalPagination(pagination: Pagination): Triple<Observable<out List<Sheet>>, Pagination, List<Sheet>> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)
                                Mockito.`when`(mockSuperGlideRestService.getDrss(ArgumentMatchers.anyString(),
                                        ArgumentMatchers.any(),
                                        ArgumentMatchers.anyString(),
                                        ArgumentMatchers.anyInt(),
                                        ArgumentMatchers.anyInt()))
                                        .thenReturn(Observable.just(
                                                SgPaginated(100,
                                                        paginationMap[pagination]!!.map {
                                                            SgDeliveryRunSheetListing(it.toSgDrs(), 9120, emptyList())
                                                        })))
                                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                                val configRepository = ConfigurationRepository(sharedPreference)

                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return Triple(mockSheetSuperGlideRepository.getAll(pagination),
                                        pagination,
                                        paginationMap[pagination]!!)
                            }

                            override fun getFaultyPaginations(): Set<Pagination> =
                                    faultyPaginationMap.keys

                            override fun getAllWithFaultyPagination(pagination: Pagination): Triple<Observable<out List<Sheet>>, Pagination, Throwable> {
                                val mockSuperGlideRestService = Mockito.mock(SuperGlideRestService::class.java)

                                /*
                                 * We are checking that the exception will pass transparently
                                 * to us. This may not be a good choice BTW.
                                 */
                                Mockito.`when`(mockSuperGlideRestService.getDrss(ArgumentMatchers.anyString(),
                                        ArgumentMatchers.any(),
                                        ArgumentMatchers.anyString(),
                                        ArgumentMatchers.anyInt(),
                                        ArgumentMatchers.anyInt()))
                                        .thenReturn(Observable.error(RuntimeException(DATA_ERROR)))

                                val sharedPreference: SharedPreferences = RuntimeEnvironment.application.getSharedPreferences(null, Context.MODE_PRIVATE)
                                val configRepository = ConfigurationRepository(sharedPreference)
                                val mockSheetSuperGlideRepository =
                                        SheetSuperGlideRepository(mockSuperGlideRestService, configRepository)

                                return Triple(mockSheetSuperGlideRepository.getAll(Pagination(expr, sort, 0, 2)),
                                        pagination,
                                        faultyPaginationMap[pagination]!!)
                            }
                        }
                    }

                    override fun toString(): String =
                            SheetSuperGlideRepository::class.java.simpleName
                }),
                arrayOf(object : GetAllRepositoryUnitTest.SetupTestParameter<ScannedBarcode> {
                    override fun setup(): GetAllRepositoryUnitTest.TestParameter<ScannedBarcode> {
                        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val scannedBarcodeRepository = ScannedBarcodeRepositoryImp(daoSession.daoScannedBarcodeDao)

                        val barcode1 = "123456789"
                        val barcode2 = "254789154"
                        val barcode3 = "178462480"
                        val barcode4 = "487264801"
                        val barcode5 = "995400215"
                        val barcode6 = "274158790"

                        val scannedBarcodes = listOf(ScannedBarcode(12, barcode1, false),
                                ScannedBarcode(13, barcode2, true),
                                ScannedBarcode(14, barcode6, true))
                        val daoScannedBarcodes = scannedBarcodes.map { it.toDaoScannedBarcode() }

                        for (dao in daoScannedBarcodes) {
                            daoSession.daoScannedBarcodeDao.insert(dao)
                        }

                        val expr1 = Condition("barcode", Operator.Equal, barcode1)
                        val expr2 = Condition("barcode", Operator.Equal, barcode2)
                        val expr3 = Condition("barcode", Operator.Equal, barcode6)
                        val expr4 = Condition("bar", Operator.Equal, barcode3) //invalid
                        val expr5 = Condition("barcode", Operator.Like, barcode1) //invalid
                        val expr6 = Condition("barcode", Operator.Equal, barcode4) //empty
                        val expr7 = Condition("barcode", Operator.Equal, barcode5) //empty
                        val expr8 = Condition("barcode", Operator.Equal, barcode1.toInt()) //invalid
                        val expr9 = Condition("found", Operator.Equal, true)
                        val expr10 = Condition("notfound", Operator.Equal, false) //invalid
                        val expr11 = Condition("found", Operator.Like, false) //invalid
                        val expr12 = Condition("found", Operator.Equal, "true") //invalid

                        val expr13 = OrExpr(expr1, OrExpr(expr3, expr2))
                        val expr14 = OrExpr(expr6, expr7) //Empty
                        val expr15 = AndExpr(expr1, expr10) //invalid
                        val expr16 = OrExpr(expr6, expr1)
                        val expr17 = OrExpr(expr14, expr8) //invalid
                        val expr18 = AndExpr(expr1, expr9) //empty

                        val paginationMap = hashMapOf(
                                Pagination() to scannedBarcodes,
                                Pagination(expr2) to scannedBarcodes.subList(1, 2),
                                Pagination(expr3) to scannedBarcodes.subList(2, 3),
                                Pagination(expr6) to emptyList(),
                                Pagination(expr9) to scannedBarcodes.subList(1, 3),
                                Pagination(expr13, null, 0, 2) to scannedBarcodes.subList(0, 2),
                                Pagination(expr13) to scannedBarcodes,
                                Pagination(expr14) to emptyList(),
                                Pagination(expr16) to scannedBarcodes.subList(0, 1),
                                Pagination(expr18) to emptyList()
                        )

                        val faultyPaginationMap = hashMapOf<Pagination, Throwable>(
                                Pagination(expr4) to IllegalArgumentException(UNSUPPORTED_FIELD),
                                Pagination(expr5) to IllegalArgumentException(UNSUPPORTED_OPERATION),
                                Pagination(expr8) to IllegalArgumentException(UNSUPPORTED_CONSTANT),
                                Pagination(expr9, null, 0, -1) to IllegalArgumentException(PAGE_SIZE_NEGATIVE),
                                Pagination(expr10) to IllegalArgumentException(UNSUPPORTED_FIELD),
                                Pagination(expr11) to IllegalArgumentException(UNSUPPORTED_OPERATION),
                                Pagination(expr12) to IllegalArgumentException(UNSUPPORTED_CONSTANT),
                                Pagination(expr13, null, -2, 4) to IllegalArgumentException(OFFSET_NEGATIVE),
                                Pagination(expr13, SortBy("barcode", Ascending())) to IllegalArgumentException(UNSUPPORTED_FIELD),
                                Pagination(expr15) to IllegalArgumentException(UNSUPPORTED_FIELD),
                                Pagination(expr17) to IllegalArgumentException(UNSUPPORTED_CONSTANT)
                        )

                        return object : GetAllRepositoryUnitTest.TestParameter<ScannedBarcode> {
                            override fun getNormalPaginations(): Set<Pagination> =
                                    paginationMap.keys

                            override fun getAllWithNormalPagination(pagination: Pagination): Triple<Observable<out List<ScannedBarcode>>, Pagination, List<ScannedBarcode>> {
                                return Triple(scannedBarcodeRepository.getAll(pagination),
                                        pagination,
                                        paginationMap[pagination]!!)
                            }

                            override fun getFaultyPaginations(): Set<Pagination> =
                                    faultyPaginationMap.keys

                            override fun getAllWithFaultyPagination(pagination: Pagination): Triple<Observable<out List<ScannedBarcode>>, Pagination, Throwable> {
                                return Triple(scannedBarcodeRepository.getAll(pagination),
                                        pagination,
                                        faultyPaginationMap[pagination]!!)
                            }
                        }
                    }

                    override fun toString(): String = ScannedBarcodeRepositoryImp::class.java.simpleName
                }))
    }

    @Test
    fun testGetAllWithNormalPaginationFromRepository() {
        val testParameter = setupTestParameter.setup()

        val testObserver = TestObserver<Triple<List<Any?>, Pagination, List<Any?>>>()
        Observable.fromIterable(testParameter.getNormalPaginations()
                .map {
                    val triple = testParameter.getAllWithNormalPagination(it)
                    triple.first.map { Triple(it, triple.second, triple.third) }
                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertSubscribed()
                .assertNoErrors()
                .assertComplete()
        testObserver.values().forEach {
            Assert.assertEquals(it.second.toString(), it.third, it.first)
        }
    }

    @Test
    @Config(shadows = [(GetAllRepositoryUnitTest.ShadowSQLiteDatabase::class)])
    fun testGetAllWithFaultyPaginationFromRepository() {
        val testParameter = setupTestParameter.setup()

        val testObserver = TestObserver<Triple<Notification<out List<Any?>>, Pagination, Throwable>>()
        Observable.fromIterable(testParameter.getFaultyPaginations()
                .map {
                    val triple = testParameter.getAllWithFaultyPagination(it)
                    triple.first.materialize().map { Triple(it, triple.second, triple.third) }

                })
                .flatMap { it }
                .subscribeOn(Schedulers.io())
                .subscribe(testObserver)

        testObserver.awaitTerminalEvent(1, TimeUnit.MINUTES)
        testObserver.assertSubscribed()
                .assertNoErrors()
                .assertComplete()
        testObserver.values().forEach {
            Assert.assertTrue(it.second.toString(), it.first.isOnError)
            Assert.assertTrue(it.second.toString(), it.first.error is InfrastructureException)
            Assert.assertEquals(it.second.toString(),
                    it.third.message, it.first.error?.cause?.message)
        }
    }


    interface TestParameter<out T> {
        fun getNormalPaginations(): Set<Pagination>
        fun getAllWithNormalPagination(pagination: Pagination): Triple<Observable<out List<T>>, Pagination, List<T>>
        fun getFaultyPaginations(): Set<Pagination>
        fun getAllWithFaultyPagination(pagination: Pagination): Triple<Observable<out List<T>>, Pagination, Throwable>
    }

    interface SetupTestParameter<out T> {
        fun setup(): TestParameter<T>
    }

    @Implements(SQLiteDatabase::class)
    class ShadowSQLiteDatabase {

        @Suppress("unused")
        @Implementation
        fun rawQuery(@Suppress("UNUSED_PARAMETER") sql: String,
                     @Suppress("UNUSED_PARAMETER") selectionArgs: Array<String>): Cursor {
            throw RuntimeException(DATA_ERROR)
        }

    }
}