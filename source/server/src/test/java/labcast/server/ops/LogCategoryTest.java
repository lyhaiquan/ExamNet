package labcast.server.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogCategoryTest {
    @Test
    void logXepTheoPackage_lopMoiTrongPackageCuTuVaoDungLoai() {
        assertEquals(LogCategory.KET_NOI, LogCategory.ofLogger("labcast.server.net.TcpServer"));
        assertEquals(LogCategory.KET_NOI, LogCategory.ofLogger("labcast.server.session.SessionManager"));
        assertEquals(LogCategory.DANG_NHAP, LogCategory.ofLogger("labcast.server.account.AccountModule"));
        assertEquals(LogCategory.BAI_HOC, LogCategory.ofLogger("labcast.server.lesson.LessonPreparer"));
        assertEquals(LogCategory.CHAM_BAI, LogCategory.ofLogger("labcast.server.grade.Grader"));
        assertEquals(LogCategory.CHAM_BAI, LogCategory.ofLogger("labcast.server.sqlviz.LopChuaCo"));
        assertEquals(LogCategory.CHIEU, LogCategory.ofLogger("labcast.server.cast.CastSender"));
        assertEquals(LogCategory.CODE, LogCategory.ofLogger("labcast.server.journal.CodeStore"));
    }

    @Test
    void loggerNgoaiLabCastHoacLa_laHeThong() {
        assertEquals(LogCategory.HE_THONG, LogCategory.ofLogger("labcast.server.ServerMain"));
        assertEquals(LogCategory.HE_THONG, LogCategory.ofLogger("labcast.server.ops.Dashboard"));
        assertEquals(LogCategory.HE_THONG, LogCategory.ofLogger("java.net.http.HttpClient"));
        assertEquals(LogCategory.HE_THONG, LogCategory.ofLogger(""));
        assertEquals(LogCategory.HE_THONG, LogCategory.ofLogger(null));
    }

    @Test
    void suKienAudit_xepTheoViec() {
        assertEquals(LogCategory.DANG_NHAP, LogCategory.ofAction("AUTH_FAIL"));
        assertEquals(LogCategory.GIAN_LAN, LogCategory.ofAction("APP_LEAVE"));
        assertEquals(LogCategory.GIAN_LAN, LogCategory.ofAction("PASTE_FLAG"));
        assertEquals(LogCategory.KET_NOI, LogCategory.ofAction("BAD_FRAME"));
        assertEquals(LogCategory.CHIEU, LogCategory.ofAction("CAST_VIA_TCP"));
        assertEquals(LogCategory.QUAN_TRI, LogCategory.ofAction("ADMIN_ROSTER_IMPORT"));
        assertEquals(LogCategory.HE_THONG, LogCategory.ofAction("VIEC_LA"));
        assertEquals(LogCategory.HE_THONG, LogCategory.ofAction(null));
    }
}
