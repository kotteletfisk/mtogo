package mtogo.customer.service;


import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.messaging.Producer;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SupplierService {

    private final BlockingQueue<List<SupplierDTO>> queue = new ArrayBlockingQueue<>(1);

    private static final SupplierService INSTANCE = new SupplierService();
    public static SupplierService getInstance() { return INSTANCE; }

    private SupplierService() {}

    public List<SupplierDTO> requestSuppliersBlocking(String zipCode) throws Exception {
        // Clear any leftover result
        queue.clear();

        // Send request to Redis Driver
        Producer.publishMessage("customer:supplier_request", zipCode);

        // Wait max 2 seconds for the response
        List<SupplierDTO> suppliers = queue.poll(2, TimeUnit.SECONDS);
        if (suppliers == null) {
            throw new TimeoutException("No response from Redis driver");
        }
        return suppliers;
    }

    public void completeSupplierRequest(List<SupplierDTO> suppliers) {
        queue.offer(suppliers);
    }
}
