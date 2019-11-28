package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;

/**
 * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
 * to the resolvers domain. Compiled queries are executed.
 * <p>
 * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
 */
public class ChainedCompilerObserver<P, Q> implements LogicCompilerObserver <P, Q> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCompilation ( Sentence <P> sentence ) {
    }

    /**
     * Accepts notification of the completion of the compilation of a query into binary form.
     *
     * @param sentence The compiled query.
     */
    @Override
    public void onQueryCompilation ( Sentence <Q> sentence ) {

    }
}