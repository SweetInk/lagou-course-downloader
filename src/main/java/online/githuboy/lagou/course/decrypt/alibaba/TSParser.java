package online.githuboy.lagou.course.decrypt.alibaba;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TS文件解析器
 * //TODO 代码优化
 *
 * @author suchu
 * @date 2021/5/28
 */
public class TSParser {
    private final static byte SYNC_BYTE = 0x47;
    private final static int PACKET_LENGTH = 188;
    private final static int PAYLOAD_START_MASK = 0x40;
    private final static int ATF_MASK = 0x30;
    private final static int ATF_RESERVE = 0x00;
    private final static int ATF_PAYLOAD_ONLY = 0x01;
    private final static int ATF_FIELD_ONLY = 0x02;
    private final static int ATF_FILED_FOLLOW_PAYLOAD = 0x03;

    public void decrypt(TSStream stream, byte[] key) {
        stream.byteBuf.resetReaderIndex();
        final AES aesECB = new AES(Mode.ECB, Padding.NoPadding, key);
        decryptPES(stream.byteBuf, stream.videos, aesECB);
        decryptPES(stream.byteBuf, stream.audios, aesECB);
    }

    public void decryptPES(ByteBuf byteBuf, List<TSPesFragment> pesFragments, AES aesECB) {
        for (TSPesFragment pes : pesFragments) {
            ByteArrayBuffer buffer = new ByteArrayBuffer();
            pes.list.forEach(packet -> {
                byte[] payload = packet.payload;
                if (null == payload) {
                    throw new RuntimeException("payload is null");
                }
                buffer.append(payload);
            });
            int length = buffer.bytes.length;

            if (length % 16 > 0) {
                int newLength = 16 * (length / 16);
                int remainLength = length - newLength;
                byte[] encryptedBytes = buffer.subArray(0, newLength);
                byte[] decrypt = aesECB.decrypt(encryptedBytes);
                byte[] bytes = buffer.subArray(newLength, remainLength);
                buffer.reset();
                buffer.append(decrypt);
                buffer.append(bytes);
            } else {
                byte[] all = buffer.all();
                byte[] decrypt = aesECB.decrypt(all);
                buffer.reset();
                buffer.append(decrypt);
            }
            //Rewrite decrypted bytes to byteBuf
            int startOffset = 0;
            for (TSPacket packet : pes.list) {
                int payloadLength = packet.payloadLength;
                long payloadStartOffset = packet.payloadStartOffset;
                byte[] bytes = buffer.subArray(startOffset, payloadLength);
                startOffset += payloadLength;
                byteBuf.writerIndex((int) payloadStartOffset);
                byteBuf.writeBytes(bytes);
            }
        }
    }

    @SneakyThrows
    public TSStream fromRandomAccessFile(RandomAccessFile file) {
        int packageNo = 0;
        long length = file.length();
        if (length % PACKET_LENGTH != 0) throw new RuntimeException("not a ts package");
        long packNums = length / PACKET_LENGTH;
        TSStream stream = new TSStream();
        TSPesFragment pes = null;
        stream.file = file;
        while (packageNo < packNums) {
            byte[] buffer = new byte[PACKET_LENGTH];
            file.read(buffer);
            TSPacket packet = parseTSPacket(buffer, packageNo, packageNo * PACKET_LENGTH);
            switch (packet.header.pid) {
                //FIXME 从PMT表读取视频、音频的PID
                //video data
                case 0x100:
                    if (packet.header.isPayloadStart) {
                        if (null != pes) {
                            stream.videos.add(pes);
                        }
                        pes = new TSPesFragment();
                    }
                    if (null != pes)
                        pes.append(packet);
                    break;
                //audio data
                case 0x101:
                    if (packet.header.isPayloadStart) {
                        if (null != pes) {
                            stream.audios.add(pes);
                        }
                        pes = new TSPesFragment();
                    }
                    if (null != pes)
                        pes.append(packet);
                    break;
            }
            stream.addPacket(packet);
            packageNo++;
        }
        return stream;
    }

