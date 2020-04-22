package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.OpSymbolFunctor;
import org.ltc.hitalk.term.OpSymbolFunctor.Associativity;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;
import java.util.Deque;

import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;

/**
 *
 */
public interface IParser<T extends HtClause> extends IHitalkObject {

    /**
     * @return
     */
    Deque<PlLexer> getTokenSourceStack();

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
    default void setOperator(OpSymbolFunctor op) {
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
        return getTokenSourceStack().peek();
    }

    /**
     * @param source
     */
    default void setTokenSource(PlLexer source) {
        final Deque<PlLexer> stack = getTokenSourceStack();
        if (!stack.contains(source) && source.isOpen()) {
            stack.push(source);
            source.getInputStream().addListener(source);
        }
    }

    /**
     *
     */
    default void popTokenSource() {
        PlLexer ts = getTokenSource();
        if (ts == null) {
            return;
        }
        ts.getInputStream().removeListener(ts);

        if (!getParser().tokenSourceStack.isEmpty()) {
            ts = getParser().tokenSourceStack.pop();
            ts.close();
            if (!getParser().tokenSourceStack.isEmpty()) {
                ts = getTokenSource();
                ts.getInputStream().addListener(ts);
            }
        }
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
     *
     * @param factory
     */
    default void setTermFactory(ITermFactory factory) {
        getParser().setTermFactory(factory);
    }

    /**
     * @param rdelim
     * @return
     * @throws IOException
     */
    ITerm expr(TokenKind rdelim) throws Exception;
//
//    /**
//     *
//     * @return
//     */
//    T parseClause() throws Exception;

    ;

    default T convert(ITerm t) throws Exception {
        return (T) convertToClause(t, getInterner());
    }
}
