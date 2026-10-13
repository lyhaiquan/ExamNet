package labcast.client.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalNicTest {

    @Test
    void noiQuaLoopback_ngheMoiCard() throws Exception {
        List<NetworkInterface> all = LocalNic.forLocalAddress(InetAddress.getLoopbackAddress());
        assertFalse(all.isEmpty());
        assertEquals(LocalNic.allIpv4(), all);
    }

    @Test
    void noiQuaCardThat_chiNgheDungCardDo() throws Exception {
        NetworkInterface real = null;
        InetAddress addr = null;
        for (NetworkInterface n : LocalNic.allIpv4()) {
            if (!n.isLoopback()) {
                for (InetAddress a : Collections.list(n.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        real = n;
                        addr = a;
                    }
                }
            }
        }
        assumeTrue(real != null, "máy không có card mạng nào ngoài loopback");
        assertEquals(List.of(real), LocalNic.forLocalAddress(addr));
    }
}
