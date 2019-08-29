package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkAstCompiler;
import org.ltc.hitalk.wam.compiler.HiTalkCompilerApp;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.File;

public
class HiTalkAstInterpreterApp implements IApplication {
    /**
     * @return
     */
    @Override
    public
    IConfig getConfig () {
        return null;
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

    }

    @Override
    public
    void setParser ( HtPrologParser parser ) {

    }

    public static
    void main ( String[] args ) {
        try {
            SymbolTable <Integer, String, Object> symbolTable = new SymbolTableImpl <>();
            VariableAndFunctorInterner interner = new VariableAndFunctorInternerImpl("HiTalk_Variable_Namespace", "HiTalk_Functor_Namespace");
            HtTokenSource tokenSource = HtTokenSource.getTokenSourceForFile(new File(args[0]));
            HtPrologParser parser = new HiTalkParser(tokenSource, interner);
            HiTalkAstCompiler compiler = new HiTalkAstCompiler(symbolTable, interner, parser);
            HtResolutionEngine <HtClause, HtClause> engine = new HtResolutionEngine(parser, interner, compiler)
            HiTalkDefaultBuiltIn defaultBuiltIn = new HiTalkDefaultBuiltIn(symbolTable, interner);

            HiTalkCompilerApp app = new HiTalkCompilerApp(symbolTable, interner, parser, compiler, defaultBuiltIn);
            app.setFileName(args[0]);
//            app.createFlags(app.loadContext, DEFAULT_SCRATCH_DIRECTORY);
            app.setParser(parser);
            //
            app.banner();
            app.start();

        } catch (Exception e) {
//            e.printStackTrace();
            throw new ExecutionError(ExecutionError.Kind.TYPE_ERROR, null);
        }
    }
}
