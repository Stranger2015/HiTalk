package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;

import java.io.IOException;
import java.util.Deque;

import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_DOT;

/**
 *
 */
public class LibParser implements IParser {
    /**
     *
     */
    protected HtPrologParser parser = getAppContext().getParser();

    public LibParser() throws Exception {
    }

    @Override
    public Deque<PlLexer> getTokenSourceStack() {
        return parser.getTokenSourceStack();
    }

    /**
     * @return
     */
    @Override
    public HtPrologParser getParser() {
        return parser;
    }

    /**
     * @return
     */
    @Override
    public IVafInterner getInterner () {
        return parser.getInterner();
    }

    @Override
    public void setInterner ( IVafInterner interner ) {
        parser.setInterner(interner);
    }

    @Override
    public ITermFactory getFactory () {
        return parser.getFactory();
    }

    @Override
    public IOperatorTable getOptable () {
        return parser.getOptable();
    }

    /**
     * @param optable
     */
    @Override
    public void setOptable ( IOperatorTable optable ) {
        parser.setOptable(optable);
    }

    /**
     * @return
     */
    @Override
    public Language language () {
        return parser.language();
    }

    /**
     * @return
     */
    @Override
    public ITerm parse() throws Exception {
        return parser.termSentence();
    }

    /**
     * @return
     */
    public PlLexer getTokenSource() {
        return parser.getTokenSource();
    }

    /**
     *
     */
    @Override
    public void initializeBuiltIns() {
        parser.initializeBuiltIns();
    }

    /**
     * @param rdelim
     * @return
     * @throws IOException
     */
    public ITerm expr(PlToken.TokenKind rdelim) throws Exception {
        return null;
    }

    /**
     * @return
     * @throws IOException
     */
    public ITerm next() throws Exception {
        return parser.expr(TK_DOT);
    }

    /**
     * @return
     * @throws ParserException
     * @throws IOException
     * @throws HtSourceCodeException
     */
    @Override
    public HtClause parseClause () throws Exception {
        return parser.parseClause();
    }

    public void toString0 ( StringBuilder sb ) {
    }
}
