package org.ltc.hitalk.wam.compiler.hitalk;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.task.StandardPreprocessor;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class HiTalkPreprocessor<T extends ITerm, TT extends TransformTask <T>>
        extends HiTalkPreCompiler {

    protected final DefaultTransformer <T> defaultTransformer;
    //    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    //    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TT> components = new ArrayList <>();
    //    protected final Function <TC, List <TC>> defaultAction;

    @Override
    public LogicCompilerObserver <HtPredicate, HtClause> getObserver () {
        return observer;
    }

    protected LogicCompilerObserver <HtPredicate, HtClause> observer;
    protected List <HtClause> preCompiledTarget;
    protected PlPrologParser parser;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param resolver
     */
    public
    HiTalkPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                         IVafInterner interner,
                         PrologBuiltInTransform builtInTransform,
                         PrologDefaultBuiltIn defaultBuiltIn,
                         Resolver <HtPredicate, HtClause> resolver,
                         PlPrologParser parser )
            throws LinkageException {

        super(symbolTable, interner, builtInTransform, defaultBuiltIn, resolver, parser);

//        this.builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);

        defaultTransformer = new DefaultTransformer <>(null);

//        int i = interner.internFunctorName(BEGIN_OF_FILE, 0);
//        Term target = new Functor(i, Atom.EMPTY_TERM_ARRAY);
//        defaultAction = new Function <TC, List <TC>>() {
//            DcgRuleExpander ruleExpander
//            @Override
//            public
//            List <TC> apply ( TC tc ) {
//                return null;
//            }
//        };
        if (preCompiledTarget != null) {
            for (HtClause t : preCompiledTarget) {
                resolver.setQuery(t);
                resolver.resolve();
            }
        }

        components.add((TT) new DefaultTermExpander(preCompiledTarget, defaultTransformer));
//        components.add((TT) new HiLogPreprocessor <>(null, defaultTransformer, interner));
        components.add((TT) new StandardPreprocessor(null, preCompiledTarget, defaultTransformer));
    }

    /**
     * @param clauses
     */
    @Override
    protected
    void saveResult ( List <HtClause> clauses ) {
        preCompiledTarget = clauses;
    }

    protected List <HtClause> preprocess ( HtClause t ) {
        return null;
    }

    /**
     * @param t
     * @return
     */
    protected List <T> preprocess ( T t ) {
        List <T> list = new ArrayList <>();

        components.stream().map(task -> task.invoke(t)).forEach(list::addAll);

        return list;
    }

    private
    void initialize () {

    }

    /**
     * @param t
     */
//    @Override
    public
    void add ( TT t ) {
        components.add(t);
    }

    //    @Override
    public
    List <TT> getComponents () {
        return components;
    }
}
