package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.INameable;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
class HtProperty implements IProperty, INameable <Functor> {
    protected HtType type;
    protected Functor name;
    protected Term value;

    /**
     * @param name
     * @param args
     */
    public
    HtProperty ( int name, ListTerm args ) {
        this.name = name;
        this.value = args;
    }

//  YYTUUYYY

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
    Functor getName () {
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
    HtProperty ( HtType type, Functor name, Term value ) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
