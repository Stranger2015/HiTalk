package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.INameable;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
class HtProperty implements IProperty, INameable <FunctorName> {
    protected HtType type;
    protected FunctorName name;
    protected Term value;

    /**
     * @param name
     * @param args
     */
    public
    HtProperty ( FunctorName name, ListTerm args ) {
        this.name = name;
        this.value = args;
    }

    public
    HtProperty ( ListTerm terms ) {

    }

    /**
     *
     * @return
     */
    public
    HtType getType () {
        return type;
    }

    /**
     * @return
     */
    public
    FunctorName getName () {
        return name;
    }

    /**
     * @return
     */
    public
    Term getValue () {
        return value;
    }

    /**
     * @param type
     * @param name
     * @param value
     */
    public
    HtProperty ( HtType type, FunctorName name, Term value ) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
