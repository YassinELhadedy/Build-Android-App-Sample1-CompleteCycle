package com.transporter.streetglide.ui.util

import android.content.Context
import android.util.Log
import com.transporter.streetglide.infrastructure.dao.DaoMaster
import com.transporter.streetglide.infrastructure.dao.DaoMaster.OpenHelper
import org.greenrobot.greendao.database.Database



class ProductionDbOpenHelper(context: Context, name: String) : OpenHelper(context, name) {

    override fun onCreate(db: Database) {
        Log.i("greenDAO", "Creating tables for schema version " + DaoMaster.SCHEMA_VERSION)
        DaoMaster.createAllTables(db, false)
    }

    override fun onUpgrade(db: Database, oldVersion: Int, newVersion: Int) {
        super.onUpgrade(db, oldVersion, newVersion)
        Log.i("greenDAO", "migrating schema from version $oldVersion to $newVersion")
        for (migrateVersion in oldVersion + 1..newVersion)
            upgrade(db, migrateVersion)
    }

    /**
     * in case of android.database.sqlite.SQLiteException, the schema version is
     * left untouched just fix the code in the version case and push a new
     * release
     *
     * @param db
     * @param migrateVersion
     */
    @Suppress("UNUSED_EXPRESSION")
    private fun upgrade(@Suppress("UNUSED_PARAMETER") db: Database, migrateVersion: Int) {
        when (migrateVersion) {
        /*   2 ->
               db.execSQL("ALTER TABLE INHABITANT ADD COLUMN 'SPECIES' TEXT;")
               db.execSQL("ALTER TABLE INVERTEBRATE ADD COLUMN 'SPECIES' TEXT;")
               db.execSQL("ALTER TABLE PLANT ADD COLUMN 'SPECIES' TEXT;")
               db.execSQL("ALTER TABLE CORAL ADD COLUMN 'SPECIES' TEXT;") */
        }
    }
}