package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;

public
class HtProperty {
    protected HtType type;
    protected String name;
    protected Term value;

    public
    HtType getType () {
        return type;
    }

    public
    String getName () {
        return name;
    }

    public
    Term getValue () {
        return value;
    }

    public
    HtProperty ( HtType type, String name, Term value ) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
