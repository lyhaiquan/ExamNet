package labcast.client.player;

/**
 * Việc các nút của {@link PlayerPane} làm. Mặc định ({@link #LOCAL}) điều khiển trình phát trên máy
 * này. Khi chiếu: nút của giáo viên gửi lệnh cho server ({@code PresenterController}); nút của học viên
 * đang theo giáo viên thì tách ra trước rồi mới tự điều khiển.
 */
public interface PlayerControls {
    void play(Player p);

    void pause(Player p);

    void seek(Player p, int k);

    void speed(Player p, double x);

    void nextKey(Player p);

    PlayerControls LOCAL = new PlayerControls() {
        @Override
        public void play(Player p) {
            p.play();
        }

        @Override
        public void pause(Player p) {
            p.pause();
        }

        @Override
        public void seek(Player p, int k) {
            p.seek(k);
        }

        @Override
        public void speed(Player p, double x) {
            p.speed(x);
        }

        @Override
        public void nextKey(Player p) {
            p.nextKeyStep();
        }
    };
}
