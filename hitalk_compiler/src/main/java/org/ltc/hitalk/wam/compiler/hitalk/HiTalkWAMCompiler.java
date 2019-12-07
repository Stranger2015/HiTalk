package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

/**
 * logtalk_library_path(logtalk_third_party_libraries, home('Documents/Logtalk/logtalk_third_party_libraries/')).
 * logtalk_library_path(Project, logtalk_third_party_libraries(ProjectPath)) :-
 * os::directory_files('$HOME/Documents/Logtalk/logtalk_third_party_libraries', Projects, [type(directory), dot_files(false)]),
 * list::member(Project, Projects),
 * atom_concat(Project, '/', ProjectPath).
 *
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class HiTalkWAMCompiler<T extends HtMethod, P, Q, PC, QC> extends PrologWAMCompiler <T, P, Q, PC, QC> {
    public HiTalkWAMCompiler () {

    }

    /**
     *
     */
    public enum ImplPolicy {
        NATIVE_LOGTALK,   //using specializer
        PROLOG_CONVERSION,//using specializer
        META_INTERPRETATION,
        PROLOG_MODELLING,//sicstus
        WAM_EXTENSION,
        ;
    }

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public HiTalkWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PlPrologParser parser,
                               LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }
}
