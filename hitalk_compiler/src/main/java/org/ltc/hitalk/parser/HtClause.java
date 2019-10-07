package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.ISubroutine;

/**
 *
 */
public class HtClause extends Clause <Functor> implements ISubroutine {
    protected final HtEntityIdentifier identifier;

    /**
     * @param head
     * @param body
     * @param identifier
     */
    public HtClause ( Functor head, Functor[] body, HtEntityIdentifier identifier ) {
        this(null, head, body);
    }

    /**
     * Creates a program sentence in L2.
     *
     * @param head The head of the program.
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public HtClause ( HtEntityIdentifier identifier, Functor head, Functor[] body ) {
        super(head, body);

        this.identifier = identifier;
    }

    public HtClause ( HtEntityIdentifier identifier, Functor head ) {
        this(identifier, head, null);
    }

    /**
     * Gets the wrapped sentence in the logical language over T.
     *
     * @return The wrapped sentence in the logical language.
     */
//    @Override
    public HtClause getT () {
        return this;
    }

    public boolean isDcgRule () {
        return false;
    }
}