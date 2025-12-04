package mtogo.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ProductController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private static ProductController instance;

    public static ProductController getInstance() {
        if (instance == null) {
            instance = new ProductController();
        }
        return instance;
    }
    private ProductController() {}


    /**
     * Retrieves menu items for a given supplier ID. Calls the MenuService to fetch the items.
     * @param ctx the Javalin context
     */
    public void getItemsBySupplierId(Context ctx){
        String supplierId = ctx.pathParam("supplierId");
        try
        {
            int supplierIdInt = Integer.parseInt(supplierId);

            log.debug("Requesting menu items for supplier: {}", supplierIdInt);
            List<menuItemDTO> items = MenuService.getInstance().requestMenuBlocking(supplierIdInt);
            log.info("Retrieved {} menu items for supplier {}", items.size(), supplierIdInt);

            ctx.status(200).json(items);

        }
        catch (NumberFormatException e) {
            log.warn("Invalid supplierId format: {}", supplierId);
            ctx.status(400).result("Invalid supplierId: " + supplierId);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Timeout waiting for menu items for supplier: {}", supplierId, e);
            ctx.status(504).result("Timed out waiting for menu items");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for menu items for supplier: {}", supplierId, e);
            Thread.currentThread().interrupt();
            ctx.status(500).result("Request was interrupted");
        } catch (Exception e) {
            log.error("Failed to retrieve menu items for supplier: {}", supplierId, e);
            ctx.status(500).result("Failed to retrieve menu items");
        }
    }

    /**
     * Gets a list of active suppliers by zipcode.
     * @param ctx the Javalin context.
     */
    public void getActiveSuppliers(Context ctx) {
        String zipcode = ctx.pathParam("zipcode");
        try {
            log.debug("Requesting suppliers for zipcode: {}", zipcode);
            List<SupplierDTO> suppliers = mtogo.customer.service.SupplierService.getInstance().requestSuppliersBlocking(zipcode);
            log.info("Retrieved {} suppliers for zipcode {}", suppliers.size(), zipcode);

            ctx.status(200).json(suppliers);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Timeout waiting for suppliers for zipcode: {}", zipcode, e);
            ctx.status(504).result("Timed out waiting for supplier list");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for suppliers for zipcode: {}", zipcode, e);
            Thread.currentThread().interrupt();
            ctx.status(500).result("Request was interrupted");
        } catch (Exception e) {
            log.error("Failed to retrieve suppliers for zipcode: {}", zipcode, e);
            ctx.status(500).result("Internal Server Error: " + e.getMessage());
        }
    }
}