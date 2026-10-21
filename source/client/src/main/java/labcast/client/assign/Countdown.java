package labcast.client.assign;

/**
 * Đồng hồ đếm ngược của bài đang làm, theo giờ server ({@code Clock.serverNow()}). Chủ: B.
 *
 * <p>Chỉ để hiện; hết giờ thật do server quyết định khi nộp.
 */
public interface Countdown {

    /** Số mili giây còn lại; 0 khi đã hết; âm khi bài không giới hạn thời gian. */
    long remainingMs();

    /** Gọi lại mỗi giây khi đang đếm, và một lần khi về 0. */
    void onTick(Runnable listener);
}
