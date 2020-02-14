package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IdentifiedTerm;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;

import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;

/**
 *
 */
public interface IParser extends IHitalkObject {
    /**
     * @return
     */
    HtPrologParser getParser();

    /**
     * @param parser
     */
    default void setParser(HtPrologParser parser) {
        if (parser.getClass() == getParser().getClass()) {
            throw new IllegalStateException(
                    String.format("INTERNAL ERROR:%s#setParser()", parser.getClass().getSimpleName()));
        }

        doSetParser(parser);
    }

    /**
     * @param parser
     */
    default void doSetParser(HtPrologParser parser) {
        getParser().setParser(parser);
    }

    /**
     * @return
     */
    default HiTalkStream getStream() {
        return getParser().getStream();
    }

    /**
     * @param stream
     */
    default void setStream(HiTalkStream stream) {
        getParser().setStream(stream);
    }

    /**
     * @return
     */
    IVafInterner getInterner();

    /**
     * @param interner
     */
    void setInterner(IVafInterner interner);

    /**
     * @return
     */
    ITermFactory getFactory()//fixme
    ;

    /**
     * @return
     */
    IOperatorTable getOptable()//fixme
    ;

    /**
     * @param optable
     */
    void setOptable(IOperatorTable optable);

    /**
     * @param op
     */
    default void setOperator(IdentifiedTerm op) {
        getParser().setOperator(op);
    }

    /**
     * @return
     */
    Language language();

    /**
     * @return
     * @throws HtSourceCodeException
     */
    ITerm parse() throws Exception;

    /**
     * @return
     */
    default PlLexer getTokenSource() {
        return getParser().getTokenSource();
    }

    /**
     * @param source
     */
    default void setTokenSource(PlLexer source) {
        if (source.isOpen()) {
            getParser().setTokenSource(source);
        }
    }

    /**
     * @return
     */
    default PlLexer popTokenSource() throws IOException {
        PlLexer ts = getTokenSource();
        ts.getInputStream().removeListener(ts);
        ts = getParser().popTokenSource();
        ts.getInputStream().addListener(ts);
        ts.close();
        return ts;

    }

    /**
     * @param name
     * @param priority
     * @param associativity
     */
    default void internOperator(String name, int priority, Associativity associativity) {
        getParser().internOperator(name, priority, associativity);
    }

    /**
     *
     */
    void initializeBuiltIns();

    /**
     * @param factory
     */
    default void setTermFactory(ITermFactory factory) {
        getParser().setTermFactory(factory);
    }

    /**
     * @return
     * @throws IOException
     */
    ITerm expr() throws Exception;

    /**
     * @return
     */
    HtClause parseClause() throws Exception;

    /**
     * @return
     */
    default HtClause sentence() {
        return getParser().sentence();
    }

    /**
     * @param t
     * @return
     */
    default HtClause convert(ITerm t) throws Exception {
        return convertToClause(t, getInterner());
    }
}
