package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;

import java.io.IOException;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public class LibParser implements IParser {
    /**
     *
     */
    protected PlPrologParser parser = getAppContext().getParser();

    public LibParser() throws Exception {
    }

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser () {
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
    public ITerm parse () throws Exception {
        return parser.termSentence();
    }

    /**
     *
     */
    @Override
    public void initializeBuiltIns () {
        parser.initializeBuiltIns();
    }

    /**
     * @return
     * @throws IOException
     */
    public ITerm next () throws Exception {
        return parser.next();
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
