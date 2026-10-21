package labcast.common.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LanguageTest {

    @Test
    void khoaTaiLieu_pythonGiuNguyenMaBai() {
        assertEquals("dsa.cay.heap-them", Language.docKey("dsa.cay.heap-them", Language.PYTHON));
        assertEquals("dsa.cay.heap-them#cpp", Language.docKey("dsa.cay.heap-them", Language.CPP));
    }

    @Test
    void tachKhoaTaiLieu() {
        assertEquals("dsa.cay.heap-them", Language.lessonOf("dsa.cay.heap-them#java"));
        assertEquals("dsa.cay.heap-them", Language.lessonOf("dsa.cay.heap-them"));
        assertEquals(Language.JAVA, Language.ofDocKey("dsa.cay.heap-them#java"));
        assertEquals(Language.PYTHON, Language.ofDocKey("dsa.cay.heap-them"));
        assertNull(Language.ofDocKey("dsa.cay.heap-them#go"));
    }

    @Test
    void maRongLaPython_maLaTraNull() {
        assertEquals(Language.PYTHON, Language.of(null));
        assertEquals(Language.PYTHON, Language.of(""));
        assertEquals(Language.C, Language.of("C"));
        assertNull(Language.of("rust"));
    }

    @Test
    void duoiFileVaHeSoThoiGian() {
        assertEquals("cpp", Language.CPP.extension());
        assertEquals("java", Language.JAVA.extension());
        assertEquals(2, Language.JAVA.timeFactor());
        assertEquals(1, Language.C.timeFactor());
    }
}
