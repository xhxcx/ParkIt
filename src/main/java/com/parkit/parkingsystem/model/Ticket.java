package com.parkit.parkingsystem.model;

import java.util.Date;

public class Ticket {
    private int id;
    private ParkingSpot parkingSpot;
    private String vehicleRegNumber;
    private double price;
    private Date inTime;
    private Date outTime;
    private Boolean isEligibleForRecurringUser;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }

    public void setParkingSpot(ParkingSpot parkingSpot) {
        this.parkingSpot = parkingSpot;
    }

    public String getVehicleRegNumber() {
        return vehicleRegNumber;
    }

    public void setVehicleRegNumber(String vehicleRegNumber) {
        this.vehicleRegNumber = vehicleRegNumber;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Date getInTime() { return null!=inTime ? (Date) inTime.clone() : null; }

    public void setInTime(Date inTime) {
        this.inTime = null != inTime ? (Date) inTime.clone() : null;
    }

    public Date getOutTime() {
        return null != outTime ? (Date) outTime.clone() : null;
    }

    public void setOutTime(Date outTime) { this.outTime = null != outTime ? (Date) outTime.clone() : null; }

    public void setIsEligibleForRecurringUser(boolean b) { this.isEligibleForRecurringUser = b; }

    public Boolean isEligibleForRecurringUser(){ return isEligibleForRecurringUser; }
}
