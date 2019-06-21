package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.INameable;

public
class HtProperty implements INameable <FunctorName> {
    protected HtType type;
    protected String name;
    protected Term value;

    /**
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
    String getName () {
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
    HtProperty ( HtType type, String name, Term value ) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
