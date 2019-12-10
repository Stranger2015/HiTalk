package org.ltc.hitalk.interpreter;


import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;

/**
 *
 */
public class InteractiveParser implements IParser {
    protected PlPrologParser parser;

    /**
     * @param parser
     */
    public InteractiveParser ( PlPrologParser parser ) {
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
     * @throws SourceCodeException
     */
    public ISentence <ITerm> parse () throws SourceCodeException, ParseException, IOException {
        return parser.parse();
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
     */
    @Override
    public HtClause parseClause () throws ParseException, IOException, SourceCodeException {
        return parser.parseClause();
    }

    /**
     * @return
     */
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

    public void setOptable ( IOperatorTable optable ) {
        parser.setOptable(optable);
    }
}
