package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public class HiTalkWAMCompiler extends PrologWAMCompiler {
    public enum ImplPolicy {
        NATIVE_LOGTALK,   //using specializer
        PROLOG_CONVERSION,//using specializer
        META_INTERPRETATION,
        PROLOG_MODELLING,
        WAM;

    }

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public HiTalkWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               VariableAndFunctorInterner interner,
                               PlPrologParser parser ) {
        super(symbolTable, interner, parser);
    }

//    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
//
//    public HiTalkDefaultBuiltIn getDefbi () {
//        return defbi;
//    }
//
//    private final HiTalkDefaultBuiltIn defbi;
//
//    private HiTalkInstructionCompiler instructionCompiler;
//    private HiTalkPreprocessor <Term, TransformTask <HtClause, Term>> preCompiler;
//    private Resolver <HtClause, HiTalkWAMCompiledQuery> resolver;
//    private Resolver <HtClause, HtClause> resolver2;
//
//    /**
//     * Creates a base machine over the specified symbol table.
//     *
//     * @param symbolTable The symbol table for the machine.
//     * @param interner    r The interner for the machine.
//     * @param parser
//     */
//    public
//    HiTalkWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
//                        VariableAndFunctorInterner interner,
//                        HtPrologParser parser ) throws LinkageException {
//        super(symbolTable, interner, parser);
//        defbi = new HiTalkDefaultBuiltIn(symbolTable, interner);
//        instructionCompiler = new HiTalkInstructionCompiler(symbolTable, interner, defbi);
//        getPreCompiler().getCompilerObserver(new ClauseChainObserver());
//    }
//
//    public HiTalkPreCompiler getPreCompiler () throws LinkageException {
//        if (preCompiler == null) {
//            preCompiler = new HiTalkPreprocessor <>(
//                    getSymbolTable(),
//                    getInterner(),
//                    getDefaultBuiltIn(),
//                    getResolver2(),
//                    this);
//        }
//
//        return preCompiler;
//    }
//
//    private
//    HiTalkDefaultBuiltIn getDefaultBuiltIn () {
//        return new HiTalkDefaultBuiltIn(getSymbolTable(), getInterner());
//    }
//
//    HiTalkInstructionCompiler getInstructionCompiler () {
//        if (instructionCompiler == null) {
//            instructionCompiler = new HiTalkInstructionCompiler(
//                    getSymbolTable(),
//                    getInterner(),
//                    new HiTalkDefaultBuiltIn(getSymbolTable(), getInterner()));
//        }
//        return instructionCompiler;
//    }
//
//    /**
//     * Establishes an observer on the compiled forms that the compiler outputs.
//     *
//     * @param observer The compiler output observer.
//     */
//    @Override
//    public
//    void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {
//        instructionCompiler.setCompilerObserver(observer);
//    }
//
//    /**
//     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
//     *
//     * @param sentence The sentence to compile.
//     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
//     */
//    @Override
//    public
//    void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
//        getPreCompiler().compile(sentence);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    public
//    void endScope () throws SourceCodeException {
//        preCompiler.endScope();
//        instructionCompiler.endScope();
//    }
//
//    /**
//     * @return
//     */
//    @Override
//    public
//    Logger getConsole () {
//        return logger;
//    }

//    /**
//     * @return
//     */
//    @Override
//    public
//    HtPrologParser getParser () {
//        return parser;
//    }

    /**
     * @param rule
     */
    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    /**
     * @param query
     */
    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    /**
     * @param clause
     */
    @Override
    public void compileClause ( HtClause clause ) {

    }


//    public void compileFile ( File file ) throws IOException, SourceCodeException {
//        compile(PlTokenSource.getTokenSourceForIoFile(file));
//    }
}
