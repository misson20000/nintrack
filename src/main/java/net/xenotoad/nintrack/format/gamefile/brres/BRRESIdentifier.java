package net.xenotoad.nintrack.format.gamefile.brres;

import net.xenotoad.nintrack.MagicNumberIdentifier;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESIdentifier extends MagicNumberIdentifier {
    public static final byte[] magic = new byte[] { 0x62, 0x72, 0x65, 0x73, (byte) 0xFE, (byte) 0xFF };

    public BRRESIdentifier() {
        super(magic, BRRESFileType.instance);
    }
}
