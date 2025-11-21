/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.supplier.DTO;

import java.util.List;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author kotteletfisk
 */
@JacksonXmlRootElement(localName = "Order")
public class LegacyOrder {

    @JacksonXmlProperty(localName = "Total")
    private float total;
    @JacksonXmlProperty(localName = "Phone")
    private String phone;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "OrderLine")
    private List<LegacyOrderLine> orderlines;

    public float getTotal() {
        return this.total;
    }

    public String getPhone() {
        return this.phone;
    }

    public List<LegacyOrderLine> getOrderlines() {
        return this.orderlines;
    }

    public void setTotal(float total) {
        this.total = total;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setOrderlines(List<LegacyOrderLine> orderlines) {
        this.orderlines = orderlines;
    }

}
