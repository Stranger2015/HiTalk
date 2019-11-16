package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.term.HtBaseTerm;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public class HtClause/*<F extends IFunctor>*/ extends HtBaseTerm implements ITerm, ISubroutine {
    protected final HtEntityIdentifier identifier;
    protected IFunctor head;
    protected IFunctor[] body;



    /**
     * Creates a program sentence in L2.
     *
     * @param head The head of the program.
     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
     */
    public HtClause ( HtEntityIdentifier identifier, IFunctor head, IFunctor[] body ) {
//        super(head, body);
        this.identifier = identifier;
        this.head = head;
        this.body = body;
    }

    /**
     * @param identifier
     * @param head
     */
    public HtClause ( HtEntityIdentifier identifier, IFunctor head ) {
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

    public IFunctor getHead () {
        return null;
    }

    public IFunctor[] getBody () {
        return new IFunctor[0];
    }

    /**
     * @param i
     * @return
     */
    @Override
    public IFunctor getGoal ( int i ) {
        return getBody()[i];
    }

    /**
     * @return
     */
    public int bodyLength () {
        return getBody().length;
    }

    public ITerm getValue () {
        return null;
    }//todo

    public void free () {

    }

    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return toString();
    }

    public ListTerm getBodyAsListTerm () {
        return null;
    }
}