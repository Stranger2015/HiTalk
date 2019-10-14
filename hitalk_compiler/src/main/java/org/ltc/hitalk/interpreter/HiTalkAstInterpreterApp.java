package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
class HiTalkAstInterpreterApp<T extends HtClause, T, T> extends BaseApplication <T, T, T> {

    /**
     *
     * @param arg
     */
    public
    HiTalkAstInterpreterApp ( String arg ) {
        setFileName(arg);
    }

//    /**
//     * @param symbolTable
//     * @param interner
//     * @param parser
//     * @param compiler
//     * @param defaultBuiltIn
//     */
//    public
//    HiTalkAstInterpreterApp ( SymbolTable <Integer, String, Object> symbolTable,
//                              VariableAndFunctorInterner interner,
//                              HtPrologParser<T> parser,
//                              ICompiler <T, P, Q> compiler,
//                              HiTalkDefaultBuiltIn defaultBuiltIn ) {
//
//        this.symbolTable = symbolTable;
//        this.interner = interner;
//        this.parser = parser;
//        this.compiler = compiler;
//        this.defaultBuiltIn = defaultBuiltIn;
//    }

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
        HiTalkAstInterpreterApp app = new HiTalkAstInterpreterApp(args[0]);
        try {
//            app.setConfig(null);
//            app.setFileName(args[0]);
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
