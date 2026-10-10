package labcast.client.clock;

/**
 * Giờ của server nhìn từ app (spec §17.3). Mọi mốc hẹn giờ trong protocol (giờ phát animation, giờ mở
 * và đóng câu hỏi nhanh, thời gian chờ) đều theo đồng hồ này, không theo đồng hồ máy học viên.
 */
public interface Clock {
    long serverNow();
}
