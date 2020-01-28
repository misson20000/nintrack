package net.xenotoad.nintrack;

/**
 * Created by misson20000 on 2/25/17.
 */
public class ExtractionException extends Exception {
    public ExtractionException(Throwable t) {
        super(t);
    }

    public ExtractionException() {
        super();
    }

    public ExtractionException(String s) {
        super(s);
    }
}
