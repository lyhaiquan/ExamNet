package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ZoomPaneTest {
    @Test
    void steps_through_levels_and_stops_at_the_ends() {
        assertEquals(1.1, ZoomPane.step(1.0, +1));
        assertEquals(0.85, ZoomPane.step(1.0, -1));
        assertEquals(2.0, ZoomPane.step(2.0, +1));
        assertEquals(0.75, ZoomPane.step(0.75, -1));
    }

    @Test
    void a_value_between_levels_moves_to_the_next_level_in_that_direction() {
        assertEquals(1.25, ZoomPane.step(1.2, +1));
        assertEquals(1.1, ZoomPane.step(1.2, -1));
    }

    @Test
    void zooming_in_stops_where_the_lesson_screen_still_fits() {
        // 1366×768: chiều cao giới hạn, 768 / 520 ≈ 1,48
        assertEquals(768 / ZoomPane.MIN_LAYOUT_H, ZoomPane.effective(2.0, 1366, 768), 1e-9);
        assertEquals(1.25, ZoomPane.effective(1.25, 1366, 768));
        // cửa sổ nhỏ hơn khổ tối thiểu: không tự thu nhỏ dưới 100%
        assertEquals(1.0, ZoomPane.effective(1.6, 700, 500));
        // người dùng tự thu nhỏ thì giữ nguyên
        assertEquals(0.85, ZoomPane.effective(0.85, 700, 500));
        // chưa có kích thước (chưa hiện cửa sổ)
        assertEquals(1.6, ZoomPane.effective(1.6, 0, 0));
    }

    @Test
    void zoom_out_from_a_capped_value_goes_to_the_level_below_it() {
        assertEquals(1.4, ZoomPane.step(ZoomPane.effective(2.0, 1366, 768), -1));
    }

    @Test
    void clamp_keeps_saved_value_in_range_and_resets_broken_ones() {
        assertEquals(1.25, ZoomPane.clamp(1.25));
        assertEquals(2.0, ZoomPane.clamp(5));
        assertEquals(0.75, ZoomPane.clamp(0.1));
        assertEquals(1.0, ZoomPane.clamp(Double.NaN));
        assertEquals(1.0, ZoomPane.clamp(-3));
    }
}
