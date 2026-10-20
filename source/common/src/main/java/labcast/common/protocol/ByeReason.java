package labcast.common.protocol;

/** Lý do trong thông điệp {@code BYE(reason)} (spec §10.3). Gửi dưới dạng tên hằng. */
public enum ByeReason {
    LOGGED_IN_ELSEWHERE,
    SERVER_SHUTDOWN,
    CLIENT_EXIT,
    TOO_MANY_MESSAGES
}
