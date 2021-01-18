package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.time.Duration;

/**
 * Fare calculator to set price for a given Ticket
 *
 */
public class FareCalculatorService {

    /**
     * Calculate a fee based on a Ticket
     * get entry and exit hours
     * then multiply the duration of stay by the rate regarding the parking type
     *
     * @param ticket
     */
    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        //Get entry and exit hours in millis
        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        //Calculate the duration and convert it from millis to hours by dividing by 3600*1000
        double duration = (double)Duration.ofMillis(outHour - inHour).toMillis()/3600000;

        double fare = 0;

        // Calculate fee only if fare shouldn't be free
        if(!isFreeFare(duration)) {
            //Apply fare regarding parking type
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    fare = (duration * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    fare = (duration * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
            //Apply recurring discount if ticket is eligible
            fare -= ticket.isEligibleForRecurringUser()?(fare*Fare.RECURRING_USER_DISCOUNT):0;
        }

        ticket.setPrice(fare);
    }

    /**
     * Evaluate if a fee should be free or not
     *
     * @param durationStay in hours
     * @return True only if the stay is less than 30 minutes
     */
    public boolean isFreeFare(double durationStay){
        return(durationStay<0.5);
    }
}