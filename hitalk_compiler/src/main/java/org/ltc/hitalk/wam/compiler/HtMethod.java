package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;

/**
 * @param <F>
 */
public class HtMethod<F extends HtFunctor> extends HtClause <F> {
    /**
     * Creates a program sentence in L2.
     *
     * @param identifier
     * @param head       The head of the program.
     * @param body       The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public HtMethod ( HtEntityIdentifier identifier, F head, ListTerm body ) {
        super(head, body);
    }

    /**
     * @param identifier
     * @param head
     */
    public HtMethod ( HtEntityIdentifier identifier, F head ) {
        this(identifier, head, null);
    }
}
