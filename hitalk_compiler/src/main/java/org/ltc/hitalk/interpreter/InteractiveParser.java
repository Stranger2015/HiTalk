package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.Language;

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
     *
     */
    @Override
    public void initializeBuiltIns () {
        parser.initializeBuiltIns();
    }

    /**
     * @return
     */
    @Override
    public Sentence <HtClause> parseClause () {
        return parser.parseClause();
    }

    /**
     * @param t
     * @return
     */
    @Override
    public HtClause convert ( Term t ) {
        return null;//todo
    }

    /**
     * @return
     */
    public PlPrologParser getParser () {
        return parser;
    }
}
