package net.xenotoad.nintrack;

import java.io.IOException;

/**
 * Created by misson20000 on 2/24/17.
 */
public interface FileIdentifier {
    FileType identify(ByteRegion region) throws IOException;
}
