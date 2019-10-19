package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.Tools;

import static org.ltc.hitalk.wam.compiler.Language.HITALK;
import static org.ltc.hitalk.wam.compiler.Tools.INTERPRETER;

/**
 *
 */
public
class AstInterpreterApp<T extends HtClause, P, Q> extends BaseApplication <T, P, Q> {

    /**
     * @param arg
     */
    public AstInterpreterApp ( String arg ) {
        setFileName(arg);
    }

    public static void main ( String[] args ) {
        AstInterpreterApp app = new AstInterpreterApp(args[0]);
        try {
//            app.setConfig(null);
//            app.setFileName(args[0]);
            app.init();
            app.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public String namespace ( String varOrFunctor ) {
//        return "HiTalk"
//    }

    //    @Override
    public BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> createWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, PlPrologParser parser ) {
        return null;
    }

    @Override
    public String namespace ( String varOrFunctor ) {
        return null;
    }

    @Override
    public void undoInit () {

    }

    @Override
    public void shutdown () {

    }

    /**
     *
     */
    @Override
    public void doStart () {

    }

    @Override
    public IProduct product () {
        return null;
    }

    @Override
    public Language language () {
        return HITALK;
    }

    @Override
    public Tools tool () {
        return INTERPRETER;
    }
}
