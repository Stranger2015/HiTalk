package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Clause;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.wam.compiler.HtFunctor;

/**
 *
 */
public class HtClause extends Clause <HtFunctor> implements ISubroutine {
    protected final HtEntityIdentifier identifier;

    /**
     * Creates a program sentence in L2.
     *
     * @param head The head of the program.
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public HtClause ( HtEntityIdentifier identifier, HtFunctor head, HtFunctor[] body ) {
        super(head, body);

        this.identifier = identifier;
    }

    /**
     * @param identifier
     * @param head
     */
    public HtClause ( HtEntityIdentifier identifier, HtFunctor head ) {
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

    /**
     * @return
     */
    public boolean isDcgRule () {
        return false;
    }

//    public PackedDottedPair getBodyAsDottedPair () {
//        return getBody();
//    }

    /**
     * @param i
     * @return
     */
    @Override
    public HtFunctor getGoal ( int i ) {
        return getBody()[i];
    }

    /**
     * @return
     */
    public int bodyLength () {
        return getBody().length;
    }
}