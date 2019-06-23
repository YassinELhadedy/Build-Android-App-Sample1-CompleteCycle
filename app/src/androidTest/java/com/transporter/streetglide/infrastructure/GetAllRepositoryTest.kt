package com.transporter.streetglide.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import com.google.gson.Gson
import com.transporter.streetglide.infrastructure.ScannedBarcodeMapper.toDaoScannedBarcode
import com.transporter.streetglide.infrastructure.SheetMapper.toDaoSheet
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dto.SgLogin
import com.transporter.streetglide.models.*
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*
import java.util.concurrent.TimeUnit

private const val DATA_ERROR = "Data Error!"

/**
 * Test All GetAllRepository
 */
@RunWith(Parameterized::class)
class GetAllRepositoryTest(private val setupTestParameter: SetupTestParameter<*>) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Array<*>> = listOf(
                arrayOf(object : GetAllRepositoryTest.SetupTestParameter<Sheet> {
                    override fun setup(): GetAllRepositoryTest.TestParameter<Sheet> {
                        val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
                        val daoSession = DaoMaster(openHelper.writableDb).newSession()
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val configurationRepository = ConfigurationRepository(sharedPreference)
                        val sheetDiskRepository = SheetDiskRepository(daoSession.daoSheetDao, daoSession.daoShipmentDao, configurationRepository)

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
                        val expr28 = AndExpr(expr4, expr9)


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
                                Pagination(expr26) to emptyList(),
                                Pagination(expr28) to listOf(sheets[0])
                        )

                        val faultyPaginationMap = hashMapOf<Pagination, Throwable>(
//                                Pagination(expr1) to RuntimeException(DATA_ERROR),
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

                        return object : GetAllRepositoryTest.TestParameter<Sheet> {
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
                arrayOf(object : GetAllRepositoryTest.SetupTestParameter<Sheet> {
                    override fun setup(): GetAllRepositoryTest.TestParameter<Sheet> {

                        val superGlideRestService: SuperGlideRestService = SuperGlideRestServiceFactory(MOCK_BASE_URL).service
                        val signedToken = superGlideRestService.login(SgLogin(USER_NAME, PASS)).blockingFirst()
                        val sharedPreference: SharedPreferences = InstrumentationRegistry.getContext().getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
                        val config = Configuration(signedToken.toSignedToken(), 2, "122")
                        val jsonString = Gson().toJson(config)
                        sharedPreference.edit().putString(ConfigurationRepository.KEY_CONFIG, jsonString).apply()
                        val configurationRepository = ConfigurationRepository(sharedPreference)
                        val sheetSuperGlideRepository = SheetSuperGlideRepository(superGlideRestService, configurationRepository)

                        val barcode1 = "1594413926"
                        val barcode2 = "1658312510"
                        val barcode3 = "6645106182"
                        val barcode4 = "6655879410"
                        val barcode5 = "5678910879"

                        val date1 = 1463642955000L //Thu May 19 09:29:15 GMT+02:00 2016
                        val date2 = 1463646433000L //Thu May 19 10:27:13 GMT+02:00 2016
                        val date3 = 1486620978000L //Thu Feb 09 08:16:18 GMT+02:00 2017
                        val date4 = 1486622248000L //Thu Feb 09 08:37:28 GMT+02:00 2017
                        val date5 = 1486623579000L //Thu Feb 09 08:59:39 GMT+02:00 2017
                        val date6 = 1486623813000L //Thu Feb 09 09:03:33 GMT+02:00 2017
                        val date7 = 1486624078000L //Thu Feb 09 09:07:58 GMT+02:00 2017
                        val date8 = 1486628102000L //Thu Feb 09 10:15:02 GMT+02:00 2017
                        val date9 = 1488956948000L //Wed Mar 08 09:09:08 GMT+02:00 2017
                        val date10 = 1489216596000L //Sat Mar 11 09:16:36GMT+02:00 2017
                        val date11 = 1463642083000L //Thu May 19 07:14:43 UTC 2016
                        val date12 = 1464505550000L //Sun May 29 09:05:50GMT+02:00 2016
                        val date13 = 1464590085000L //Mon May 30 08:34:45GMT+02:00 2016
                        val date14 = 1497184058000L //Sun Jun 11 14:27:38GMT+02:00 2017
                        val date15 = 1508839951000L //Tue Oct 24 12:12:31GMT+02:00 2017
                        val date16 = 1508838642000L //Tue Oct 24 11:50:42GMT+02:00 2017
                        val date17 = 1508838170000L //Tue Oct 24 11:42:50GMT+02:00 2017
                        val date18 = 1487425514000L //Sat Feb 18 15:45:14GMT+02:00 2017
                        val date19 = 1486967282000L //Mon Feb 13 08:28:02 GMT+02:00 2017
                        val date20 = 1487743357000L //Sat Feb 18 15:45:14GMT+02:00 2017
                        val date21 = 1488111870000L //Sun Feb 26 14:24:30 GMT+02:00 2017
                        val date22 = 1465906138000L //Tue Jun 14 14:08:58 GMT+02:00 2016
                        val date23 = 1466074166000L //Thu Jun 16 12:49:26 GMT+02:00 2016
                        val date24 = 1489211940000L //Sat Mar 11 07:59:00 GMT+02:00 2017
                        val date25 = 1489304204000L //Sun Mar 12 09:36:44 GMT+02:00 2017
                        val date26 = 1489384449000L //Mon Mar 13 07:54:09 GMT+02:00 2017
                        val date27 = 1486673826000L //Thu Feb 09 22:57:06 GMT+02:00 2017
                        val date28 = 1486627734000L //Thu Feb 09 10:08:54 GMT+02:00 2017
                        val date29 = 1486628102000L //Thu Feb 09 10:15:02 GMT+02:00 2017

                        val dateEX1 = 1486616999000L //Thu Feb 09 07:09:59 GMT+02:00 2017
                        val dateEX2 = 1488716999000L //Sun Mar 05 12:29:59 UTC 2017
                        val dateEX3 = 1046516999000L //Sat Mar 01 13:09:59 GMT+02:00 2003
                        val dateEX4 = 2546516999000L //Sun sept 11 15:49:59 GMT+02:00 2050
                        val dateEX5 = 1463646433000L //Thu May 19 10:27:13 GMT+02:00 2016

                        val sheets = listOf(
                                Sheet(5, "1869758886", 1330, 1, Date(date1), false, consignees = emptyList()),
                                Sheet(13, "3137717392", 1304, 1, Date(date2), false, consignees = emptyList()),
                                Sheet(9807, "1594413926", 4312, 1, Date(date3), true, consignees = emptyList()),
                                Sheet(9809, "1658312510", 3750, 1, Date(date4), true, consignees = emptyList()),
                                Sheet(9810, "6645106182", 1291, 1, Date(date5), true, consignees = emptyList()),
                                Sheet(9811, "9117946869", 6555, 1, Date(date6), true, consignees = emptyList()),
                                Sheet(9812, "8864499619", 1310, 1, Date(date7), true, consignees = emptyList()),
                                Sheet(9823, "2758121222", 1302, 1, Date(date8), true, consignees = emptyList()),
                                Sheet(10549, "3348199643", 1291, 1, Date(date9), true, consignees = emptyList()),
                                Sheet(10625, "8776391767", 1291, 1, Date(date10), true, consignees = emptyList()),
                                Sheet(3, "3027770656", 1290, 1, Date(date11), false, consignees = emptyList()),
                                Sheet(327, "6066583289", 1290, 1, Date(date12), true, consignees = emptyList()),
                                Sheet(368, "8482496036", 1290, 1, Date(date13), true, consignees = emptyList()),
                                Sheet(15301, "5244755029", 1290, 1, Date(date14), true, consignees = emptyList()),
                                Sheet(26307, "8194990039", 3229, 6, Date(date15), true, consignees = emptyList()),
                                Sheet(26306, "1688359452", 7952, 3, Date(date16), false, consignees = emptyList()),
                                Sheet(26304, "6717559108", 9349, 6, Date(date17), false, consignees = emptyList()),
                                Sheet(10054, "4022607498", 1290, 1, Date(date18), true, consignees = emptyList()),
                                Sheet(9905, "2057547779", 4272, 1, Date(date19), true, consignees = emptyList()),
                                Sheet(10155, "2047039381", 4312, 1, Date(date20), true, consignees = emptyList()),
                                Sheet(10278, "7490957260", 3229, 1, Date(date21), true, consignees = emptyList()),
                                Sheet(920, "6658855419", 3245, 1, Date(date22), true, consignees = emptyList()),
                                Sheet(1012, "8943630259", 3322, 1, Date(date23), true, consignees = emptyList()),
                                Sheet(10615, "8843433453", 4312, 1, Date(date24), true, consignees = emptyList()),
                                Sheet(10665, "5171577476", 3229, 1, Date(date25), true, consignees = emptyList()),
                                Sheet(10681, "1273437488", 4312, 1, Date(date26), true, consignees = emptyList()),
                                Sheet(9839, "7794680698", 4312, 1, Date(date27), true, consignees = emptyList()),
                                Sheet(9821, "1488393343", 6002, 1, Date(date28), true, consignees = emptyList()),
                                Sheet(9823, "2758121222", 1302, 1, Date(date29), true, consignees = emptyList()))

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

                        val expr1 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX1))
                        val expr2 = Condition("deliveryRunSheet.dateTime", Operator.LessThanOrEqual, Date(dateEX1))
                        val expr3 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX2))
                        val expr4 = Condition("deliveryRunSheet.dateTime", Operator.LessThanOrEqual, Date(dateEX1))
                        val expr5 = Condition("deliveryRunSheet.dateTime", Operator.LessThanOrEqual, Date(dateEX3)) // Empty
                        val expr6 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, Date(dateEX4)) // Empty
                        val expr7 = Condition("deliveryRunSheet.dateTime", Operator.Like, Date(dateEX1)) // Invalid
                        val expr8 = Condition("deliveryRunSheet.dateTime", Operator.GreaterThanOrEqual, dateEX5) // Invalid
                        val expr9 = Condition("barcode", Operator.Like, barcode1)
                        val expr10 = Condition("deliveryRunSheet.barcode", Operator.Like, barcode2)
                        val expr11 = Condition("barcode", Operator.Like, barcode3)
                        val expr12 = Condition("deliveryRunSheet.barcode", Operator.Like, barcode4) // Empty
                        val expr13 = Condition("barcode", Operator.Like, barcode5) // Empty
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
                        val expr28 = AndExpr(expr4, expr9)

                        val sort1 = SortBy("deliveryRunSheet.dateTime", Ascending())
                        val sort2 = SortBy("deliveryRunSheet.dateTime", Descending())
                        val sort3 = SortBy("bad", Ascending())

                        val paginationMap = hashMapOf(
                                Pagination(expr1, sort1, 0, 3) to sheets.subList(2, 5),
                                Pagination(expr1, sort2, 0, 3) to sheets.subList(14, 17),
                                Pagination(expr1, sort1, 1, 2) to sheets.subList(3, 5),
                                Pagination(expr1, sort1, 0, 1) to sheets.subList(2, 3),
                                Pagination(expr1, sort1, 0, 3) to sheets.subList(2, 5),
                                Pagination(expr1, sort1, 2, 3) to sheets.subList(4, 7),
                                Pagination(expr1, sort1, pageSize = 3) to sheets.subList(2, 5),
                                Pagination(expr1, pageSize = 3) to sheets.subList(18, 21),
                                Pagination(pageSize = 3) to listOf(sheets[10], sheets[22], sheets[21]).asReversed(),
                                Pagination(expr2, sort1, 0, 3) to listOf(sheets[10], sheets[0], sheets[1]),
                                Pagination(expr3, pageSize = 3) to sheets.subList(23, 26),
                                Pagination(expr4, pageSize = 3) to listOf(sheets[10], sheets[22], sheets[21]).asReversed(),
                                Pagination(expr5, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr6, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr9, pageSize = 3) to sheets.subList(2, 3),
                                Pagination(expr10, pageSize = 3) to sheets.subList(3, 4),
                                Pagination(expr11, pageSize = 3) to sheets.subList(4, 5),
                                Pagination(expr12, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr13, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr16, pageSize = 3) to sheets.subList(26, 29),
                                Pagination(expr17, pageSize = 3) to listOf(sheets[10], sheets[22], sheets[21]).asReversed(),
                                Pagination(expr18, pageSize = 3) to listOf(sheets[10], sheets[22], sheets[21]).asReversed(),
                                Pagination(expr19, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr21, pageSize = 3) to sheets.subList(2, 4),
                                Pagination(expr22, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr24, pageSize = 3) to sheets.subList(26, 29),
                                Pagination(expr25, pageSize = 3) to sheets.subList(2, 4),
                                Pagination(expr26, pageSize = 3) to emptyList(), //emptylist
                                Pagination(expr28, pageSize = 3) to sheets.subList(2, 3)
                        )

                        val faultyPaginationMap = hashMapOf<Pagination, Throwable>(
//                                Pagination(expr1) to RuntimeException(DATA_ERROR),
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

                        return object : GetAllRepositoryTest.TestParameter<Sheet> {

                            override fun getNormalPaginations(): Set<Pagination> =
                                    paginationMap.keys

                            override fun getAllWithNormalPagination(pagination: Pagination): Triple<Observable<out List<Sheet>>, Pagination, List<Sheet>> {
                                return Triple(sheetSuperGlideRepository.getAll(pagination),
                                        pagination,
                                        paginationMap[pagination]!!)
                            }

                            override fun getFaultyPaginations(): Set<Pagination> =
                                    faultyPaginationMap.keys

                            override fun getAllWithFaultyPagination(pagination: Pagination): Triple<Observable<out List<Sheet>>, Pagination, Throwable> {
                                return Triple(sheetSuperGlideRepository.getAll(pagination),
                                        pagination,
                                        faultyPaginationMap[pagination]!!)
                            }
                        }
                    }

                    override fun toString(): String =
                            SheetSuperGlideRepository::class.java.simpleName
                }),
                arrayOf(object : GetAllRepositoryTest.SetupTestParameter<ScannedBarcode> {
                    override fun setup(): GetAllRepositoryTest.TestParameter<ScannedBarcode> {
                        val openHelper = DaoMaster.DevOpenHelper(InstrumentationRegistry.getContext(), null)
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

                        return object : GetAllRepositoryTest.TestParameter<ScannedBarcode> {
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

                    override fun toString(): String =
                            ScannedBarcodeRepositoryImp::class.java.simpleName
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
}