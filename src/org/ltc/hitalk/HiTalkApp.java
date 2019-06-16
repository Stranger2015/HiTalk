package org.ltc.hitalk;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.aima.logic.fol.isoprologparser.ClauseParser;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.wam.compiler.HiTalkCompiler;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;

import java.io.IOException;

public
class HiTalkApp extends HiTalkEngine implements IApplication {

    protected String fileName;
    protected IConfig config;
//    protected HiTalkCompiler compiler;

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     * @param compiler The compiler.
     */

    public
    HiTalkApp ( ClauseParser parser, VariableAndFunctorInterner interner, LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler ) {
        super(parser, interner, compiler);
    }

    /**
     * @param args
     */
    public static
    void main ( String[] args ) {
        try {
            SymbolTable <Integer, String, Object> symbolTable = new SymbolTableImpl <>();
            VariableAndFunctorInterner interner = new VariableAndFunctorInternerImpl("HiTalk_Variable_Namespace", "HiTalk_Functor_Namespace");
            LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler = new HiTalkCompiler(symbolTable, interner, new HiTalkDefaultBuiltIn(symbolTable, interner));

            HiTalkApp app = new HiTalkApp(new ClauseParser(interner), interner, compiler);
            app.setFileName(args[0]);
            app.banner();
            app.start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    /**
     * @return
     */
    public
    String getFileName () {
        return fileName;
    }

    /**
     * @param fileName
     */
    public
    void setFileName ( String fileName ) {
        this.fileName = fileName;
    }

    /**
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
    void start () throws IOException {
        ((HiTalkCompiler) compiler).setApplication(this);
        resolver = new HiTalkEngine(new ClauseParser(getInterner()), getInterner(), getCompiler());
        ((HiTalkCompiler) compiler).compileFile(fileName);
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
     *
     */
    @Override
    public
    void banner () {
        System.out.printf("\n%s", "HiTalk system, v0.1-alpha.1, (c) Anton Danilov, 2019, All rights reserved.");
    }

}
