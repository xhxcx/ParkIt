package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingCustomerDAO;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingCustomer;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 * Manage parking entry and exit regarding informations provided by user
 */
public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private static FareCalculatorService fareCalculatorService = new FareCalculatorService();

    private InputReaderUtil inputReaderUtil;
    private ParkingSpotDAO parkingSpotDAO;
    private  TicketDAO ticketDAO;
    private ParkingCustomerDAO customerDAO;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO, ParkingCustomerDAO parkingCustomerDAO){
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
        this.customerDAO = parkingCustomerDAO;
    }

    /**
     * Verify if a parking spot is available
     * get informations about the vehicle from user entry
     * Set a ticket and save it in DB
     * Print to the user the spot available and ticket informations
     *
     * @throws Exception e if DB connection issues
     *
     */
    public void processIncomingVehicle() {
        try{
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if(parkingSpot !=null && parkingSpot.getId() > 0){
                String vehicleRegNumber = getVehichleRegNumber();
                parkingSpot.setAvailable(false);
                parkingSpotDAO.updateParking(parkingSpot);//allot this parking space and mark it's availability as false

                Date inTime = new Date();
                Ticket ticket = new Ticket();
                //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME, ELIGIBLE_TO_DISCOUNT)
                //ticket.setId(ticketID);
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(0);
                ticket.setInTime(inTime);
                ticket.setOutTime(null);

                //Verify if the vehicle is already registered as customer
                if(null == customerDAO.getParkingCustomer(vehicleRegNumber)) {
                    //If not : create new customer and set the ticket not eligible for discount
                    addNewParkingCustomer(vehicleRegNumber, customerDAO);
                    ticket.setIsEligibleForRecurringUser(false);
                }

                else {
                    ticket.setIsEligibleForRecurringUser(true);
                    System.out.println("Welcome back! As a recurring user of our parking lot, you'll benefit from a " + Fare.RECURRING_USER_DISCOUNT*100 + "% discount.");
                }

                ticketDAO.saveTicket(ticket);
                System.out.println("Generated Ticket and saved in DB");
                System.out.println("Please park your vehicle in spot number:"+parkingSpot.getId());
                System.out.println("Recorded in-time for vehicle number:"+vehicleRegNumber+" is:"+inTime);

            }
        }catch(Exception e){
            logger.error("Unable to process incoming vehicle",e);
        }
    }

    /**
     * Ask user to enter his plate number then get it with InputReader
     *
     * @return Plate number of the entering vehicle
     * @throws Exception
     */
    private String getVehichleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    /**
     * Verify if the spots are available parsing spots by hash
     *
     * @return first spot available as ParkingSpot
     *
     * @throws Exception if no spots available with a message displayed to the user
     * @throws IllegalArgumentException ie if the vehicle type is not recognised
     * @throws Exception e if there is an error with DB connection
     */
    public ParkingSpot getNextParkingNumberIfAvailable(){
        int parkingNumber=0;
        ParkingSpot parkingSpot = null;
        try{
            ParkingType parkingType = getVehichleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if(parkingNumber > 0){
                parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
            }else{
                throw new Exception("Error fetching parking number from DB. Parking slots might be full");
            }
        }catch(IllegalArgumentException ie){
            logger.error("Error parsing user input for type of vehicle", ie);
        }catch(Exception e){
            logger.error("Error fetching next available parking slot", e);
        }
        return parkingSpot;
    }

    /**
     * Ask user to select his vehicle type then use InputReader to get it
     *
     * @return ParkingType of the parked vehicle (See ParkingType constants)
     */
    private ParkingType getVehichleType(){
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch(input){
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    /**
     * Verify if the vehicle is a recurring user or not (create a customer if needed)
     * Calculate the fee for the stay
     * Update the ticket at the exit of a vehicle to set out time and price regarding if a discount is available
     * Free the parking spot and update ParkingSpot
     * Print the Ticket informations related to the exit
     *
     * @throws Exception e if database connection fails for TicketDAO or ParkingCustomerDAO
     */
    public void processExitingVehicle() {
        try{
            String vehicleRegNumber = getVehichleRegNumber();
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            Date outTime = new Date();

            ticket.setOutTime(outTime);

            fareCalculatorService.calculateFare(ticket);
            if(ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                parkingSpotDAO.updateParking(parkingSpot);
                System.out.println("Please pay the parking fare:" + ticket.getPrice());
                System.out.println("Recorded out-time for vehicle number:" + ticket.getVehicleRegNumber() + " is:" + outTime);
            }else{
                System.out.println("Unable to update ticket information. Error occurred");
            }
        }catch(Exception e){
            logger.error("Unable to process exiting vehicle",e);
        }
    }

    /**
     * Create a new ParkingCustomer using the plate number
     * Add to the customer the basic RECURRING_USER_DISCOUNT fare discount
     *
     * @param plateNumber String of the vehicle reg number
     * @param customerDAO Data object to save in the customer table
     */
    private void addNewParkingCustomer(String plateNumber, ParkingCustomerDAO customerDAO) {
        ParkingCustomer customer = new ParkingCustomer();
        customer.setPlateNumber(plateNumber);
        customer.setDiscountBonus(Fare.RECURRING_USER_DISCOUNT);

        customerDAO.saveParkingCustomer(customer);
    }
}
