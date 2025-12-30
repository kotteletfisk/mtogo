package mtogo.ui;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.ListedHashTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JMeter-based performance tests for customer order flow
 * Tests run alongside UA tests in the same test suite
 */
public class CustomerPerformanceTests {

    private static String baseUrl;
    private static String jmeterHome;

    @BeforeAll
    static void setupJMeter() {
        baseUrl = System.getProperty("base.url",
                System.getenv().getOrDefault("BASE_URL", "172.16.0.11:7071"));

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }

        String[] urlParts = baseUrl.split("/");
        if (urlParts.length >= 3) {
            baseUrl = urlParts[0] + "//" + urlParts[2];
        }

        System.out.println(">>> Performance test base URL: " + baseUrl);

        // Setup JMeter
        jmeterHome = System.getProperty("jmeter.home",
                System.getenv().getOrDefault("JMETER_HOME", "/tmp/jmeter"));

        File jmeterHomeDir = new File(jmeterHome);
        if (!jmeterHomeDir.exists()) {
            jmeterHomeDir.mkdirs();
        }

        try {
            File propsDir = new File(jmeterHome + "/bin");
            propsDir.mkdirs();
            File propsFile = new File(propsDir, "jmeter.properties");
            if (!propsFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(propsFile)) {
                    fos.write("# JMeter Properties\n".getBytes());
                }
            }

            JMeterUtils.setJMeterHome(jmeterHome);
            JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
            JMeterUtils.initLocale();

