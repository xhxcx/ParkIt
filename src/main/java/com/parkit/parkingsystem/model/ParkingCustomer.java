package com.parkit.parkingsystem.model;

public class ParkingCustomer {
    private int id;
    private String plateNumber;
    private double discountBonus;

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public void setPlateNumber(String plateNumber) { this.plateNumber=plateNumber; }

    public String getPlateNumber() { return plateNumber; }

    public void setDiscountBonus(double discount) { this.discountBonus=discount; }

    public double getDiscountBonus(){ return discountBonus; }
}
