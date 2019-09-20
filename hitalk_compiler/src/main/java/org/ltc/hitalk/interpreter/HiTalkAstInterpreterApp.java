package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;

/**
 *
 */
public
class HiTalkAstInterpreterApp<T extends HtClause, P, Q> extends BaseApplication <T, P, Q> {

    /**
     *
     */
    public
    HiTalkAstInterpreterApp () {

    }

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param compiler
     * @param defaultBuiltIn
     */
    public
    HiTalkAstInterpreterApp ( SymbolTable <Integer, String, Object> symbolTable,
                              VariableAndFunctorInterner interner,
                              HtPrologParser parser,
                              ICompiler <T, P, Q> compiler,
                              HiTalkDefaultBuiltIn defaultBuiltIn ) {

        this.symbolTable = symbolTable;
        this.interner = interner;
        this.parser = parser;
        this.compiler = compiler;
        this.defaultBuiltIn = defaultBuiltIn;
    }

    /**
     * @return
     */

    /**
     *
     */
    @Override
    public
    void banner () {
//todo
    }

    public static
    void main ( String[] args ) {
        HiTalkAstInterpreterApp app = new HiTalkAstInterpreterApp();
        try {
            app.setConfig(null);
            app.setFileName(args[0]);
            app.init();
            app.start();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    /**
     *
     */
    @Override
    public
    void doStart () {

    }
}
