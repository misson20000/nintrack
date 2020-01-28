package net.xenotoad.nintrack.format.rom.wii;

import net.xenotoad.nintrack.CryptoException;
import net.xenotoad.nintrack.ExtractionException;
import net.xenotoad.nintrack.MagicNumberMismatchException;
import net.xenotoad.nintrack.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Created by misson20000 on 2/25/17.
 */
public class WiiTicket {
    private static final byte[] commonKey = Util.hexStringToByteArray("ebe42a225e8593e448d9c5457381aaf7");
    public final byte[] titleId;
    public final byte[] titleKey;
    public final SecretKeySpec titleKeyObject;

    public WiiTicket(byte[] titleId, byte[] titleKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.titleId = titleId;
        this.titleKey = titleKey;
        this.titleKeyObject = new SecretKeySpec(titleKey, "AES");
    }

    public static WiiTicket read(ByteBuffer buffer) throws ExtractionException {
        buffer.order(ByteOrder.BIG_ENDIAN);
        int signatureType = buffer.getInt();
        if(signatureType != 0x10001) {
            throw new MagicNumberMismatchException(signatureType, 0x10001);
        }
        buffer.position(0x1BF);
        byte[] encryptedTitleKey = new byte[0x10];
        buffer.get(encryptedTitleKey);

        buffer.position(0x1DC);
        byte[] titleId = new byte[0x10];
        Arrays.fill(titleId, (byte) 0);
        buffer.get(titleId, 0, 0x08);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new CryptoException(e);
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(commonKey, "AES"), new IvParameterSpec(titleId));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e);
        }

        byte[] titleKey;
        try {
            titleKey = cipher.doFinal(encryptedTitleKey);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoException(e);
        }

        try {
            return new WiiTicket(titleId, titleKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }
}
