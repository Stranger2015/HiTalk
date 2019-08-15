package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;

/**
 *
 */
public
class DirectiveClause extends Clause <Functor> {
    /**
     * Creates a program sentence in L2.
     *
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public
    DirectiveClause ( Functor[] body ) {
        super(null, body);
    }

    /**
     *
     */
    enum Kind {
        SOURCE_FILE_DIRECTIVE,
        CONDITIONAL_COMPILATION_DIRECTIVE,
        OBJECT_DIRECTIVE,
        CATEGORY_DIRECTIVE,
        PROTOCOL_DIRECTIVE,
        PREDICATE_DIRECTIVE,

    }
}
