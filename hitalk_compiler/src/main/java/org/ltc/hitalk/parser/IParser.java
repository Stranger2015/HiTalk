package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ParseException;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.TermParser;
import org.ltc.hitalk.term.HlOpSymbol;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;

import static java.lang.String.format;

/**
 *
 */
public interface IParser extends TermParser <ITerm> {
    PlPrologParser getParser ();

    default void setParser ( PlPrologParser parser ) {
        if (parser.getClass() == getParser().getClass()) {
            throw new IllegalStateException(
                    format("INTERNAL ERROR:%s#setParser()", parser.getClass().getSimpleName()));
        }

        doSetParser(parser);
    }

    default void doSetParser ( PlPrologParser parser ) {
        getParser().setParser(parser);
    }

    /*
     * @return
     */
    default HiTalkStream getStream () {
        return getParser().getStream();
    }

    /**
     * @param stream
     */
    default void setStream ( HiTalkStream stream ) {
        getParser().setStream(stream);
    }

    default IVafInterner getInterner () {
        return getParser().getInterner();
    }

    default void setInterner ( IVafInterner interner ) {
        getParser().setInterner(interner);
    }

    default ITermFactory getFactory () {
        return getParser().getFactory();
    }

    default IOperatorTable getOptable () {
        return getParser().getOptable();
    }

    default void setOptable ( IOperatorTable optable ) {
        getParser().setOptable(optable);
    }

    /**
     * @param op
     */
    default void setOperator ( HlOpSymbol op ) {
        getParser().setOperator(op);
    }

    /**
     * @return
     */
    Language language ();

    /**
     * @return
     * @throws SourceCodeException
     */
    default Sentence <ITerm> parse () throws SourceCodeException, ParseException, IOException {
        return getParser().parse();
    }

    /**
     * @return
     */
    default PlTokenSource getTokenSource () {
        return getParser().getTokenSource();
    }

    /**
     * @param source
     */
    default void setTokenSource ( PlTokenSource source ) {
        getParser().setTokenSource(source);
    }

    /**
     * @param name
     * @param priority
     * @param associativity
     */
    default void internOperator ( String name, int priority, Associativity associativity ) {
        getParser().internOperator(name, priority, associativity);
    }

    /**
     *
     */
    void initializeBuiltIns ();

    /**
     * @param factory
     */
    default void setTermFactory ( ITermFactory factory ) {
        getParser().setTermFactory(factory);
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    default ITerm next () throws IOException {
        return getParser().next();
    }

    /**
     * @return
     */
    Sentence <HtClause> parseClause ();

    /**
     * @return
     */
    default HtClause sentence () {
        return getParser().sentence();
    }

    /**
     * @param t
     * @return
     */
    default HtClause convert ( ITerm t ) throws SourceCodeException {
        return TermUtilities.convertToClause((IFunctor) t, getInterner());
    }
}
