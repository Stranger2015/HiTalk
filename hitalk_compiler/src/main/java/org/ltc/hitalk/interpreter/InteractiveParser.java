package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;
import java.util.Deque;

import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_DOT;

/**
 *
 */
public class InteractiveParser implements IParser {
    protected HtPrologParser parser;

    /**
     * @param parser
     */
    public InteractiveParser (HtPrologParser parser ) {
        this.parser = parser;
    }

    /**
     *
     */
    public InteractiveParser () {
        super();
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
     * @throws Exception
     */
    public ITerm parse() throws Exception {
        return parser.parse();
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
        return parser.expr(TK_DOT);//fixme
    }

//    /**
//     * @return
//     */
//    @Override
//    public HtClause parseClause() throws Exception {
//        return parser.parseClause();
//    }

    @Override
    public Deque<PlLexer> getTokenSourceStack() {
        return parser.getTokenSourceStack();
    }

    /**
     * @return
     */
    public HtPrologParser getParser() {
        return parser;
    }

    @Override
    public IVafInterner getInterner() {
        return parser.getInterner();
    }

    @Override
    public void setInterner(IVafInterner interner) {
        parser.setInterner(interner);
    }

    @Override
    public ITermFactory getFactory() {
        return parser.getFactory();
    }

    public IOperatorTable getOptable() {
        return parser.getOptable();
    }

    @Override
    public void setOptable(IOperatorTable optable) {
        parser.setOptable(optable);
    }

    public void toString0(StringBuilder sb) {

    }
}
