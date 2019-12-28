package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;

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
    public DirectiveClause ( ListTerm body ) {
        super(null, body);
    }

    /**
     *
     */
    enum Kind {
        SOURCE_FILE_DIRECTIVE,
        CONDITIONAL_COMPILATION_DIRECTIVE,
        ENCODING,
        INCLUDE,
        OP,
        MODULE,

        OBJECT_DIRECTIVE,
        CATEGORY_DIRECTIVE,
        PROTOCOL_DIRECTIVE,
        PREDICATE_DIRECTIVE,

    }
}
