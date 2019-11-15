package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
class HtProperty implements IProperty {
    //    protected HtType type;
    protected String name;
    protected ITerm value;

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
    public ITerm getValue () {
        return value;
    }

    /**
     * @param term
     */
//    @Override
    public void setValue ( ITerm term ) {
        if (!value.equals(term)) {
            value = term;
        }
    }

    /**
     * @param name
     * @param value
     */
    public HtProperty ( String name, ITerm value ) {
//        this.type = type;
        this.name = name;
        this.value = value;
    }
}
