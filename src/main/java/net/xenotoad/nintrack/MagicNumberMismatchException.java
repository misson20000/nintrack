package net.xenotoad.nintrack;

/**
 * Created by misson20000 on 2/25/17.
 */
public class MagicNumberMismatchException extends CorruptedFileException {
    public MagicNumberMismatchException(byte[] expected, byte[] actual) {
        super();
    }

    public MagicNumberMismatchException(int expected, int actual) {
        super();
    }
}
