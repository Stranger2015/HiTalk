package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.stream.IntStream;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public
class HtProperty implements IProperty {
    private ITerm[] values = EMPTY_TERM_ARRAY;
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

    /**
     * @param terms
     */
    public HtProperty(ListTerm terms) {

    }

    public HtProperty(String name, String value, String... values) {
        this(name, getAppContext().getTermFactory().newVariable(value));//ficme

        this.value = getAppContext().getTermFactory().createAtom(value);
        IntStream.range(0, values.length).forEachOrdered(i -> this.values[i] =
                getAppContext().getTermFactory().createAtom(values[i]));
    }

    /**
     * @param name
     * @param newVariable
     */
    public HtProperty(String name, HtVariable newVariable) {
    }

    /**
     * @param name
     * @param value
     * @param values
     * @return
     */
    public static HtProperty createProperty(String name, String value, String... values) {
        return new HtProperty(name, value, values);
    }

    /**
     *
     */
    public static HtProperty createProperty(String name, String value) {
        return new HtProperty(name, value, EMPTY_STRING_ARRAY);
    }


    /**
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    @Override
    public ITerm getValue() {
        return value;
    }

    /**
     * @param term
     */
    @Override
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
        this.name = name;
        this.value = value;
    }
}
