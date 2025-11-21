/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package mtogo.supplier.DTO;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

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

    public int getItemId() {
        return this.itemId;
    }

    public int getAmount() {
        return this.amount;
    }

    public float getUnitPrice() {
        return this.unitPrice;
    }

    public float getSubTotal() {
        return this.subTotal;
    }

    public void setItemId (int itemId) {
        this.itemId = itemId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setUnitPrice(float unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setSubTotal(float subTotal) {
        this.subTotal = subTotal;
    }

}
