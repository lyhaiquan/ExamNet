package labcast.client.app;

/**
 * Lớp chính của jar. KHÔNG kế thừa {@code Application}: jar gộp có lớp chính kế thừa Application thì
 * Java báo "JavaFX runtime components are missing" — xem docs/plans/giai-doan-0.md task 0.4.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        LabCastApp.main(args);
    }
}
