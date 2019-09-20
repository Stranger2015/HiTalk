package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.core.ptree.ProcessTree;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.transformers.IGeneralizer;
import org.ltc.hitalk.wam.transformers.IInliner;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.List;

/**
 *
 */
public
class SuperCompiler<T extends HtClause, TC extends Term> extends StandardPreprocessor <T, TC>
        implements IInliner <T, TC>, ISpecializer <T, TC>, IGeneralizer <T, TC>, ITransformer <T, TC> {

    private ProcessTree pTree;

    /**
     * @param target
     * @param transformer
     */
    public
    SuperCompiler ( List <TC> target, ITransformer transformer ) {
        super(null, target, transformer);
    }

    public
    ProcessTree buildProcessTree ( T term ) {
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

//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    T generalize ( T clause ) {
//        return null;
//    }

//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    T inline ( T clause ) {
//        return null;
//    }
//
//    /**
//     * @param clause
//     * @return
//     */
//    @Override
//    public
//    T specialize ( T clause ) {
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
//    T transform ( HtClause clause ) {
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
    public
    T generalize ( T clause ) {
        return clause;
    }

    /**
     * @param clause
     * @return
     */
    @Override
    public
    T inline ( T clause ) {
        return clause;
    }

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    public
    T transform ( T clause ) {
        T newClause = clause;
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
    public
    T specialize ( T clause ) {
        return clause;
    }


    public
    void setMode ( TransformMode mode ) {
        this.mode = mode;
    }

}
