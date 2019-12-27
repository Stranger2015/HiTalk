
package org.ltc.hitalk.core;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import static java.lang.String.format;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.RESOURCE_ERROR;

/**
 *
 */
public
class HtVersion {
    private int major;
    private int minor;
    private int patch;
    private int build;

    private int updateBuildNum ( String compilerProps, String buildNum ) throws IOException {
        Properties properties = new Properties();
        File file = Paths.get(compilerProps).toAbsolutePath().toFile();
        /*return*/
        properties.load(new FileInputStream(new File(compilerProps)));
        int build = (int) properties.get(buildNum);
        properties.replace(buildNum, ++build);
        return build;
    }

    private String suffix;

    /**
     * @param major
     * @param minor
     * @param patch
     * @param build
     * @param suffix
     * @param snapshot
     */
    public HtVersion ( int major, int minor, int patch, int build, String suffix, boolean snapshot ) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        try {
            this.build = build == -1 ? updateBuildNum("compiler.properties", "build.num") : build;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionError(RESOURCE_ERROR, null);
        }
        this.suffix = suffix;
        this.snapshot = snapshot;
    }

    protected boolean snapshot;

    public int getMajor () {
        return major;
    }

    public void setMajor ( int major ) {
        this.major = major;
    }

    public int getMinor () {
        return minor;
    }

    public void setMinor ( int minor ) {
        this.minor = minor;
    }

    public int getPatch () {
        return patch;
    }

    public void setPatch ( int patch ) {
        this.patch = patch;
    }

    public int getBuild () {
        return build;
    }

    public void setBuild ( int build ) {
        this.build = build;
    }

    public String getSuffix () {
        return suffix;
    }

    /**
     * @param suffix
     */
    public void setSuffix ( String suffix ) {
        this.suffix = suffix;
    }

    /**
     * @return
     */
    public boolean isSnapshot () {
        return snapshot;
    }

    /**
     * @param snapshot
     */
    public void setSnapshot ( boolean snapshot ) {
        this.snapshot = snapshot;
    }

    @Override
    public String toString () {
        return format("version %d.%d.%db#%d%s%s", major, minor, patch, build, suffix, "");
    }
}
