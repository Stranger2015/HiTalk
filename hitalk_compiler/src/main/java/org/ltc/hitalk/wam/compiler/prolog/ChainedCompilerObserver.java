package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.parser.HtClause;

/**
 * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
 * to the resolvers domain. Compiled queries are executed.
 * <p>
 * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
 */
public class ChainedCompilerObserver<T extends HtClause, P, Q> implements LogicCompilerObserver <T, Q> {
    /**
     * Holds the chained observer for compiler outputs.
     */
    protected LogicCompilerObserver <T, Q> observer;
    protected Resolver <T, Q> resolver;

    /**
     * Sets the chained observer for compiler outputs.
     *
     * @param observer The chained observer.
     */
    public void setCompilerObserver ( LogicCompilerObserver <T, Q> observer ) {
        this.observer = observer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCompilation ( Sentence <T> sentence ) throws SourceCodeException {
        if (observer != null) {
            observer.onCompilation(sentence);
        }

        getResolver().addToDomain(sentence.getT());
    }

    /**
     * @return
     */
    public Resolver <T, Q> getResolver () {
        return resolver;
    }

    /**
     * @param resolver
     */
    public void setResolver ( Resolver <T, Q> resolver ) {
        this.resolver = resolver;
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