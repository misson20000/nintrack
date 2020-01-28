package net.xenotoad.nintrack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by misson20000 on 2/24/17.
 */
public class MemoryByteRegion extends ByteRegion {
    private final ByteBuffer backing;

    public MemoryByteRegion(ByteBuffer backing) {
        this.backing = backing;
    }

    public MemoryByteRegion(ByteBuffer backing, long offset, long length) {
        ByteBuffer dup = backing.duplicate();
        dup.clear();
        dup.position((int) offset);
        dup.limit((int) (offset + length));
        this.backing = dup.slice();
    }

    @Override
    public long getLength() {
        return backing.capacity();
    }

    @Override
    public MemoryByteRegion subdivide(long offset, long length) {
        if(offset + length > this.backing.capacity()) {
            throw new IndexOutOfBoundsException("Length exceeds size of region");
        }
        return new MemoryByteRegion(backing, offset, length);
    }

    @Override
    public MemoryByteRegion buffer() throws IOException {
        return this;
    }

    @Override
    public SeekableByteChannel read() throws IOException {
        ByteBuffer independentBuffer = backing.duplicate(); // concurrency
        independentBuffer.clear();
        return new SeekableByteChannel() {
            @Override
            public int read(ByteBuffer dst) throws IOException {
                if(independentBuffer.remaining() <= 0) {
                    return -1; // EOF
                }
                int transferSize = Math.min(dst.remaining(), independentBuffer.remaining());
                independentBuffer.limit(independentBuffer.position() + transferSize);
                dst.put(independentBuffer);
                independentBuffer.limit(independentBuffer.capacity());
                return transferSize;
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public long position() throws IOException {
                return independentBuffer.position();
            }

            @Override
            public SeekableByteChannel position(long newPosition) throws IOException {
                independentBuffer.position((int) newPosition);
                return this;
            }

            @Override
            public long size() throws IOException {
                return independentBuffer.capacity();
            }

            @Override
            public SeekableByteChannel truncate(long size) throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {

            }
        };
    }
}
