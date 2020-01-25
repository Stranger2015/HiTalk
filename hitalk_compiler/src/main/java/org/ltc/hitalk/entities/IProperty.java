package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public
interface IProperty {

    /**
     * @return
     */
    IFunctor getName();

    String getNameAsString();

    /**
     * @return
     */
    HtNonVar getValue();

    /**
     * @param value
     */
    void setValue(HtNonVar value);
}
