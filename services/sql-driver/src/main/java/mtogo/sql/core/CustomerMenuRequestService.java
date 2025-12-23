/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.DTO.menuItemDTO;
import mtogo.sql.ports.out.IModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class CustomerMenuRequestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final IModelRepository repo;

    public CustomerMenuRequestService(IModelRepository repo) {
        this.repo = repo;
    }

    public List<menuItemDTO> call (int supplierId) throws Exception {
        
            List<menuItemDTO> items = repo.getMenuItemsBySupplierId(supplierId);

            if (items == null) {
                items = java.util.Collections.emptyList();
            }

            log.debug("Found {} items for Supplier ID: {}", items.size(), supplierId);
            return items;
    }
}
