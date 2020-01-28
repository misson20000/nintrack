package net.xenotoad.nintrack;

/**
 * Created by misson20000 on 2/25/17.
 */
public final class DirectoryFileType implements FileType {
    private DirectoryFileType() {

    }

    public static final DirectoryFileType instance = new DirectoryFileType();

    @Override
    public String getName() {
        return "Directory";
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
        return false;
    }
}