    @SneakyThrows
    public TSStream fromInputStream(InputStream inputStream) {
        TSStream stream = new TSStream();
        byte[] bytes = IoUtil.readBytes(inputStream, inputStream.available());
        stream.byteBuf = Unpooled.wrappedBuffer(bytes);
        ByteBuf byteBuf = stream.byteBuf;
        inputStream.close();
        int packageNo = 0;
        long length = stream.byteBuf.capacity();
        if (length % PACKET_LENGTH != 0) throw new RuntimeException("not a ts package");
        long packNums = length / PACKET_LENGTH;
        TSPesFragment pes = null;

        while (packageNo < packNums) {
            byte[] buffer = new byte[PACKET_LENGTH];
            byteBuf.readBytes(buffer);
            TSPacket packet = parseTSPacket(buffer, packageNo, packageNo * PACKET_LENGTH);
            switch (packet.header.pid) {
                //video data
                case 0x100:
                    if (packet.header.isPayloadStart) {
                        if (null != pes) {
                            stream.videos.add(pes);
                        }
                        pes = new TSPesFragment();
                    }
                    if (null != pes)
                        pes.append(packet);
                    break;
                //audio data
                case 0x101:
                    if (packet.header.isPayloadStart) {
                        if (null != pes) {
                            stream.audios.add(pes);
                        }
                        pes = new TSPesFragment();
                    }
                    if (null != pes)
                        pes.append(packet);
                    break;
            }
            stream.addPacket(packet);
            packageNo++;
        }
        return stream;
    }

    @SneakyThrows
    public TSStream fromByteArray(byte[] bytes) {
        TSStream stream = new TSStream();
        stream.byteBuf = Unpooled.wrappedBuffer(bytes);
        ByteBuf byteBuf = stream.byteBuf;
        int packageNo = 0;
        long length = stream.byteBuf.capacity();
        if (length % PACKET_LENGTH != 0) throw new RuntimeException("not a ts package");
        long packNums = length / PACKET_LENGTH;
        TSPesFragment pes = null;

        while (packageNo < packNums) {
            byte[] buffer = new byte[PACKET_LENGTH];
            byteBuf.readBytes(buffer);
            TSPacket packet = parseTSPacket(buffer, packageNo, packageNo * PACKET_LENGTH);
            switch (packet.header.pid) {
                //video data
                case 0x100:
                    if (packet.header.isPayloadStart) {
                        if (null != pes) {
                            stream.videos.add(pes);
                        }
                        pes = new TSPesFragment();
                    }
                    if (null != pes)
                        pes.append(packet);
                    break;
                //audio data
                case 0x101:
                    if (packet.header.isPayloadStart) {
                        if (null != pes) {
                            stream.audios.add(pes);
                        }
                        pes = new TSPesFragment();
                    }
                    if (null != pes)
                        pes.append(packet);
                    break;
            }
            stream.addPacket(packet);
            packageNo++;
        }
        return stream;
    }

    public TSPacket parseTSPacket(byte[] buffer, int packNo, long offset) {
        if (buffer[0] != SYNC_BYTE)
            throw new RuntimeException("Invalid ts package in :" + packNo + " offset:" + offset);
        TSHeader header = new TSHeader();
        header.syncByte = buffer[0];
        header.transportErrorIndicator = (byte) ((buffer[1] & 0x80) > 0 ? 1 : 0);
        header.payloadUnitStartIndicator = (byte) ((buffer[1] & PAYLOAD_START_MASK) > 0 ? 1 : 0);
        header.transportErrorIndicator = (byte) ((buffer[1] & 0x20) > 0 ? 1 : 0);
        header.pid = ((buffer[1] & 0x1F) << 8 | buffer[2] & 0xFF);
        header.transportScramblingControl = (byte) (((buffer[3] & 0xC0) >> 6) & 0xFF);
        header.adaptationFiled = (byte) (((buffer[3] & ATF_MASK) >> 4) & 0xFF);
        header.continuityCounter = (byte) ((buffer[3] & 0x0F) & 0xFF);
        header.hasError = header.transportErrorIndicator != 0;
        header.isPayloadStart = header.payloadUnitStartIndicator != 0;
        header.hasAdaptationFieldField = header.adaptationFiled == ATF_FIELD_ONLY || header.adaptationFiled == ATF_FILED_FOLLOW_PAYLOAD;
        header.hasPayload = header.adaptationFiled == ATF_PAYLOAD_ONLY || header.adaptationFiled == ATF_FILED_FOLLOW_PAYLOAD;
        TSPacket packet = new TSPacket();
        packet.header = header;
        packet.packNo = packNo;
        packet.startOffset = offset;
        if (header.hasAdaptationFieldField) {
            int atfLength = buffer[4] & 0xFF;
            packet.headerLength += 1;
            packet.atfLength = atfLength;
        }
        if (header.isPayloadStart) {
            packet.pesOffset = packet.startOffset + packet.headerLength + packet.atfLength;
            // 9 bytes : 6 bytes for PES header + 3 bytes for PES extension
            packet.pesHeaderLength = 6 + 3 + buffer[packet.headerLength + packet.atfLength + 8] & 0xFF;
        }
        packet.payloadRelativeOffset = packet.headerLength + packet.atfLength + packet.pesHeaderLength;
        packet.payloadStartOffset = packet.startOffset + packet.payloadRelativeOffset;
        packet.payloadLength = PACKET_LENGTH - packet.payloadRelativeOffset;
        if (packet.payloadLength > 0) {
            packet.payload = Arrays.copyOfRange(buffer, packet.payloadRelativeOffset, PACKET_LENGTH);
        }
        return packet;
    }