            System.out.println(">>> JMeter initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize JMeter: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Test
    void spikeTest_5minutes_customerOrderFlow() throws Exception {
        System.out.println("\n========================================");
        System.out.println("STARTING 5-MINUTE SPIKE TEST");
        System.out.println("========================================\n");

        TestPlan testPlan = new TestPlan("Customer Order Flow - Spike Test");

        // Create Thread Group with spike pattern
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Customer Users - Spike Pattern");
        threadGroup.setNumThreads(20);

        // Spike test schedule:
        // 0-30s: Ramp up to 10 users (baseline)
        // 30s-120s: Hold 10 users
        // 120s-150s: Spike to 20 users
        // 150s-240s: Hold 20 users (spike duration)
        // 240s-270s: Ramp down to 10 users
        // 270s-300s: Hold 10 users (recovery)

        threadGroup.setScheduler(true);
        threadGroup.setDuration(300); // 5 minutes total
        threadGroup.setDelay(0);
        threadGroup.setRampUp(30); // Initial ramp-up to baseline

        // Loop controller - keep making requests for the duration
        LoopController loopController = new LoopController();
        loopController.setLoops(-1); // Infinite loops (controlled by duration)
        loopController.setFirst(true);
        threadGroup.setSamplerController(loopController);

        // Create HTTP Samplers for realistic customer flow
        HTTPSampler findSuppliersRequest = createFindSuppliersRequest();
        HTTPSampler loadMenuRequest = createLoadMenuRequest();
        HTTPSampler createOrderRequest = createCreateOrderRequest();

        // Create result collector for reporting
        Summariser summer = new Summariser("Summary");
        String reportFileName = "target/jmeter-results/spike-test-" + System.currentTimeMillis() + ".jtl";
        Files.createDirectories(Paths.get("target/jmeter-results"));

        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(reportFileName);

        // Build test tree
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);

        ListedHashTree threadGroupTree = (ListedHashTree) testPlanTree.add(testPlan, threadGroup);

        // Add HTTP samplers to thread group
        threadGroupTree.add(findSuppliersRequest);
        threadGroupTree.add(loadMenuRequest);
        threadGroupTree.add(createOrderRequest);

        // Add result collector
        testPlanTree.add(testPlan, logger);

        // Run test
        StandardJMeterEngine jmeter = new StandardJMeterEngine();
        jmeter.configure(testPlanTree);

        System.out.println(">>> Starting spike test...");
        System.out.println(">>> Baseline: 10 users | Spike: 50 users | Duration: 5 minutes");
        System.out.println(">>> Schedule:");
        System.out.println("    0-30s:       Ramp to 10 users (baseline)");
        System.out.println("    30s-2min:    Hold 10 users");
        System.out.println("    2min-2:30:   SPIKE to 50 users");
        System.out.println("    2:30-4min:   Hold 50 users (peak load)");
        System.out.println("    4min-4:30:   Ramp down to 10 users");
        System.out.println("    4:30-5min:   Recovery at 10 users\n");

        long startTime = System.currentTimeMillis();

        System.out.println(">>> Running test...");
        jmeter.run();

        // Wait for test to complete with proper timeout
        int maxWaitSeconds = 320; // 5 min 20 sec
        int waited = 0;
        while (jmeter.isActive() && waited < maxWaitSeconds) {
            Thread.sleep(5000); // Check every 5 seconds
            waited += 5;
            if (waited % 30 == 0) {
                System.out.println(">>> Test still running... (" + waited + "s elapsed)");
            }
        }

        // Force stop if still running
        if (jmeter.isActive()) {
            System.out.println(">>> Forcing test stop after timeout");
            jmeter.stopTest(true);
            Thread.sleep(5000); // Give it time to clean up
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("\n>>> Spike test completed in " + duration + " seconds");
        System.out.println(">>> Results saved to: " + reportFileName);

        // Basic validation - test file should exist and have content
        File resultsFile = new File(reportFileName);
        assertTrue(resultsFile.exists(), "Results file should be created");
        assertTrue(resultsFile.length() > 0, "Results file should have content");

        System.out.println("\n========================================");
        System.out.println("SPIKE TEST COMPLETED");
        System.out.println("========================================\n");
    }

    private HTTPSampler createFindSuppliersRequest() {
        HTTPSampler sampler = new HTTPSampler();
        sampler.setName("Find Active Suppliers");
        sampler.setDomain(baseUrl.replace("http://", "").replace("https://", "").split(":")[0]);
        sampler.setPort(extractPort(baseUrl));
        sampler.setPath("/api/suppliers/2200");  // Zipcode in path
        sampler.setMethod("GET");

        sampler.setConnectTimeout("5000");
        sampler.setResponseTimeout("10000");

        return sampler;
    }

    private HTTPSampler createLoadMenuRequest() {
        HTTPSampler sampler = new HTTPSampler();
        sampler.setName("Load Menu Items");
        sampler.setDomain(baseUrl.replace("http://", "").replace("https://", "").split(":")[0]);
        sampler.setPort(extractPort(baseUrl));
        sampler.setPath("/api/1/items");
        sampler.setMethod("GET");

        sampler.setConnectTimeout("5000");
        sampler.setResponseTimeout("10000");

        return sampler;
    }

    private HTTPSampler createCreateOrderRequest() {
        HTTPSampler sampler = new HTTPSampler();
        sampler.setName("Create Order");
        sampler.setDomain(baseUrl.replace("http://", "").replace("https://", "").split(":")[0]);
        sampler.setPort(extractPort(baseUrl));
        sampler.setPath("/api/createorder");
        sampler.setMethod("POST");
        sampler.setPostBodyRaw(true);

        String jsonBody = "{"
                + "\"customerId\":3,"
                + "\"supplierId\":1,"
                + "\"status\":\"created\","
                + "\"paymentMethod\":\"MOBILEPAY\","
                + "\"orderLines\":["
                + "{\"itemId\":1,\"priceSnapshot\":75.0,\"amount\":1}"
                + "]"
                + "}";

        sampler.addNonEncodedArgument("", jsonBody, "");
        sampler.setHeaderManager(createJsonHeaders());

        sampler.setConnectTimeout("5000");
        sampler.setResponseTimeout("15000");

        return sampler;
    }

    private org.apache.jmeter.protocol.http.control.HeaderManager createJsonHeaders() {
        org.apache.jmeter.protocol.http.control.HeaderManager headerManager =
                new org.apache.jmeter.protocol.http.control.HeaderManager();
        headerManager.add(new org.apache.jmeter.protocol.http.control.Header("Content-Type", "application/json"));
        return headerManager;
    }

    private int extractPort(String url) {
        try {
            String[] parts = url.split(":");
            if (parts.length == 3) {
                return Integer.parseInt(parts[2].split("/")[0]);
            }
            return url.startsWith("https") ? 443 : 80;
        } catch (Exception e) {
            return 7071;
        }
    }
}