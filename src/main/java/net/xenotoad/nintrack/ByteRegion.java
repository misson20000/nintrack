package net.xenotoad.nintrack;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by misson20000 on 2/24/17.
 */
public abstract class ByteRegion {
    public abstract long getLength();
    public abstract ByteRegion subdivide(long offset, long length);
    public abstract MemoryByteRegion buffer() throws IOException;
    public abstract SeekableByteChannel read() throws IOException;
}
