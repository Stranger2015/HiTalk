package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.isoprologparser.PrologParser;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;

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
    public ITerm parse () throws Exception {
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
    public ITerm expr() throws Exception {
        return null;
    }

    /**
     * @return
     * @throws IOException
     */
    public ITerm next () throws Exception {
        return parser.next();//fixme
    }

    /**
     * @return
     */
    @Override
    public HtClause parseClause () throws Exception {
        return parser.parseClause();
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
