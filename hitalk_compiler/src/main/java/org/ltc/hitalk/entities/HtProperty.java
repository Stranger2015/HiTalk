package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
class HtProperty implements IProperty {
    //    protected HtType type;
    protected String name;
    protected Term value;

    /**
     * @param name
     * @param args
     */
    public
    HtProperty ( String name, ListTerm args ) {
        this.name = name;
        this.value = args;
    }

    public
    HtProperty ( ListTerm terms ) {

    }

//    public
//    HtType getType () {
//        return type;
//    }

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
     * @param term
     */
    @Override
    public
    void setValue ( Term term ) {
        if (!value.equals(term)) {
            value = term;
        }
    }

    /**
     * @param name
     * @param value
     */
    public
    HtProperty ( String name, Term value ) {
//        this.type = type;
        this.name = name;
        this.value = value;
    }
}
