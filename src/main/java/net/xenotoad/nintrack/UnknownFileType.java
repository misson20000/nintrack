package net.xenotoad.nintrack;

/**
 * Created by misson20000 on 2/25/17.
 */
public final class UnknownFileType implements FileType {
    public static final UnknownFileType instance = new UnknownFileType();

    private UnknownFileType() {

    }

    @Override
    public String getName() {
        return "?";
    }

    @Override
    public boolean canBeExtracted() {
        return false;
    }

    @Override
    public String getCheckboxName() {
        return "";
    }

    @Override
    public boolean openByDefault() {
        return false;
    }
}
