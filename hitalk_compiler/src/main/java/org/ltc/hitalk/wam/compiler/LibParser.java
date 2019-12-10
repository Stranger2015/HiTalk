package org.ltc.hitalk.wam.compiler;

import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;

import java.io.IOException;

/**
 *
 */
public class LibParser implements IParser {
    /**
     *
     */
    protected PlPrologParser parser = BaseApp.getAppContext().getParser();

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    public IVafInterner getInterner () {
        return parser.getInterner();
    }

    public void setInterner ( IVafInterner interner ) {
        parser.setInterner(interner);
    }

    public ITermFactory getFactory () {
        return parser.getFactory();
    }

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
    public ISentence <ITerm> parse () throws ParseException, IOException {
        return new PlSentenceImpl(parser.termSentence());
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
    public ITerm next () throws IOException, ParseException {
        return parser.next();
    }

    /**
     * @return
     * @throws ParseException
     * @throws IOException
     * @throws SourceCodeException
     */
    @Override
    public HtClause parseClause () throws ParseException, IOException, SourceCodeException {
        return parser.parseClause();
    }
}
