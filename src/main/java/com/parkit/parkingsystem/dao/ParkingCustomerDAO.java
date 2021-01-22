package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.model.ParkingCustomer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Manage interaction with parking DB table customer
 */
public class ParkingCustomerDAO {

    private static final Logger logger = LogManager.getLogger("CustomerDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    /**
     * Save a ParkingCustomer object in the DB
     *
     * @param customer ParkingCustomer object
     * @return boolean true if connection and insert are ok
     * @throws Exception ex if DB connection failed
     */
    public boolean saveParkingCustomer(ParkingCustomer customer){
        try (Connection con = dataBaseConfig.getConnection(); PreparedStatement ps =con.prepareStatement(DBConstants.SAVE_CUSTOMER)){
            ps.setString(1, customer.getPlateNumber());
            ps.setDouble(2, customer.getDiscountBonus());
            return ps.execute();
        }catch (SQLException | ClassNotFoundException ex){
            logger.error("Error fetching parking customer",ex);
            return false;
        }
    }

    /**
     * Search in DB if there is an existing customer with the given plate number
     *
     * @param plateNumber vehicle reg number
     * @return ParkingCustomer with attributes set
     * @throws Exception ex if DB connection failed
     */
    public ParkingCustomer getParkingCustomer(String plateNumber) {
        ParkingCustomer parkingCustomer = null;
        try (Connection con = dataBaseConfig.getConnection() ; PreparedStatement ps = con.prepareStatement(DBConstants.GET_CUSTOMER)) {
            // ID,VEHICLE_REG_NUMBER, DISCOUNT_AVAILABLE
            ps.setString(1, plateNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                parkingCustomer = new ParkingCustomer();
                parkingCustomer.setPlateNumber(plateNumber);
                parkingCustomer.setId(rs.getInt(2));
                parkingCustomer.setDiscountBonus(rs.getDouble(3));
            }

            dataBaseConfig.closeResultSet(rs);
            //dataBaseConfig.closePreparedStatement(ps);
        } catch (SQLException | ClassNotFoundException ex) {
            logger.error("Error fetching parking customer", ex);
        }

        return parkingCustomer;
    }
}
