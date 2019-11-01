package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;

/**
 * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
 * to the resolvers domain. Compiled queries are executed.
 * <p>
 * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
 */
public class ChainedCompilerObserver<P, Q> implements LogicCompilerObserver <P, Q> {
//
//    /**
//     * Sets the chained observer for compiler outputs.
//     *
//     * @param observer The chained observer.
//     */
//    public void setCompilerObserver ( LogicCompilerObserver <T, Q> observer ) {
//        this.observer = observer;
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCompilation ( Sentence <P> sentence ) throws SourceCodeException {
    }

    /**
     * Accepts notification of the completion of the compilation of a query into binary form.
     *
     * @param sentence The compiled query.
     * @throws SourceCodeException If there is an error in the compiled code that prevents its further processing.
     */
    @Override
    public void onQueryCompilation ( Sentence <Q> sentence ) throws SourceCodeException {

    }
}