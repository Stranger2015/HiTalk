package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.function.Predicate;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
class HtProperty implements IProperty {
    private String s;
    private String v;
    private String[] options;
    protected HtNonVar[] values;
    protected IFunctor name;
    protected HtNonVar value;

    /**
     * @param name
     * @param arity
     * @param body
     * @param values
     */
    public HtProperty(IFunctor name, int arity, Predicate<IFunctor> body, HtNonVar... values) {
        this.name = name;
        this.values = values;
        if (values.length > 0) {

        }
    }

    public HtProperty(String name, String... values) throws Exception {
        ITerm t =
                this.name = (IFunctor) getAppContext().getParser().parseSingleTerm(name);
        this.values = new HtNonVar[values.length];
//        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
//            final String item = values[i];
//            this.values[i] = (IFunctor) getAppContext().getParser().parseSingleTerm(item);
//        }
        this.value = (HtNonVar) getAppContext().getParser().parseSingleTerm(values.length == 0 ? "_" : values[0]);
    }

    public String getS() {
        return s;
    }

    public String getV() {
        return v;
    }

    public HtProperty(String s, String v, String... options) {
        this.s = s;
        this.v = v;
        this.options = options;
    }

    /**
     * @return
     */
    @Override
    public IFunctor getName() {
        return name;
    }

    /**
     * @return
     */
    @Override
    public HtNonVar getValue() {
        return value;
    }


    /**
     * @param value
     */
    @Override
    public void setValue(HtNonVar value) {
        if (!value.equals(this.value)) {
            this.value = value;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HtProperty)) {
            return false;
        }

        final HtProperty that = (HtProperty) o;

        return getName().equals(that.getName());
    }

    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * @param name
     * @param value
     */
    public HtProperty(IFunctor name, HtNonVar value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return
     */
    public String getNameAsString() {
        return name.toString();
    }
}
