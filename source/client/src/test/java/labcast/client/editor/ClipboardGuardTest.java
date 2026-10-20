package labcast.client.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClipboardGuardTest {

    @Test
    void chuaCopyGi_danRaChuoiRong() {
        assertEquals("", new ClipboardGuard().pasteText());
    }

    @Test
    void copyTrongOCode_danLaiDungDoanDo() {
        var g = new ClipboardGuard();
        g.copied("a[j], a[j + 1]");
        assertEquals("a[j], a[j + 1]", g.pasteText());
    }

    @Test
    void copyKhongChonGi_giuDoanCu() {
        var g = new ClipboardGuard();
        g.copied("x = 1");
        g.copied("");
        g.copied(null);
        assertEquals("x = 1", g.pasteText());
    }

    @Test
    void clipboardHeThongCoChuTuNgoai_laDanTuNgoai() {
        var g = new ClipboardGuard();
        assertTrue(g.isExternal("print(sorted(a))"));
        g.copied("print(sorted(a))");
        assertFalse(g.isExternal("print(sorted(a))"));
        assertFalse(g.isExternal(""));
        assertFalse(g.isExternal(null));
    }
}
