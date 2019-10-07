package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.core.HtVersion;

public interface IProduct extends INameable {

    /**
     * @return
     */
    String getCopyright ();

    /**
     * @return
     */
    HtVersion getVersion ();
}
