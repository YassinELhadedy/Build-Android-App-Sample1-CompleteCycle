package com.transporter.streetglide.infrastructure.dao;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToMany;

import java.util.List;

/**
 * Entity mapped to table "SHEET".
 */
@Entity(active = true)
public class DaoSheet implements DaoIdEntity {

    @Id
    private long id;

    @NotNull
    private int runnerId;

    @NotNull
    private int branchId;

    @NotNull
    private String barcode;

    @NotNull
    private java.util.Date dateTime;

    @ToMany(referencedJoinProperty = "sheetId")
    private List<DaoShipment> shipments;

    /**
     * Used to resolve relations
     */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /**
     * Used for active entity operations.
     */
    @Generated(hash = 1714661822)
    private transient DaoSheetDao myDao;

    @Generated(hash = 780803363)
    public DaoSheet(long id, int runnerId, int branchId, @NotNull String barcode,
                    @NotNull java.util.Date dateTime) {
        this.id = id;
        this.runnerId = runnerId;
        this.branchId = branchId;
        this.barcode = barcode;
        this.dateTime = dateTime;
    }

    @Generated(hash = 1047140536)
    public DaoSheet() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRunnerId() {
        return this.runnerId;
    }

    public void setRunnerId(int runnerId) {
        this.runnerId = runnerId;
    }

    public int getBranchId() {
        return this.branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public java.util.Date getDateTime() {
        return this.dateTime;
    }

    public void setDateTime(java.util.Date dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Resets a to-many relationship, making the next get call to query for a fresh result.
     */
    @Generated(hash = 1071984324)
    public synchronized void resetShipments() {
        shipments = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1055921339)
    public List<DaoShipment> getShipments() {
        if (shipments == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            DaoShipmentDao targetDao = daoSession.getDaoShipmentDao();
            List<DaoShipment> shipmentsNew = targetDao._queryDaoSheet_Shipments(id);
            synchronized (this) {
                if (shipments == null) {
                    shipments = shipmentsNew;
                }
            }
        }
        return shipments;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 154746521)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getDaoSheetDao() : null;
    }

}
