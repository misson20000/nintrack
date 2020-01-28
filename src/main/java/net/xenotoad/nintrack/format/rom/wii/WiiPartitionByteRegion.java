package net.xenotoad.nintrack.format.rom.wii;

import net.xenotoad.nintrack.ByteRegion;
import net.xenotoad.nintrack.MemoryByteRegion;
import net.xenotoad.nintrack.SubdivisionTooLongException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by misson20000 on 2/25/17.
 */
public class WiiPartitionByteRegion extends ByteRegion {
    private ByteRegion backing;
    private WiiPartitionFileType type;
    private WiiTicket ticket;
    private long offset;
    private long length;

    public WiiPartitionByteRegion(ByteRegion backing, WiiTicket ticket, int type) {
        this(backing, ticket, type, 0, (backing.getLength() / 0x8000) * 0x7C00);
    }

    public WiiPartitionByteRegion(ByteRegion backing, WiiTicket ticket, int type, long offset, long length) {
        this.type = WiiPartitionFileType.values()[type];
        this.backing = backing;
        this.ticket = ticket;
        this.offset = offset;
        this.length = length;
    }

    public WiiPartitionFileType getType() {
        return type;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public ByteRegion subdivide(long offset, long length) {
        if(offset + length > this.length) {
            throw new SubdivisionTooLongException("Length exceeds size of region");
        }
        return new WiiPartitionByteRegion(backing, ticket, type.ordinal(), this.offset + offset, length);
    }

    @Override
    public MemoryByteRegion buffer() throws IOException {
        if(length > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Length is too long to read into buffer");
        }
        ByteBuffer buffer = ByteBuffer.allocate((int) length);
        read().read(buffer);
        return new MemoryByteRegion(buffer);
    }

    @Override
    public SeekableByteChannel read() throws IOException {
        return new DecryptChannel();
    }

    private class DecryptChannel implements SeekableByteChannel {
        private int currentClusterNo = -1;
        private ByteBuffer decryptedClusterBuffer = ByteBuffer.allocateDirect(0x7C00);
        private ByteBuffer encryptedClusterBuffer = ByteBuffer.allocateDirect(0x8000);
        private byte[] iv = new byte[0x10];
        private long position;
        private SeekableByteChannel channel = backing.read();
        private Cipher cipher;

        public DecryptChannel() throws IOException {
            try {
                cipher = Cipher.getInstance("AES/CBC/NoPadding");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new IOException(e);
            }
            position(0);
        }

        private synchronized void seekToCluster(int no) throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, ShortBufferException, IllegalBlockSizeException {
            if(currentClusterNo != no) {
                currentClusterNo = no;
                channel.position(no * 0x8000L);
                encryptedClusterBuffer.clear();
                while(encryptedClusterBuffer.remaining() > 0) {
                    channel.read(encryptedClusterBuffer);
                }
                encryptedClusterBuffer.position(0x3D0);
                encryptedClusterBuffer.limit(0x3E0);
                encryptedClusterBuffer.get(iv);
                encryptedClusterBuffer.clear();
                encryptedClusterBuffer.position(0x400);
                decryptedClusterBuffer.clear();
                cipher.init(Cipher.DECRYPT_MODE, ticket.titleKeyObject, new IvParameterSpec(iv));
                cipher.doFinal(encryptedClusterBuffer, decryptedClusterBuffer);
            }
        }

        private int clusterAt(long pos) {
            return (int) (pos / 0x7C00L);
        }

        private int availableInCluster() {
            return (int) (0x7C00L - (position % 0x7C00L));
        }

        @Override
        public synchronized int read(ByteBuffer dst) throws IOException {
            if(position() >= size()) {
                return -1;
            }
            int count = 0;
            while(dst.remaining() > 0 && position() < size()) {
                int readAmount = (int) Math.min(availableInCluster(), Math.min(dst.remaining(), size()-(position-offset)));
                decryptedClusterBuffer.position((int) (position % 0x7C00L));
                decryptedClusterBuffer.limit((int) (position % 0x7C00L) + readAmount);
                dst.put(decryptedClusterBuffer);
                position(position - offset + readAmount); // seek forward, potentially to next cluster
                count+= readAmount;
            }
            return count;
        }

        @Override
        public synchronized int write(ByteBuffer src) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized long position() throws IOException {
            return position - offset;
        }

        @Override
        public synchronized SeekableByteChannel position(long newPosition) throws IOException {
            try {
                seekToCluster(clusterAt(newPosition + offset));
            } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                throw new IOException(e);
            }
            position = newPosition + offset;
            return this;
        }

        @Override
        public long size() throws IOException {
            return length;
        }

        @Override
        public synchronized SeekableByteChannel truncate(long size) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public synchronized void close() throws IOException {

        }
    }
}
