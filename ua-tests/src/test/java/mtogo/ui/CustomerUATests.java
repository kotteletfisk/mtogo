package mtogo.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerUATests extends BaseUITest {

    @AfterEach
    void printBrowserLogs() {
        System.out.println("===== BROWSER CONSOLE LOGS =====");
        driver.manage().logs().get(LogType.BROWSER)
                .forEach(log -> System.out.println(log.getLevel() + ": " + log.getMessage()));
    }


    @Test
    void findActiveSuppliers_showsSuppliersForZip() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Navigate to main page
        System.out.println(">>> Navigating to: " + baseUrl);
        driver.get(baseUrl);
        System.out.println(">>> Page title: " + driver.getTitle());

        // Adding zipcode as input
        WebElement zipInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("zipcode"))
        );
        zipInput.clear();
        zipInput.sendKeys("2200");
        System.out.println(">>> Typed zipcode: 2200");

        // Clicking "Find active suppliers" button
        WebElement findBtn = driver.findElement(
                By.xpath("//button[contains(., 'Find active suppliers')]")
        );
        System.out.println(">>> Clicking 'Find active suppliers' button");
        findBtn.click();

        WebElement supplierSelect = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("supplierSelect"))
        );

        // Wait until at least one option is loaded
        wait.until(d -> {
            int count = supplierSelect.findElements(By.tagName("option")).size();
            System.out.println(">>> supplierSelect has " + count + " <option> elements");
            return count > 1;
        });

        List<WebElement> options = supplierSelect.findElements(By.tagName("option"));

        System.out.println("===== SUPPLIER OPTIONS FOUND =====");
        for (WebElement opt : options) {
            String value = opt.getAttribute("value");
            String text = opt.getText();
            System.out.println("  option: value='" + value + "', text='" + text + "'");
        }

        List<WebElement> statusMessages = driver.findElements(By.cssSelector(".status-message"));
        if (!statusMessages.isEmpty()) {
            System.out.println("===== STATUS MESSAGES ON PAGE =====");
            for (WebElement msg : statusMessages) {
                System.out.println("  class='" + msg.getAttribute("class") + "', text='" + msg.getText() + "'");
            }
        }

        // Require at least one supplier option for test to pass
        assertTrue(options.size() > 1, "Expected at least one supplier option");

        boolean hasSomeSupplier = options.stream()
                .anyMatch(o -> !o.getAttribute("value").isEmpty());
        assertTrue(hasSomeSupplier, "Expected at least one non-empty supplier value");

        System.out.println(">>> Test findActiveSuppliers_showsSuppliersForZip PASSED");
    }

    @Test
    void choosingMenuItems_addsThemToBasket() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        System.out.println(">>> Navigating to: " + baseUrl);
        driver.get(baseUrl);

        // Find suppliers for a zipcode
        WebElement zipInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("zipcode"))
        );
        zipInput.clear();
        zipInput.sendKeys("2200");
        System.out.println(">>> Typed zipcode: 2200");

        WebElement findBtn = driver.findElement(
                By.xpath("//button[contains(., 'Find active suppliers')]")
        );
        System.out.println(">>> Clicking 'Find active suppliers' button");
        findBtn.click();

        WebElement supplierSelect = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("supplierSelect"))
        );
        wait.until(d -> {
            int count = supplierSelect.findElements(By.tagName("option")).size();
            System.out.println(">>> supplierSelect has " + count + " <option> elements");
            return count > 1;
        });

        // Choosing a supplier
        List<WebElement> supplierOptions = supplierSelect.findElements(By.tagName("option"));

        WebElement chosenSupplier = supplierOptions.get(1);
        System.out.println(">>> Choosing supplier: value='" +
                chosenSupplier.getAttribute("value") +
                "', text='" + chosenSupplier.getText() + "'");
        chosenSupplier.click();

        // Loading the suppliers menu
        WebElement loadMenuBtn = driver.findElement(
                By.xpath("//button[contains(., 'Load menu items')]")
        );
        System.out.println(">>> Clicking 'Load menu items'");
        loadMenuBtn.click();

        // Wait for menu items container
        WebElement menuContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//label[contains(., 'Menu items for selected supplier')]/following-sibling::div")
                )
        );

        // Wait for at least one "Add to order" button
        wait.until(d ->
                menuContainer.findElements(By.xpath(".//button[contains(., 'Add to order')]")).size() > 0
        );

        List<WebElement> addButtons = menuContainer.findElements(
                By.xpath(".//button[contains(., 'Add to order')]")
        );
        System.out.println(">>> Found " + addButtons.size() + " 'Add to order' buttons in menu");

        assertFalse(addButtons.isEmpty(), "Expected at least one menu item to add");

        // Counting order lines before adding items
        List<WebElement> orderLinesBefore = driver.findElements(By.cssSelector(".orderline-row"));
        int beforeCount = orderLinesBefore.size();
        System.out.println(">>> Order lines before adding: " + beforeCount);

        // Adding first item to the basket
        System.out.println(">>> Clicking first 'Add to order'");
        addButtons.get(0).click();

        // Adding second item to the basket, if available
        if (addButtons.size() > 1) {
            System.out.println(">>> Clicking second 'Add to order'");
            addButtons.get(1).click();
        }

        // Check that order lines increased
        wait.until(d -> {
            int count = d.findElements(By.cssSelector(".orderline-row")).size();
            System.out.println(">>> Order lines currently: " + count);
            return count > beforeCount;
        });

        List<WebElement> orderLinesAfter = driver.findElements(By.cssSelector(".orderline-row"));
        int afterCount = orderLinesAfter.size();
        System.out.println(">>> Order lines after adding: " + afterCount);

        assertTrue(afterCount > beforeCount,
                "Expected number of order lines to increase after adding items");

        // Check that at least one line has all fields filled
        long filledLines = orderLinesAfter.stream().filter(line -> {
            List<WebElement> inputs = line.findElements(By.tagName("input"));
            if (inputs.size() < 3) {
                return false;
            }
            String itemId = inputs.get(0).getAttribute("value");
            String price = inputs.get(1).getAttribute("value");
            String amount = inputs.get(2).getAttribute("value");
            return !itemId.isBlank() && !price.isBlank() && !amount.isBlank();
        }).count();

        System.out.println(">>> Basket/order lines with all fields filled: " + filledLines);

        assertTrue(filledLines >= 1,
                "Expected at least one basket line with item_id, price_snapshot and amount filled");

        System.out.println(">>> Test choosingMenuItems_addsThemToBasket PASSED");
    }

    @Test
    void placingOrder_withItemsInBasket_showsPaymentAndConfirmation_andIsPersistedInDb() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        System.out.println(">>> Navigating to: " + baseUrl);
        driver.get(baseUrl);

        // --- GIVEN: put items in basket (same steps as before) ---

        WebElement zipInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("zipcode"))
        );
        zipInput.clear();
        zipInput.sendKeys("2200");

        WebElement findBtn = driver.findElement(
                By.xpath("//button[contains(., 'Find active suppliers')]")
        );
        findBtn.click();

        WebElement supplierSelect = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("supplierSelect"))
        );
        wait.until(d -> supplierSelect.findElements(By.tagName("option")).size() > 1);

        List<WebElement> supplierOptions = supplierSelect.findElements(By.tagName("option"));
        supplierOptions.get(1).click(); // first real supplier

        WebElement loadMenuBtn = driver.findElement(
                By.xpath("//button[contains(., 'Load menu items')]")
        );
        loadMenuBtn.click();

        WebElement menuContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//label[contains(., 'Menu items for selected supplier')]/following-sibling::div")
                )
        );

        wait.until(d ->
                menuContainer.findElements(By.xpath(".//button[contains(., 'Add to order')]")).size() > 0
        );

        List<WebElement> addButtons = menuContainer.findElements(
                By.xpath(".//button[contains(., 'Add to order')]")
        );
        assertFalse(addButtons.isEmpty(), "Expected at least one menu item to add");

        List<WebElement> orderLinesBefore = driver.findElements(By.cssSelector(".orderline-row"));
        int beforeCount = orderLinesBefore.size();

        addButtons.get(0).click();

        wait.until(d -> d.findElements(By.cssSelector(".orderline-row")).size() > beforeCount);

        // --- WHEN: choose payment & press "Create order" ---

        WebElement paymentSelect = driver.findElement(By.id("paymentMethod"));
        paymentSelect.findElement(By.cssSelector("option[value='MOBILEPAY']")).click();

        WebElement customerIdInput = driver.findElement(By.id("customerId"));
        customerIdInput.clear();
        customerIdInput.sendKeys("3");

        WebElement createBtn = driver.findElement(
                By.xpath("//button[contains(., 'Create order')]")
        );
        createBtn.click();

        // --- THEN: success message as before ---

        WebElement statusMsg = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".status-message")
                )
        );

        String statusClass = statusMsg.getAttribute("class");
        String statusText = statusMsg.getText();

        System.out.println("===== STATUS MESSAGE =====");
        System.out.println("class='" + statusClass + "'");
        System.out.println("text='" + statusText + "'");

        assertTrue(statusClass.contains("status-success"),
                "Expected a success status message class but got: " + statusClass);

        assertTrue(
                statusText.toLowerCase().contains("order created"),
                "Expected confirmation about order creation, got: " + statusText
        );

        //
        // EXTRA: verify persistence in Postgres
        //

        // 1) Extract JSON after "Server said:"
        String marker = "Server said:";
        int idx = statusText.indexOf(marker);
        assertTrue(idx >= 0, "Expected 'Server said:' in status text");

        String jsonPart = statusText.substring(idx + marker.length()).trim();
        System.out.println(">>> Extracted JSON from confirmation: " + jsonPart);

        // 2) Parse JSON to get orderId
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonPart);
        String orderIdStr = root.get("orderId").asText();
        System.out.println(">>> Order ID from confirmation: " + orderIdStr);

        // 3) Query Postgres

        //String host = "mtogo-db";
        String host = "172.16.0.11";
        String port = "5432";
        String db = System.getenv().getOrDefault("POSTGRES_DB", "mtogo");
        String dbUser = System.getenv().getOrDefault("POSTGRES_USER", "mtogo-test1234");
        String dbPass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "test1234");
        String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;

        System.out.println(">>> Checking DB for order " + orderIdStr);

        boolean found = false;
        String statusInDb = null;
        int customerInDb = -1;

        // Small poll loop in case the async consumer is a bit delayed
        long deadline = System.currentTimeMillis() + 10_000; // 10 seconds
        while (System.currentTimeMillis() < deadline && !found) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT customer_id, order_status FROM orders WHERE order_id = ?"
                 )) {
                ps.setObject(1, UUID.fromString(orderIdStr));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        found = true;
                        customerInDb = rs.getInt("customer_id");
                        statusInDb = rs.getString("order_status");
                    }
                }
            }
            if (!found) {
                System.out.println(">>> Order not in DB yet, waiting...");
                Thread.sleep(500);
            }
        }

        assertTrue(found, "Order should be persisted in DB but was not found");
        assertEquals(3, customerInDb, "Persisted order should have customer_id = 3");
        assertEquals("created", statusInDb, "Persisted order should have status 'created'");

        System.out.println(">>> DB check passed for order " + orderIdStr);
    }



}
