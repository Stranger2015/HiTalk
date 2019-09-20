
package org.ltc.hitalk.core;

public
class HtVersion {
    private int major;
    private int minor;
    private int patch;
    private int build;
    private String suffix;

    public
    HtVersion ( int major, int minor, int patch, int build, String suffix, boolean snapshot ) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        this.suffix = suffix;
        this.snapshot = snapshot;
    }

    protected boolean snapshot;

    public
    int getMajor () {
        return major;
    }

    public
    void setMajor ( int major ) {
        this.major = major;
    }

    public
    int getMinor () {
        return minor;
    }

    public
    void setMinor ( int minor ) {
        this.minor = minor;
    }

    public
    int getPatch () {
        return patch;
    }

    public
    void setPatch ( int patch ) {
        this.patch = patch;
    }

    public
    int getBuild () {
        return build;
    }

    public
    void setBuild ( int build ) {
        this.build = build;
    }

    public
    String getSuffix () {
        return suffix;
    }

    public
    void setSuffix ( String suffix ) {
        this.suffix = suffix;
    }

    public
    boolean isSnapshot () {
        return snapshot;
    }

    public
    void setSnapshot ( boolean snapshot ) {
        this.snapshot = snapshot;
    }

    @Override
    public
    String toString () {
        return "HtVersion{" +
                "major=" + major +
                ", minor=" + minor +
                ", patch=" + patch +
                ", build=" + build +
                ", suffix='" + suffix + '\'' +
                ", snapshot=" + snapshot +
                '}';
    }
}
