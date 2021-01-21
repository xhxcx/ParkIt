package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingCustomerDAO;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingCustomer;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static ParkingCustomerDAO customerDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static Ticket myTicket;
    private final static Date fakeInTime = new Date();

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        customerDAO = new ParkingCustomerDAO();
        customerDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();

        myTicket = null;
        fakeInTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );

    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar(){
        //GIVEN see before each SetUpPerTest
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);

        // WHEN
        parkingService.processIncomingVehicle();
        myTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());

        // THEN : check ticket in DB information and availability of parking spot
            assertThat(myTicket).isNotNull();
            assertThat(myTicket.getVehicleRegNumber()).isEqualTo("ABCDEF");
            assertThat(myTicket.getParkingSpot().hashCode()).isEqualTo(1);
            assertThat(myTicket.getPrice()).isEqualTo(0);

            assertThat(parkingSpotDAO.getNextAvailableSlot(myTicket.getParkingSpot().getParkingType())).isNotEqualTo(1);
            assertThat(myTicket.getParkingSpot().isAvailable()).isFalse();
    }

    @Test
    public void testParkingARecurringCar(){
        //GIVEN see before each SetUpPerTest
        ParkingCustomer customer = new ParkingCustomer();
        customer.setPlateNumber("ABCDEF");
        customer.setDiscountBonus(Fare.RECURRING_USER_DISCOUNT);
        customerDAO.saveParkingCustomer(customer);


        // WHEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);
        parkingService.processIncomingVehicle();
        try {
            myTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // THEN : check ticket in DB is eligible to the discount and check if the customer is correctly get
        assertThat(myTicket.isEligibleForRecurringUser()).isTrue();
        assertThat(customerDAO.getParkingCustomer(myTicket.getVehicleRegNumber())).isNotNull();
        assertThat(customerDAO.getParkingCustomer(myTicket.getVehicleRegNumber()).getDiscountBonus()).isEqualTo(0.05);
    }

    @Test
    public void testIncrementationOfParkingSpot(){
        //GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);
        ParkingSpot firstParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //WHEN
        parkingService.processIncomingVehicle();
        ParkingSpot secondParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //THEN verify if the 2 parking spots are different so that the update parking with the 1st is ok
        assertFalse(firstParkingSpot.equals(secondParkingSpot));
    }

    @Test
    public void testParkingLotExit(){

        // GIVEN
        testParkingACar();
        //Modify entry time to set it 24 hours ago and save the ticket
        myTicket.setInTime(fakeInTime);
        System.out.println("Override in time to : " + myTicket.getInTime());
        ticketDAO.saveTicket(myTicket);

        // WHEN : process exiting
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);
        parkingService.processExitingVehicle();
        // get ticket from DB
        myTicket = ticketDAO.getTicket("ABCDEF");

        // THEN : check exit time, price and if parking spot has been freed
        assertThat(myTicket.getOutTime()).isAfter(myTicket.getInTime());
        assertThat(myTicket.getPrice()).isGreaterThan(0);
        assertThat(parkingService.getNextParkingNumberIfAvailable().hashCode()).isEqualTo(1);

    }

}
