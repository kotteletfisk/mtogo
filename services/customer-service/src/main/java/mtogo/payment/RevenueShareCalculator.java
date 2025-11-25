package mtogo.payment;

import java.text.NumberFormat;
import java.time.*;
import java.util.Locale;

public class RevenueShareCalculator {

    private static final ZoneId COPENHAGEN = ZoneId.of("Europe/Copenhagen");
    private static final NumberFormat DKK_FORMAT = NumberFormat.getNumberInstance(new Locale("da", "DK"));

    static {
        DKK_FORMAT.setMaximumFractionDigits(2);
        DKK_FORMAT.setMinimumFractionDigits(2);
    }

    private static String formatAmount(double amount) {
        return DKK_FORMAT.format(amount) + " DKK";
    }


    /**
     * Progressive MTOGO fee as per assignment.
     */
    public static double calculateMtogoFee(double amountBeforeVat) {

        if (amountBeforeVat <= 100){
            return amountBeforeVat*0.20;
        }
        else if (amountBeforeVat <= 500){
            return amountBeforeVat*0.18;
        }
        else if (amountBeforeVat <= 1000){
            return amountBeforeVat*0.17;
        }
        else{
            return amountBeforeVat*0.15;
        }
    }

    /**
     * Detects if the order time is "late night" (22:00–06:00).
     */
    public static boolean isLateNight(LocalDateTime orderTime) {
        LocalTime t = orderTime.toLocalTime();
        // after 22:00 OR before 06:00
        return t.isAfter(LocalTime.of(21, 59)) || t.isBefore(LocalTime.of(6, 0));
    }


    /**
     * Courier bonus:
     *  - base 4% of order value
     *  - +2% extra if late-night
     */
    public static double calculateCourierBonus(double amountBeforeVat,
                                               LocalDateTime orderTime) {
        double bonus = amountBeforeVat * 0.04;

        if (isLateNight(orderTime)) {
            bonus += amountBeforeVat * 0.02;
        }

        return bonus;
    }

    /**
     * Print breakdown for demo / exam.
     */
    public static void printBreakdown(double amountBeforeVat,
                                      LocalDateTime orderTime) {

        double mtogoFee = calculateMtogoFee(amountBeforeVat);
        double courierBonus = calculateCourierBonus(amountBeforeVat, orderTime);

        double restaurantGets = amountBeforeVat - mtogoFee;
        double mtogoProfit = mtogoFee - courierBonus;

        System.out.printf("Payment breakdown for order: %s%n", formatAmount(amountBeforeVat));
        System.out.printf("  Late-night          : %s%n", isLateNight(orderTime));
        System.out.printf("  Restaurant receives : %s%n", formatAmount(restaurantGets));
        System.out.printf("  Courier bonus       : %s%n", formatAmount(courierBonus));
        System.out.printf("  MTOGO profit        : %s%n", formatAmount(mtogoProfit));
    }
}
