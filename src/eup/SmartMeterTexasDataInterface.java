package eup;

import java.time.LocalDate;

interface SmartMeterTexasDataInterface {

    void setFeedbacker(Feedbacker fb);

    @Override
    String toString();

    /**
     * @return the date of this object.  The date is for a meter reading 
     *         at midnight at the start of this date. 
     */
    LocalDate getDate();

    /**
     * @return whether the date of this object was changed (dateChanged) due
     *         to unavailability of data for the original date.
     */
    boolean isDateChanged();

    /**
     * @return the minimum start reading value for which the output should be
     *         colored green.  The output will be red below this value.
     */
    int getGreenStart();

    /**
     * @return the maximum start reading value for which the output should be
     *         colored green.  The output will be red above this value.
     */
    int getGreenEnd();

    /**
     * @return the meter reading for this date.
     *         The meter reading is truncated to an integer.
     */
    int getStartRead();

}