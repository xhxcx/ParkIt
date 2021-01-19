package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
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

        // WHEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        try {
            myTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // THEN : check ticket in DB information and availability of parking spot
            assertThat(myTicket).isNotNull();
            assertThat(myTicket.getVehicleRegNumber()).isEqualTo("ABCDEF");
            assertThat(myTicket.getParkingSpot().hashCode()).isEqualTo(1);
            assertThat(myTicket.getPrice()).isEqualTo(0);

            assertThat(parkingSpotDAO.getNextAvailableSlot(myTicket.getParkingSpot().getParkingType())).isNotEqualTo(1);
            assertThat(myTicket.getParkingSpot().isAvailable()).isFalse();
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
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        // get ticket from DB
        myTicket = ticketDAO.getTicket("ABCDEF");

        // THEN : check exit time, price and if parking spot has been freed
        assertThat(myTicket.getOutTime()).isAfter(myTicket.getInTime());
        assertThat(myTicket.getPrice()).isGreaterThan(0);
        assertThat(parkingService.getNextParkingNumberIfAvailable().hashCode()).isEqualTo(1);

    }

}
