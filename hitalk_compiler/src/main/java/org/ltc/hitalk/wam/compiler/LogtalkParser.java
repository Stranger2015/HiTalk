package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.io.HiTalkStream;

/**
 *
 */
public class LogtalkParser extends PlPrologParser {
    /**
     * @param stream
     * @param interner
     * @param factory
     * @param optable
     */
    public LogtalkParser ( HiTalkStream stream,
                           IVafInterner interner,
                           ITermFactory factory,
                           IOperatorTable optable ) {
        super(stream, interner, factory, optable);
    }

    /**
     *
     */
    public LogtalkParser () {
    }
}
