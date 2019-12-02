
package org.ltc.hitalk.term.io;

import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.compiler.VafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HiLogParser;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlDynamicOperatorParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public
class Environment {
    private static Environment instance;

    static {
        try {
            instance = new Environment();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    public static final IFunctor HILOG_APPLY_FUNCTOR = new HtFunctor(HiLogParser.hilogApply, 1, 0);

    private IVafInterner interner;
    private ITermFactory termFactory;
    private PlPrologParser parser;
    private IOperatorTable optable;
    private ICompiler <HtClause, HtPredicate, HtClause> compiler;
    private PredicateTable <HtPredicate> predicateTable;
    private SymbolTable <Integer, String, Object> symbolTable;
    private IResolver <HtPredicate, HtClause> resolver;

    /**
     * @return
     */
    public static Environment instance () {
        return instance;
    }

    /**
     *
     */
    protected final List <HiTalkStream> streams = new ArrayList <>();

    /**
     * @param i
     * @return
     */
    public HiTalkStream getStream ( int i ) {
        return streams.get(i);
    }

    /**
     * @param termFactory
     */
    public void setTermFactory ( ITermFactory termFactory ) {
        this.termFactory = termFactory;
    }

    /**
     * @param optable
     */
    public void setOptable ( IOperatorTable optable ) {
        this.optable = optable;
    }

    /**
     * @param stream
     * @return
     */
    public HiTalkStream addStream ( HiTalkStream stream ) {
        streams.add(stream);
        return stream;
    }

    /**
     * @return
     */
    public HiTalkStream currentInput () {
        return getStream(0);
    }

    /**
     * @return
     */
    public HiTalkStream currentOutput () {
        return getStream(1);
    }

    /**
     * @return
     */
    public HiTalkStream currentError () {
        return getStream(2);
    }

    /**
     * @return
     */
    public IVafInterner getInterner () {
        return interner;
    }

    /**
     * @return
     */
    public ITermFactory getTermFactory () {
        return termFactory;
    }

    /**
     * @return
     */
    public PlPrologParser getParser () {
        return parser;
    }

    /**
     * @return
     */
    public IOperatorTable getOptable () {
        return optable;
    }

    /**
     *
     */
    private Environment () throws IOException {
        streams.add(new HiTalkStream(FileDescriptor.in, true));//  "current_input"
        streams.add(new HiTalkStream(FileDescriptor.out, false));// "current_output"
        streams.add(new HiTalkStream(FileDescriptor.err, false));// "current_error"

        interner = new VafInterner(
                "HiTalk_Variable_Namespace",
                "HiTalk_Functor_Namespace");

        termFactory = new TermFactory(interner);
        optable = new PlDynamicOperatorParser();
        parser = new PlPrologParser(getStream(0), interner, termFactory, optable);
        resolver = new HtResolutionEngine <>(parser, interner, compiler, Environment.instance().getResolver());

        initOptions("org/ltc/hitalk/wam/compiler/startup.pl");
    }

    private void initOptions ( String fn ) {

    }

    /**
     * @param parser
     */
    public void setParser ( PlPrologParser parser ) {
        this.parser = parser;
    }

    /**
     * @param interner
     */
    public void setInterner ( IVafInterner interner ) {
        this.interner = interner;
    }

    public ICompiler <HtClause, HtPredicate, HtClause> getCompiler () {
        return compiler;
    }

    public void setCompiler ( ICompiler <HtClause, HtPredicate, HtClause> compiler ) {
        this.compiler = compiler;
    }

    public PredicateTable <HtPredicate> getPredicateTable () {
        return predicateTable;
    }

    public void setPredicateTable ( PredicateTable <HtPredicate> predicateTable ) {
        this.predicateTable = predicateTable;
    }

    public SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    public IResolver <HtPredicate, HtClause> getResolver () {
        return resolver;
    }

    public void setResolver ( IResolver <HtPredicate, HtClause> resolver ) {
        this.resolver = resolver;
    }
}