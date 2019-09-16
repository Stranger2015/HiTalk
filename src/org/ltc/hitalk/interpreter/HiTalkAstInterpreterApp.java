package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;

/**
 *
 */
public
class HiTalkAstInterpreterApp<T extends HtClause, P, Q> implements IApplication {
    private IConfig config;

    public
    HiTalkAstInterpreterApp () {

    }

    public
    SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    public
    HtPrologParser getParser () {
        return parser;
    }

    public
    ICompiler <T, P, Q> getCompiler () {
        return compiler;
    }

    public
    HiTalkDefaultBuiltIn getDefaultBuiltIn () {
        return defaultBuiltIn;
    }

    private /*final*/ SymbolTable <Integer, String, Object> symbolTable;
    private/* final*/ VariableAndFunctorInterner interner;
    private /*final*/ HtPrologParser parser;
    private /*final */ ICompiler <T, P, Q> compiler;
    private /*final*/ HiTalkDefaultBuiltIn defaultBuiltIn;

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
     *
     *
     * @return
     */
    @Override
    public
    IConfig getConfig () {
        return config;
    }

    /**
     *
     */
    @Override
    public
    void start () throws Exception {

    }

    /**
     * @return
     */
    @Override
    public
    int end () {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStarted () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStopped () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public
    VariableAndFunctorInterner getInterner () {
        return interner;
    }

    /**
     *
     */
    @Override
    public
    void banner () {
//todo
    }

    @Override
    public
    void setParser ( HtPrologParser parser ) {
///todo
    }

    /**
     * @param arg
     */
    @Override
    public
    void setFileName ( String arg ) {
//todo
    }

    @Override
    public
    void setTokenSource ( TokenSource tokenSource ) {
        //TODO
    }

    public static
    void main ( String[] args ) {
        HiTalkAstInterpreterApp app = new HiTalkAstInterpreterApp();
        try {
            app.setFileName(args[0]);
            app.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public
    void setConfig ( IConfig config ) {
        this.config = config;
    }
}
/*

 */