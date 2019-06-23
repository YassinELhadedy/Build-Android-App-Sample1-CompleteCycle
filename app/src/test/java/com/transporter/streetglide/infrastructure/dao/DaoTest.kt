package com.transporter.streetglide.infrastructure.dao

import org.greenrobot.greendao.AbstractDao
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * DaoTest abstract class
 */
@Config
@RunWith(RobolectricTestRunner::class)
abstract class DaoTest<T : DaoIdEntity, out U : AbstractDao<T, Long>> {
    protected lateinit var daoSession: DaoSession
    protected abstract val dao: U
    protected abstract val it: T
    protected abstract fun assertDeepEquals(expected: T, actual: T)
    protected abstract fun assertChanged(expected: T, actual: T)
    protected abstract fun doChange(it: T)

    @Before
    fun setUp() {
        val openHelper = DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null)
        daoSession = DaoMaster(openHelper.writableDb).newSession()
    }

    @Test
    fun testInsert() {
        val it = it

        daoSession.insert(it)
        daoSession.clear()
        val loaded = dao.load(it.id)

        Assert.assertNotNull(loaded)
        Assert.assertNotSame(it, loaded)
        Assert.assertEquals(it.id, loaded.id)
        assertDeepEquals(it, loaded)
    }

    @Test
    fun testDelete() {
        val it = it

        daoSession.insert(it)
        daoSession.delete(it)
        daoSession.clear()

        Assert.assertNull(dao.load(it.id))
    }

    @Test
    fun testUpdate() {
        val it = it

        daoSession.insert(it)
        doChange(it)
        daoSession.update(it)
        daoSession.clear()

        assertChanged(this.it, dao.load(it.id))
    }

}