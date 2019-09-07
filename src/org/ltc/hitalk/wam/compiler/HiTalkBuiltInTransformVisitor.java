package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.BasePositionalVisitor;
import com.thesett.aima.logic.fol.PositionalTermVisitor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.HiTalkBuiltInTransform;

/**
 *
 */
public
class HiTalkBuiltInTransformVisitor extends BasePositionalVisitor
        implements PositionalTermVisitor {

    protected final HiTalkBuiltInTransform builtInTransform;

    /**
     * @param symbolTable
     * @param interner
     * @param termTraverser
     * @param builtInTransform
     */
    public
    HiTalkBuiltInTransformVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                    VariableAndFunctorInterner interner,
                                    PositionalTermTraverser termTraverser,
                                    HiTalkBuiltInTransform builtInTransform ) {
        super(interner, symbolTable, termTraverser);
        this.builtInTransform = builtInTransform;
    }


    /**
     * Sets up the positional term traverser used to traverse the term being visited, providing a positional context as
     * it does so.
     *
     * @param traverser The positional term traverser used to traverse the term being visited.
     */
    @Override
    public
    void setPositionalTraverser ( PositionalTermTraverser traverser ) {
        this.traverser = traverser;
    }

    /**
     * Visits a term.
     *
     * @param term The term to visit.
     */
    @Override
    public
    void visit ( Term term ) {
        lkBui
    }

    /**
     * @return
     */
    public
    HiTalkBuiltInTransform getBuiltInTransform () {
        return builtInTransform;
    }
}

