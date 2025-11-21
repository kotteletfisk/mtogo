
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import mtogo.supplier.DTO.LegacyOrder;
import mtogo.supplier.server.LegacyDBAdapter;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 *
 * @author kotteletfisk
 */
public class LegacyDBAdapterTest {

    @Test
    public void singleTonInstanceTest() {
        // Instance should be singleton to avoid port listening issues

        LegacyDBAdapter la = LegacyDBAdapter.getAdapter();

        assertNotNull(la);

        LegacyDBAdapter la2 = LegacyDBAdapter.getAdapter();

        assertEquals(la, la2);
    }

    @Test
    public void tcpListenerActiveTest() {
        // Adapter should start a working tcp listener
        try {
            LegacyDBAdapter la = LegacyDBAdapter.getAdapter().startListener(1984);

            assertEquals(1984, la.getServerSocket().getLocalPort());
            assertEquals(true, la.getServerSocket().isBound());

        } catch (IOException ex) {
            fail("Exception thrown: " + ex.getMessage());
        }

    }

    /*     @Test
    public void tcpListenerReceiveTest() {
        // Adapter should receive data on the listening port
        try {
            LegacyDBAdapter la = LegacyDBAdapter.getAdapter().startListener(1984);

            


        } catch (IOException ex) {
            fail("Exception thrown: " + ex.getMessage());
        }

    } */
    @Test
    public void mapXmlToLegacyOrder() {

        String xml = """
<Order><OrderLine><ItemId>1</ItemId><Amount>1</Amount><UnitPrice>1</UnitPrice><SubTotal>1</SubTotal></OrderLine><OrderLine><ItemId>2</ItemId><Amount>2</Amount><UnitPrice>2</UnitPrice><SubTotal>4</SubTotal></OrderLine><OrderLine><ItemId>3</ItemId><Amount>3</Amount><UnitPrice>3</UnitPrice><SubTotal>9</SubTotal></OrderLine><Total>14</Total><Phone>11111111</Phone></Order>
        """;
        XmlMapper mapper = new XmlMapper();
        LegacyOrder order = mapper.readValue(xml, LegacyOrder.class);

        assertEquals(order.getPhone(), "11111111");
    }
}
