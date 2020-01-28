package net.xenotoad.nintrack.format.gamefile.brres;

import net.xenotoad.nintrack.FileType;

/**
 * Created by misson20000 on 3/12/17.
 */
public class BRRESGroupFileType implements FileType {
    public static final BRRESGroupFileType instance = new BRRESGroupFileType();

    private BRRESGroupFileType() {

    }

    @Override
    public String getName() {
        return "BRRES Group";
    }

    @Override
    public boolean canBeExtracted() {
        return false;
    }

    @Override
    public String getCheckboxName() {
        return null;
    }

    @Override
    public boolean openByDefault() {
        return true;
    }
}
