package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Sentence;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public
class HtClause extends Clause <Functor> implements Sentence <HtClause> {
    private final HtEntityIdentifier identifier;

    /**
     * @param head
     * @param body
     */
    public
    HtClause ( Functor head, Functor[] body ) {
        this(null, head, body);
    }

    /**
     * Creates a program sentence in L2.
     *
     * @param head The head of the program.
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public
    HtClause ( HtEntityIdentifier identifier, Functor head, Functor[] body ) {
        super(head, body);

        this.identifier = identifier;
    }

    /**
     * Gets the wrapped sentence in the logical language over T.
     *
     * @return The wrapped sentence in the logical language.
     */
    @Override
    public
    HtClause getT () {
        return this;
    }
}
