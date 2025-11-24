/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package mtogo.supplier.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@Getter @Setter @ToString @AllArgsConstructor
@JacksonXmlRootElement(localName = "OrderLine")
public class LegacyOrderLine {
    @JacksonXmlProperty(localName = "ItemId")
    private int itemId;
    @JacksonXmlProperty(localName = "Amount")
    private int amount;
    @JacksonXmlProperty(localName = "UnitPrice")
    private float unitPrice;
    @JacksonXmlProperty(localName = "SubTotal")
    private float subTotal;
}
