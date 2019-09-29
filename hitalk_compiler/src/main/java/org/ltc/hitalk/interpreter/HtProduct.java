package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.INameable;
import org.ltc.hitalk.core.HtVersion;

/**
 *
 */
public
class HtProduct implements INameable <String> {
    protected String copyright;
    protected String name;
    protected HtVersion version;

    public
    HtProduct ( String copyright, String name, HtVersion version ) {
        this.copyright = copyright;
        this.name = name;
        this.version = version;
    }

    /**
     * @return
     */
    @Override
    public
    Functor getName () {
        return name;
    }
}
