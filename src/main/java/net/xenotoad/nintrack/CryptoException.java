package net.xenotoad.nintrack;

import java.security.GeneralSecurityException;

/**
 * Created by misson20000 on 2/25/17.
 */
public class CryptoException extends ExtractionException {
    public CryptoException(GeneralSecurityException e) {
        super(e);
    }
}
