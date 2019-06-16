package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;

public
class HtProperty {
    protected HiTalkType type;
    protected String name;
    protected Term value;

    public
    HiTalkType getType () {
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
    HtProperty ( HiTalkType type, String name, Term value ) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
