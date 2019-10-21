package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PrologPreprocessor<TC extends Term, TT extends TransformTask <HtClause, TC>> extends PrologPreCompiler {
    protected final DefaultTransformer <HtClause, TC> defaultTransformer;
    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    //    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TT> components = new ArrayList <>();
    //    protected final Function <TC, List <TC>> defaultAction;
    protected Resolver <HtClause, HtClause> resolver;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable        The symbol table for the machine.
     * @param interner           The interner for the machine.
     * @param defaultTransformer
     * @param defaultBuiltIn
     */
    public PrologPreprocessor ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, DefaultTransformer <HtClause, TC> defaultTransformer, HiTalkDefaultBuiltIn defaultBuiltIn ) {
        super(symbolTable, interner);
        this.defaultTransformer = defaultTransformer;
        this.defaultBuiltIn = defaultBuiltIn;
    }

    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    @Override
    public void compile ( String fileName, HtProperty[] flags ) {

    }
}
