package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.RecursiveList;
import com.thesett.aima.logic.fol.Term;

import java.util.Iterator;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public class DottedPair extends RecursiveList implements Term {
    public DottedPair () {
        super(-1, EMPTY_TERM_ARRAY);
    }

    /**
     * args = name + heads + tail
     *
     * @return
     */
    public Term newTail () {
        Term[] args = getArguments();
        return args[args.length - 1];
    }

    /**
     * args = name + heads + tail
     * @return
     */
    public Term[] getHeads () {
        Term[] args = getArguments();
        if (args.length <= 1) {
            return EMPTY_TERM_ARRAY;
        } else {
            int headsLen = args.length - 2;
            Term[] heads = new Term[headsLen];
            System.arraycopy(args, 1, heads, 0, headsLen);
            return heads;
        }
    }

    @Override
    public Iterator <Term> iterator () {
        return null;
    }

    @Override
    public boolean isNil () {
        return name < 0;
    }

    /**
     *
     */
    enum Kind {
        NIL, //"[]" "{}" "()" BY  INTERNED NAME

        LIST, //-1 [.......]
        BYPASS,//-2
        AND,//-3 blocked  term
    }
}
