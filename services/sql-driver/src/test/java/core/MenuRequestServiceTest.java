package core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mtogo.sql.core.CustomerMenuRequestService;
import mtogo.sql.model.DTO.menuItemDTO;
import mtogo.sql.ports.out.IModelRepository;

@ExtendWith(MockitoExtension.class)
public class MenuRequestServiceTest {

    @Mock
    IModelRepository repository;

    @Test
    void returnItemsTest() throws Exception {

        when(repository.getMenuItemsBySupplierId(anyInt())).thenReturn(List.of(
            new menuItemDTO(),
            new menuItemDTO()
        ));

        CustomerMenuRequestService service = new CustomerMenuRequestService(repository);
        List<menuItemDTO> result = service.call(1);

        assertEquals(2, result.size());
    }    
    
    @Test
    void returnEmptyListOnNullResult() throws Exception {

        when(repository.getMenuItemsBySupplierId(anyInt())).thenReturn(null);

        CustomerMenuRequestService service = new CustomerMenuRequestService(repository);
        List<menuItemDTO> result = service.call(1);

        assertEquals(0, result.size());
    }
}
