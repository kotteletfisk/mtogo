package mtogo.customer.controller;

import io.javalin.http.Context;
import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.service.MenuService;
import mtogo.customer.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

    @Mock
    Context ctx;


    @Test
    void getItemsBySupplierId_returns200AndItems() throws Exception {

        ProductController controller = ProductController.getInstance();

        when(ctx.pathParam("supplierId")).thenReturn("1");

        when(ctx.status(anyInt())).thenReturn(ctx);
        when(ctx.json(any())).thenReturn(ctx);

        // Fake menu items returned from MenuService
        List<menuItemDTO> fakeItems = List.of(
                new menuItemDTO(1, "Pizza", 85.0, 1, true),
                new menuItemDTO(2, "Burger", 60.0, 1, true)
        );

        try (MockedStatic<MenuService> menuServiceStatic = Mockito.mockStatic(MenuService.class)) {
            MenuService menuServiceMock = mock(MenuService.class);

            // When MenuService.getInstance() is called, return our mock
            menuServiceStatic.when(MenuService::getInstance).thenReturn(menuServiceMock);

            // When requestMenuBlocking(1) is called, return fakeItems
            when(menuServiceMock.requestMenuBlocking(1)).thenReturn(fakeItems);

            controller.getItemsBySupplierId(ctx);

            verify(menuServiceMock).requestMenuBlocking(1);
            verify(ctx).status(200);
            verify(ctx).json(fakeItems);
            verify(ctx, never()).result(anyString());
        }
    }

    @Test
    void getActiveSuppliers_returns200AndSuppliers() throws Exception {
        ProductController productController = ProductController.getInstance();
        when(ctx.pathParam("zipcode")).thenReturn("2200");
        when(ctx.status(anyInt())).thenReturn(ctx);
        when(ctx.json(any())).thenReturn(ctx);

        // Fake suppliers returned from SupplierService
        List<SupplierDTO> fakeSuppliers = List.of(
                new SupplierDTO(1, "Supplier A", "2200", SupplierDTO.status.active),
                new SupplierDTO(2, "Supplier B", "2200", SupplierDTO.status.active)
        );

        try (MockedStatic<mtogo.customer.service.SupplierService> supplierServiceStatic = Mockito.mockStatic(mtogo.customer.service.SupplierService.class)) {
            mtogo.customer.service.SupplierService supplierServiceMock = mock(mtogo.customer.service.SupplierService.class);

            // When SupplierService.getInstance() is called, return our mock
            supplierServiceStatic.when(SupplierService::getInstance).thenReturn(supplierServiceMock);

            // When requestSuppliersBlocking("2200") is called, return fakeSuppliers
            when(supplierServiceMock.requestSuppliersBlocking("2200")).thenReturn(fakeSuppliers);

            productController.getActiveSuppliers(ctx);

            verify(supplierServiceMock).requestSuppliersBlocking("2200");
            verify(ctx).status(200);
            verify(ctx).json(fakeSuppliers);
            verify(ctx, never()).result(anyString());
        }
    }


}
