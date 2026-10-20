package labcast.server.presence;

public enum PresenceState {
    ALIVE,
    /** Lỡ vài heartbeat: có thể mạng chập chờn, sơ đồ lớp tô xám nhạt. */
    SUSPECT,
    DISCONNECTED
}
