package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import labcast.common.protocol.JsonPayload;
import org.junit.jupiter.api.Test;

class NoticeModelTest {
    private static JsonNode events() throws Exception {
        return JsonPayload.MAPPER.readTree(
                """
                [{"id":3,"action":"PASSWORD_FORGOT_UNKNOWN","actor":"10.0.0.5:5000","detail":"lam-van-a"},
                 {"id":2,"action":"PASSWORD_CHANGE","actor":"sv1","detail":"10.0.0.7:4000"},
                 {"id":1,"action":"AUTH_LOCKED","actor":"sv2","detail":"10.0.0.8:4001"}]
                """);
    }

    @Test
    void locTheoNhom() throws Exception {
        assertEquals(3, NoticeModel.filter(events(), NoticeModel.ALL).size());
        assertEquals(1, NoticeModel.filter(events(), NoticeModel.FORGOT).size());
        assertEquals(1, NoticeModel.filter(events(), NoticeModel.CHANGED).size());
        assertEquals(1, NoticeModel.filter(events(), NoticeModel.LOCKED).size());
    }

    @Test
    void tenLa_moTaGomTenDaGoVaIp() throws Exception {
        assertEquals("Tên \"lam-van-a\" từ 10.0.0.5:5000", NoticeModel.describe(events().get(0)));
    }

    @Test
    void hanhDongLa_giuNguyenTen() {
        assertEquals("X_Y", NoticeModel.label("X_Y"));
    }
}
