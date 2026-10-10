package labcast.server;

/**
 * Một mảng nghiệp vụ cắm vào server. Mỗi người viết một lớp {@code XxxModule} trong package của
 * mình, đăng ký handler trong {@link #install}, rồi thêm <b>một dòng</b> vào {@link
 * LabCastServer#modules()}.
 */
public interface ServerModule {
    void install(ServerContext ctx);
}
