package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
class DirectiveClause extends HtClause {
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
