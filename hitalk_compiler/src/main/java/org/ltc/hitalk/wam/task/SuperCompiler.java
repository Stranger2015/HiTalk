package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.ptree.ProcessTree;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;
import org.ltc.hitalk.wam.transformers.IGeneralizer;
import org.ltc.hitalk.wam.transformers.IInliner;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SuperCompiler extends PrologPreCompiler <HtClause, HtPredicate, HtClause>
        implements IInliner, ISpecializer, IGeneralizer, ITransformer {

    private ProcessTree pTree;

    /**
     *
     */
    public SuperCompiler() throws Exception {
        super();
    }

    /**
     * @param symbolTable
     * @param interner
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     * @param parser
     */
    public SuperCompiler ( ISymbolTable <Integer, String, Object> symbolTable, IVafInterner interner, PrologDefaultBuiltIn defaultBuiltIn, PrologBuiltInTransform <HtClause, HtPredicate, HtClause> builtInTransform, IResolver <HtPredicate, HtClause> resolver, PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }

    public ProcessTree buildProcessTree ( ITerm term ) {
//        repeat,
//                unprocessed_node( ProcessTree, Beta )-> true; ignore(fail),
//                relevant_ancestors( Ancestors, Beta ),
//                found_in_ancestors( Ancestors, Beta, Alpha ) -> true; produce(Beta),
//                equivalent( Alpha, Beta ) ->
//        fold(ProcessTree, Alpha, Beta)
//        ;
//        (context(Alpha, ContextA),
//                context( Beta, ContextB )),
//        more_restrictive_than( ContextA, ContextB )->
//        (replace_node( ProcessTree, Alpha, ChildNode ),
//                context(ChildNode, true))%fixme
//        ;
//        instance_of( Alpha, Beta ) ->
//        make_abstraction( ProcessTree, Beta, Alpha )
//        ;
//        incommensurable( ProcessTree, Beta )->
//        split( ProcessTree, Beta )
//        ;
//        make_abstraction( ProcessTree, Alpha, Beta ),
//                fail.
//
//                        unprocessed_node( ProcessTree, Beta ) :-
//                unprocessed_nodes( ProcessTree, Nodes),
//                Nodes == [] -> fail.%fixme


        return pTree;
    }

    /**
     *
     */
    void mainLoop () {

    }

    /**
     *
     */
    void generateCode () {

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run () {

    }

    /**
     *
     */
    public void cancel () {

    }


//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    ITerm generalize ( ITerm clause ) {
//        return null;
//    }

//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    ITerm inline ( ITerm clause ) {
//        return null;
//    }
//
//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    ITerm specialize ( ITerm clause ) {
//        return null;
//    }

//    /**
//     * Applies a transformation to the clause.
//     *
//     * @param clause The clause to transform.
//     * @return A clause which is a transformation of the argument.
//     */
////    @Override
//    public
//    ITerm transform ( HtClause clause ) {
//        return null;
////    }
//
//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    HtClause generalize ( HtClause clause ) {
//        return null;
//    }
//
//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    HtClause specialize ( HtClause clause ) {
//        return null;
//    }
////
//    /**
//     * Applies a transformation to the clause.
//     *
//     * @param clause The clause to transform.
//     * @return A clause which is a transformation of the argument.
//     */
//    @Override
//    public
//    HtClause transform ( HtClause clause ) {
//        return null;
//    }

    /**
     *
     */
    enum TransformMode {
        GENERALIZE_MODE,
        INLINE_MODE,
        SPECIALIZE_MODE
    }

    private TransformMode mode;


    /**
     * @param clause
     * @return
     */
    @Override
    public List <ITerm> inline ( ITerm clause ) {
        return Collections.singletonList(clause);
    }

    /**
     * @param clause
     * @return
     */
    @Override
    public List <ITerm> generalize ( ITerm clause ) {
        return Collections.singletonList(clause);
    }

    /**
     * @return
     */
    public ExecutionContext getContext () {
        return null;
    }

    /**
     * @param context
     */
    public void setContext ( ExecutionContext context ) {

    }

    /**
     * @param max
     * @return
     */
    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
//    @Override
    public List <ITerm> transform ( ITerm clause ) {
        List <ITerm> newClause;
        switch (mode) {
            case INLINE_MODE:
                newClause = inline(clause);
                break;
            case SPECIALIZE_MODE:
                newClause = specialize(clause);
                break;
            case GENERALIZE_MODE:
                newClause = generalize(clause);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }

        return newClause;
    }

    /**
     * @param clause
     * @return
     */
    @Override
    public List <ITerm> specialize ( ITerm clause ) {
        return Collections.singletonList(clause);
    }

    public
    void setMode ( TransformMode mode ) {
        this.mode = mode;
    }
}
