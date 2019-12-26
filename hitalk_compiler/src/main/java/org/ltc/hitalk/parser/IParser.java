package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.term.HlOpSymbol;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;

/**
 *
 */
public interface IParser extends IHitalkObject {
    /**
     * @return
     */
    PlPrologParser getParser ();

    /**
     * @param parser
     */
    default void setParser ( PlPrologParser parser ) {
        if (parser.getClass() == getParser().getClass()) {
            throw new IllegalStateException(
                    String.format("INTERNAL ERROR:%s#setParser()", parser.getClass().getSimpleName()));
        }

        doSetParser(parser);
    }

    /**
     * @param parser
     */
    default void doSetParser ( PlPrologParser parser ) {
        getParser().setParser(parser);
    }

    /**
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

    IVafInterner getInterner ();

    void setInterner ( IVafInterner interner );

    ITermFactory getFactory ()//fixme
    ;

    IOperatorTable getOptable ()//fixme
    ;

    void setOptable ( IOperatorTable optable );

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
     * @throws HtSourceCodeException
     */
    ITerm parse () throws Exception;

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
    ITerm next () throws Exception;

    /**
     * @return
     */
    HtClause parseClause () throws Exception;

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
    default HtClause convert ( ITerm t ) throws HtSourceCodeException {
        return TermUtilities.convertToClause(t, getInterner());
    }
}
