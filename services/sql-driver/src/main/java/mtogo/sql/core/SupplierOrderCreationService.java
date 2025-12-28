/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.model.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.ports.out.IModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class SupplierOrderCreationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final IModelRepository repo;

    public SupplierOrderCreationService(IModelRepository repo) {
        this.repo = repo;
    }

    public OrderDetailsDTO call(LegacyOrderDetailsDTO legacyDto) throws Exception {
        log.debug("Received:\n" + legacyDto.toString());

        return repo.customerEnrichLegacyOrder(legacyDto);
    }
}
