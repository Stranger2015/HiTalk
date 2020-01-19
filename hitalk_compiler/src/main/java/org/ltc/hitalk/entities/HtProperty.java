package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public
class HtProperty implements IProperty {
    private final ITerm[] values = new ITerm[0];
    //    protected HtType type;
    protected String name;
    protected ITerm value;

    /**
     * @param name
     * @param args
     */
    public HtProperty(String name, ListTerm args) {
        this.name = name;
        this.value = args;
        values = EMPTY_TERM_ARRAY;
    }

    public HtProperty(ListTerm terms) {

    }

    public HtProperty(String alias, String atom, String[] values) {
        this(alias, getAppContext().getTermFactory().newVariable(atom));
        for (int i = 0; i < values.length; i++) {
            this.values[i] = getAppContext().getTermFactory().createFlag() values[i];

        }
        this.values = values;
    }

//    public
//    HtType getType () {
//        return type;
//    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public ITerm getValue() {
        return value;
    }

    /**
     * @param term
     */
//    @Override
    public void setValue(ITerm term) {
        if (!value.equals(term)) {
            value = term;
        }
    }

    /**
     * @param name
     * @param value
     */
    public HtProperty(String name, ITerm value) {
//        this.type = type;
        this.name = name;
        this.value = value;
    }
}
