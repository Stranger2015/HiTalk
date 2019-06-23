package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.HtType;

/**
 *
 */
public abstract
class Flag extends HtProperty {

    /**
     * @param type
     * @param name
     * @param value
     */
    public
    Flag ( HtType type, Functor name, Term value ) {
        super(type, name, value);
    }
}