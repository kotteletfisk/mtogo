package mtogo.payment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


public class RevenueCalculatorTest {
    private final PrintStream out = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final double acceptableDelta = 0.0001;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }
    @AfterEach
    public void TearDownStreams() {
        System.setOut(out);
    }



    @Test
    void calculateMtogoFee_upTo100() {
        // 20 percent
        assertEquals(20.0, RevenueShareCalculator.calculateMtogoFee(100.0), acceptableDelta);
    }

    @Test
    void calculateMtogoFee_upTo500() {
        // 18 percent
        assertEquals(90.0, RevenueShareCalculator.calculateMtogoFee(500.0), acceptableDelta);
        assertEquals(45.0, RevenueShareCalculator.calculateMtogoFee(250.0), acceptableDelta);
    }

    @Test
    void calculateMtogoFee_upTo1000() {
        // 17 percent
        assertEquals(170.0, RevenueShareCalculator.calculateMtogoFee(1000.0), acceptableDelta);
        assertEquals(102.0, RevenueShareCalculator.calculateMtogoFee(600.0), acceptableDelta);
    }

    @Test
    void calculateMtogoFee_above1000() {
        // 15 percent
        assertEquals(180.0, RevenueShareCalculator.calculateMtogoFee(1200.0), acceptableDelta);
        assertEquals(300.0, RevenueShareCalculator.calculateMtogoFee(2000.0), acceptableDelta);
    }
    @Test
    public void testisLateNight_22_00(){
        // 22:00
        LocalDateTime t = LocalDateTime.of(2025, 1, 2, 22, 0);
        assertTrue(RevenueShareCalculator.isLateNight(t));
    }
    @Test
    public void testisLateNight_04_35(){
        // 04:35
        LocalDateTime t = LocalDateTime.of(2025, 1, 2, 4, 35);
        assertTrue(RevenueShareCalculator.isLateNight(t));
    }
    @Test
    public void testisLateNight_21_59(){
        // 21:59
        LocalDateTime t = LocalDateTime.of(2025, 1, 2, 21, 59);
        assertFalse(RevenueShareCalculator.isLateNight(t));
    }
    @Test
    public void testisLateNight18_45(){
        // 18:45
        LocalDateTime t = LocalDateTime.of(2025, 1, 2, 18, 45);
        assertFalse(RevenueShareCalculator.isLateNight(t));
    }

    @Test
    public void calculateCourierBonus_normal(){
        // normal bonus 4%
        LocalDateTime t = LocalDateTime.of(2025, 1, 2, 14, 0);
        assertEquals(8.0, RevenueShareCalculator.calculateCourierBonus(200.0, t), acceptableDelta);
    }
    @Test
    public void calculateCourierBonus_lateNight(){
        // normal bonus 4% + late night 2%
        LocalDateTime t = LocalDateTime.of(2025, 1, 2, 23, 0);
        assertEquals(12.0, RevenueShareCalculator.calculateCourierBonus(200.0, t), acceptableDelta);
    }


    @Test
    public void PrintBreakdown_demo(){
        double amountBeforeVat = 600;
        LocalDateTime orderTime = LocalDateTime.of(2025, 1, 2, 23, 30);
        RevenueShareCalculator.printBreakdown(amountBeforeVat, orderTime);
        String ls = System.lineSeparator();

        // 600 * 0.17 = 102
        // restaurant: 600 - 102 = 498 DKK
        // Courier: 600 * 0.04 = 24 + late night 600 * 0.02 = 12 -> 36 DKK
        // MTOGO profit: 102 - 36 = 66 DKK

        assertEquals(
                "Payment breakdown for order: 600,00 DKK" + ls +
                        "  Late-night          : true" + ls +
                        "  Restaurant receives : 498,00 DKK" + ls +
                        "  Courier bonus       : 36,00 DKK" + ls +
                        "  MTOGO profit        : 66,00 DKK",
                outContent.toString().trim()
        );
    }

}
