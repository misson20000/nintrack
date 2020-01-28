package net.xenotoad.nintrack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;

/**
 * Created by misson20000 on 2/24/17.
 */
public class FileByteRegion extends ByteRegion {
    private final File file;
    private final long offset;

    @Override
    public long getLength() {
        return length;
    }

    private final long length;

    public FileByteRegion(File f) {
        this(f, 0L, f.length());
    }

    public FileByteRegion(File file, long offset, long length) {
        this.file = file;
        this.offset = offset;
        this.length = length;
    }

    public FileByteRegion subdivide(long offset, long length) {
        if(offset + length > this.length) {
            throw new IndexOutOfBoundsException("Length exceeds size of region");
        }
        return new FileByteRegion(file, this.offset + offset, length);
    }

    @Override
    public MemoryByteRegion buffer() throws IOException {
        if(length > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Length is too long to read into buffer");
        }
        ByteBuffer buffer = ByteBuffer.allocate((int) length);
        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        channel.position(offset);
        channel.read(buffer);
        return new MemoryByteRegion(buffer);
    }

    @Override
    public SeekableByteChannel read() throws IOException {
        FileChannel backing = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        ByteBuffer transferBuffer = ByteBuffer.allocateDirect(65536);
        backing.position(offset);

        return new SeekableByteChannel() {
            @Override
            public int read(ByteBuffer dst) throws IOException {
                long remainingLength = Math.min(dst.remaining(), length - (backing.position() - offset));
                if(remainingLength <= 0) {
                    return -1;
                }
                long readLength = 0;
                while(remainingLength > 0) {
                    transferBuffer.clear();
                    long toRead = Math.min(transferBuffer.remaining(), remainingLength);
                    transferBuffer.limit((int) toRead);
                    int read = backing.read(transferBuffer);
                    if(read < 0) {
                        break;
                    }
                    readLength+= read;
                    remainingLength-= read;
                    transferBuffer.flip();
                    dst.put(transferBuffer);
                }
                return (int) readLength;
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public long position() throws IOException {
                return backing.position() - offset;
            }

            @Override
            public SeekableByteChannel position(long newPosition) throws IOException {
                backing.position(newPosition + offset);
                return this;
            }

            @Override
            public long size() throws IOException {
                return length;
            }

            @Override
            public SeekableByteChannel truncate(long size) throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public boolean isOpen() {
                return backing.isOpen();
            }

            @Override
            public void close() throws IOException {
                backing.close();
            }
        };
    }
}
