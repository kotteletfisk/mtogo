/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.supplier.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
/**
 *
 * @author kotteletfisk
 */
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
@JacksonXmlRootElement(localName = "Order")
public class LegacyOrder {

    @JacksonXmlProperty(localName = "Total")
    private float total;
    @JacksonXmlProperty(localName = "Phone")
    private String phone;    
    @JacksonXmlProperty(localName = "Supplier")
    private int supplier;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "OrderLine")
    private List<LegacyOrderLine> orderlines;
}
