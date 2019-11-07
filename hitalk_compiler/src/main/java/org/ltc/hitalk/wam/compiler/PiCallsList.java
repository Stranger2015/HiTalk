package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 */
public class PiCallsList implements Supplier <PiCalls> {
    private final List <HtClause> clauses = new ArrayList <>();
    private final List <PiCalls> piCalls = new ArrayList <>();


    /**
     *
     */
    public List <PiCalls> build () {
        return piCalls;
    }

    /**
     * @param t
     * @return
     */
    public PiCallsList add ( PiCalls t ) {
        piCalls.add(t);
        return this;
    }

    /**
     * @param left
     * @param right
     * @return
     */
    public PiCallsList apply ( PiCallsList left, PiCallsList right ) {
        return left.addAll(right.piCalls);
    }

    public static List <PiCalls> mergeCalls () {
        return null;
    }

    public PiCallsList add ( PiCallsList piCallsList, PiCalls t ) {
        piCallsList.add(t);
        return this;
    }

    /**
     * @param calls
     * @return
     */
    public PiCallsList addAll ( List <PiCalls> calls ) {
        piCalls.addAll(calls);
        return this;
    }

    /**
     * @return
     */
    @Override
    public PiCalls get () {
        return piCalls.get(0);
    }

    /**
     *
     */
    public static class Builder implements Supplier <PiCalls> {

        /**
         * @return
         */
        @Override
        public PiCalls get () {
            return null;
        }
    }
}
