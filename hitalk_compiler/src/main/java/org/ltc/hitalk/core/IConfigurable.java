package org.ltc.hitalk.core;

import org.ltc.hitalk.compiler.bktables.IConfig;

/**
 *
 */
public
interface IConfigurable {

    /**
     * @return
     */
    IConfig getConfig ();

    /**
     * @param config
     */
    void setConfig ( IConfig config );
}