    /**
     * 子节数组动态扩容buffer
     */
    static class ByteArrayBuffer {
        byte[] bytes = new byte[0];

        public void append(byte[] data) {
            if (bytes.length < bytes.length + data.length) {
                byte[] temp = new byte[bytes.length + data.length];
                System.arraycopy(bytes, 0, temp, 0, bytes.length);
                System.arraycopy(data, 0, temp, bytes.length, data.length);
                bytes = temp;
            }
        }

        public void reset() {
            bytes = new byte[0];
        }

        public byte[] all() {
            return subArray(0, bytes.length);
        }

        public byte[] subArray(int offset, int length) {
            if (offset < 0 || offset > bytes.length)
                offset = 0;
            if (length > bytes.length) {
                throw new IllegalArgumentException("length exceed the actual length:" + bytes.length);
            }
            return Arrays.copyOfRange(bytes, offset, offset + length);
        }
    }

    private static class TSPesFragment {
        List<TSPacket> list = new ArrayList<>();

        public void append(TSPacket packet) {
            list.add(packet);
        }
    }

    public static class TSStream {
        private final List<TSPacket> packets = new ArrayList<>();
        private final List<TSPesFragment> videos = new ArrayList<>();
        private final List<TSPesFragment> audios = new ArrayList<>();
        private RandomAccessFile file;
        private ByteBuf byteBuf;

        public void addPacket(TSPacket packet) {
            packets.add(packet);
        }


        public void PIDList() {
            Set<String> collect = packets.stream().map(p -> p.header.pid).map(HexUtil::toHex).collect(Collectors.toSet());
            System.out.println(collect);
        }

        public void dumpToFile(String path) throws IOException {
            if (null != this.byteBuf) {
                this.byteBuf.resetReaderIndex();
                this.byteBuf.writerIndex(byteBuf.capacity());
                File file = new File(path);
                if (!file.exists())
                    file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                this.byteBuf.readBytes(fos, byteBuf.capacity());
                fos.flush();
                fos.close();
            }
        }

        public void dumpToFile(File file) throws IOException {
            if (null != this.byteBuf) {
                this.byteBuf.resetReaderIndex();
                this.byteBuf.writerIndex(byteBuf.capacity());

                if (!file.exists())
                    file.createNewFile();
                this.byteBuf.readBytes(new FileOutputStream(file), byteBuf.capacity());
            }
        }
    }

    private static class TSPacket {
        private TSHeader header;
        private int packNo;
        private long startOffset;
        private int headerLength = 4;
        private int atfLength;
        private long pesOffset;
        private int pesHeaderLength;
        private long payloadStartOffset;
        private int payloadRelativeOffset = 0;
        private int payloadLength = 0;
        private byte[] payload;
    }

    /**
     * 4byte
     */
    private static class TSHeader {
        private byte syncByte;//8
        private byte transportErrorIndicator;//1
        private byte payloadUnitStartIndicator;//1
        private byte transportPriority;//1
        private int pid;//13
        private byte transportScramblingControl;//2
        private byte adaptationFiled;//2
        private byte continuityCounter;//4

        private boolean hasError;
        private boolean isPayloadStart;
        private boolean hasAdaptationFieldField;
        private boolean hasPayload;
    }
}
