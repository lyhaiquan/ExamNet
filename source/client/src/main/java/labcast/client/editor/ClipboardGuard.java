package labcast.client.editor;

/**
 * Bộ nhớ tạm riêng của ô code khi học viên đang làm bài: copy/cắt trong ô code chỉ lưu ở đây (không
 * ra clipboard của Windows), dán chỉ lấy từ đây. Nhờ vậy học viên vẫn chuyển được đoạn code của chính
 * mình, nhưng không dán được code từ trình duyệt hay ChatGPT vào, cũng không copy code ra ngoài.
 */
public final class ClipboardGuard {
    private String buffer = "";

    /** Ghi đoạn vừa copy/cắt trong ô code. Chuỗi rỗng (không chọn gì) thì giữ đoạn cũ. */
    public void copied(String text) {
        if (text != null && !text.isEmpty()) {
            buffer = text;
        }
    }

    /** Đoạn được phép dán: chỉ những gì đã copy trong ô code. */
    public String pasteText() {
        return buffer;
    }

    /**
     * Học viên bấm dán trong khi clipboard của hệ thống đang chứa chữ lấy từ ngoài ô code — báo cho
     * học viên biết vì sao không dán được.
     */
    public boolean isExternal(String systemClipboard) {
        return systemClipboard != null && !systemClipboard.isEmpty() && !systemClipboard.equals(buffer);
    }
}
