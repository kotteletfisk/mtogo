package mtogo.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.service.MenuService;

import java.util.List;


public class ProductController {
    private static final ObjectMapper objectMapper = new ObjectMapper();


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

            List<menuItemDTO> items = MenuService.getInstance().requestMenuBlocking(supplierIdInt);
            ctx.status(200).json(items);

        }
        catch (NumberFormatException e) {
            ctx.status(400).result("Invalid supplierId: " + supplierId);
        } catch (java.util.concurrent.TimeoutException e) {
            ctx.status(504).result("Timed out waiting for menu items");
        } catch (Exception e) {
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
            List<SupplierDTO> suppliers = mtogo.customer.service.SupplierService.getInstance().requestSuppliersBlocking(zipcode);
            ctx.status(200).json(suppliers);
        } catch (Exception e) {
            ctx.status(500).result("Internal Server Error: " + e.getMessage());
        }
    }
}
