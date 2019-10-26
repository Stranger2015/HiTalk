package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.wam.task.TransformTask;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PrologPreprocessor<TC extends Term, TT extends TransformTask <HtClause, TC>> extends PrologPreCompiler {
    protected final List <TT> components = new ArrayList <>();

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable        The symbol table for the machine.
     * @param interner           The interner for the machine.
     */
    public PrologPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                                VariableAndFunctorInterner interner,
                                PrologDefaultBuiltIn defaultBuiltIn,
                                PrologBuiltInTransform <IApplication, T> builtInTransform,
                                Resolver <HtClause, HtClause> resolver,
                                PrologWAMCompiler compiler ) {

        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, compiler);
    }

    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException, OperationNotSupportedException {

    }

    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    @Override
    public void compileClause ( HtClause clause ) {

    }

    @Override
    public void setResolver ( Resolver <HtClause, HtClause> resolver ) {

    }

    @Override
    public void compile ( String fileName, HtProperty[] flags ) {

    }

    @Override
    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {

    }

    @Override
    public void setCompilerObserver ( LogicCompilerObserver <HtPredicate, HtClause> observer ) {

    }

    @Override
    public void endScope () throws SourceCodeException {

    }
}
