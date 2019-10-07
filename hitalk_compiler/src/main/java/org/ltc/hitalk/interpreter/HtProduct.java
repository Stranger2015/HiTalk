package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.core.HtVersion;

/**
 *
 */
public class HtProduct implements IProduct {

    protected String copyright;
    protected String name;
    protected HtVersion version;

    /**
     * @param copyright
     * @param name
     * @param version
     */
    public HtProduct ( String copyright, String name, HtVersion version ) {
        this.copyright = copyright;
        this.name = name;
        this.version = version;
    }

    /**
     * @return
     */
    public String getCopyright () {
        return copyright;
    }

    /**
     * @return
     */
    public HtVersion getVersion () {
        return version;
    }

    /**
     * @return
     */
    @Override
    public
    String getName () {
        return name;
    }
}
