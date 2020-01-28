package net.xenotoad.nintrack;

import java.io.IOException;

/**
 * Created by misson20000 on 3/12/17.
 */
public class MultipleFileIdentifier implements FileIdentifier {
    private final FileIdentifier[] identifiers;

    public MultipleFileIdentifier(FileIdentifier[] identifiers) {
        this.identifiers = identifiers;
    }

    @Override
    public FileType identify(ByteRegion region) throws IOException {
        for(FileIdentifier fi : identifiers) {
            FileType ft = fi.identify(region);
            if(ft != null) {
                return ft;
            }
        }
        return UnknownFileType.instance;
    }
}
