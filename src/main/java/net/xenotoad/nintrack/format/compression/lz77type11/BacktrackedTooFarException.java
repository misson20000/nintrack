package net.xenotoad.nintrack.format.compression.lz77type11;

import net.xenotoad.nintrack.ExtractionException;

/**
 * Created by misson20000 on 3/1/17.
 */
public class BacktrackedTooFarException extends ExtractionException {
    public BacktrackedTooFarException(int disp, int position) {
        super("Displacement: 0x" + Integer.toHexString(disp) + ", position: 0x" + Integer.toHexString(position));
    }
}
