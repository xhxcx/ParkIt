package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

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

        //Get entry and exit hours
        Instant inHour = ticket.getInTime().toInstant();
        Instant outHour = ticket.getOutTime().toInstant();

        //Calculate the duration and convert it from millis to hours by dividing by 1000*3600 (to seconds*to hours)
        double duration = Duration.between(inHour, outHour).toMillis()/3600000.0;

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

        ticket.setPrice(formatFareToPrice(fare));
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

    /**
     * Format a double to match a price display
     * Truncate the entry double to only keep 2 decimals rounding up
     *
     * @param exactFare double
     * @return double with 2 decimal
     */
    public double formatFareToPrice(double exactFare){
        return new BigDecimal(exactFare).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}