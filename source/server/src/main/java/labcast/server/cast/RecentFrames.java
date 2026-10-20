package labcast.server.cast;

import java.util.Optional;

/**
 * Vòng đệm các datagram chiếu gần nhất, theo SEQ, để sửa gói mất ({@code CAST_NACK} → {@code
 * CAST_REPAIR}). Mỗi ô giữ cả SEQ: sau khi vòng quay hết một lượt, hỏi SEQ cũ không trả nhầm gói mới
 * nằm cùng ô.
 */
final class RecentFrames {
    private final int[] seqs;
    private final byte[][] datagrams;

    RecentFrames(int capacity) {
        this.seqs = new int[capacity];
        this.datagrams = new byte[capacity][];
    }

    synchronized void put(int seq, byte[] datagram) {
        int i = Math.floorMod(seq, seqs.length);
        seqs[i] = seq;
        datagrams[i] = datagram;
    }

    synchronized Optional<byte[]> get(int seq) {
        if (seq <= 0) {
            return Optional.empty();
        }
        int i = Math.floorMod(seq, seqs.length);
        return seqs[i] == seq ? Optional.ofNullable(datagrams[i]) : Optional.empty();
    }
}
