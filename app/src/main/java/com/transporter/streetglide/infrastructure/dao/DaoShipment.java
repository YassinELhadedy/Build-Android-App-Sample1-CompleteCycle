package com.transporter.streetglide.infrastructure.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Unique;

/**
 * Entity mapped to table "SHIPMENT".
 */
@Entity
public class DaoShipment implements DaoIdEntity {

    @Id
    private long id;

    @NotNull
    @Unique
    private String barcode;

    private String note;

    @NotNull
    private String consigneeName;

    @NotNull
    private String phone;

    private Integer pickUpId;

    // FIXME: Should not an Enum
    @NotNull
    private int status;

    // FIXME: May be we need more details here
    @NotNull
    private Double goodsPrice;

    @NotNull
    private Double freightChargesOnReceiver;

    @NotNull
    private Double freightChargesOnClient;

    @NotNull
    private int areaId;

    @NotNull
    private String areaName;

    @NotNull
    private String areaCity;

    @NotNull
    private String areaGovernorate;

    @NotNull
    private String street;

    private String propertyNumber;

    private Integer floor;

    private String apartement;

    private String specialMark;

    // FIXME: Should not an Enum
    @NotNull
    private int type;

    private Integer runnerId;

    @NotNull
    private long sheetId;

    @Generated(hash = 172060238)
    public DaoShipment(long id, @NotNull String barcode, String note, @NotNull String consigneeName,
                       @NotNull String phone, Integer pickUpId, int status, @NotNull Double goodsPrice,
                       @NotNull Double freightChargesOnReceiver, @NotNull Double freightChargesOnClient,
                       int areaId, @NotNull String areaName, @NotNull String areaCity,
                       @NotNull String areaGovernorate, @NotNull String street, String propertyNumber,
                       Integer floor, String apartement, String specialMark, int type, Integer runnerId,
                       long sheetId) {
        this.id = id;
        this.barcode = barcode;
        this.note = note;
        this.consigneeName = consigneeName;
        this.phone = phone;
        this.pickUpId = pickUpId;
        this.status = status;
        this.goodsPrice = goodsPrice;
        this.freightChargesOnReceiver = freightChargesOnReceiver;
        this.freightChargesOnClient = freightChargesOnClient;
        this.areaId = areaId;
        this.areaName = areaName;
        this.areaCity = areaCity;
        this.areaGovernorate = areaGovernorate;
        this.street = street;
        this.propertyNumber = propertyNumber;
        this.floor = floor;
        this.apartement = apartement;
        this.specialMark = specialMark;
        this.type = type;
        this.runnerId = runnerId;
        this.sheetId = sheetId;
    }

    @Generated(hash = 825796565)
    public DaoShipment() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getConsigneeName() {
        return this.consigneeName;
    }

    public void setConsigneeName(String consigneeName) {
        this.consigneeName = consigneeName;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Double getGoodsPrice() {
        return this.goodsPrice;
    }

    public void setGoodsPrice(Double goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public Double getFreightChargesOnReceiver() {
        return this.freightChargesOnReceiver;
    }

    public void setFreightChargesOnReceiver(Double freightChargesOnReceiver) {
        this.freightChargesOnReceiver = freightChargesOnReceiver;
    }

    public Double getFreightChargesOnClient() {
        return this.freightChargesOnClient;
    }

    public void setFreightChargesOnClient(Double freightChargesOnClient) {
        this.freightChargesOnClient = freightChargesOnClient;
    }

    public int getAreaId() {
        return this.areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public String getStreet() {
        return this.street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPropertyNumber() {
        return this.propertyNumber;
    }

    public void setPropertyNumber(String propertyNumber) {
        this.propertyNumber = propertyNumber;
    }

    public Integer getFloor() {
        return this.floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getApartement() {
        return this.apartement;
    }

    public void setApartement(String apartement) {
        this.apartement = apartement;
    }

    public String getSpecialMark() {
        return this.specialMark;
    }

    public void setSpecialMark(String specialMark) {
        this.specialMark = specialMark;
    }

    public long getSheetId() {
        return this.sheetId;
    }

    public void setSheetId(long sheetId) {
        this.sheetId = sheetId;
    }

    public Integer getPickUpId() {
        return this.pickUpId;
    }

    public void setPickUpId(Integer pickUpId) {
        this.pickUpId = pickUpId;
    }

    public Integer getRunnerId() {
        return this.runnerId;
    }

    public void setRunnerId(Integer runnerId) {
        this.runnerId = runnerId;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getAreaName() {
        return this.areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getAreaCity() {
        return this.areaCity;
    }

    public void setAreaCity(String areaCity) {
        this.areaCity = areaCity;
    }

    public String getAreaGovernorate() {
        return this.areaGovernorate;
    }

    public void setAreaGovernorate(String areaGovernorate) {
        this.areaGovernorate = areaGovernorate;
    }
}