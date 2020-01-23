package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.wam.compiler.IFunctor;

import static java.util.stream.IntStream.range;
import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
class HtProperty implements IProperty {
    protected HtNonVar[] values;
    protected IFunctor name;
    protected HtNonVar value;
//
//    /**
//     * @param terms
//     */
//    public HtProperty(ListTerm terms) {
//
//    }

    public HtProperty(IFunctor name, HtNonVar value, HtNonVar... values) {
        this.name = name;

        this.value = getAppContext().getTermFactory().createNonvar(value);
        this.values = new HtNonVar[values.length];
        range(0, values.length).forEach(i -> this.values[i] =
                getAppContext().getTermFactory().createNonvar(values[i]));
    }

//    /**
//     * @param name
//     * @param newVariable
//     */
//    public HtProperty(String name, HtVariable newVariable) {
//    }

//    /**
//     * @param name
//     * @param value
//     * @param values
//     * @return
//     */
//    public static HtProperty createProperty(String name, String value, String... values) {
//        return new HtProperty(name, value, values);
//    }
//
    /**
     *
     */
//    public static HtProperty createProperty(String name, String value) {
//        return new HtProperty(name, value, EMPTY_STRING_ARRAY);
//    }
//

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
}
