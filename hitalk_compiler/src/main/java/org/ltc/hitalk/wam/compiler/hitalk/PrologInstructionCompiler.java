package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermUtils;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.TermWalker;
import com.thesett.aima.logic.fol.wam.builtins.Cut;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;
import org.ltc.hitalk.wam.compiler.HtPositionalTermTraverserImpl;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;

import java.util.*;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_ALLOCATION;
import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_PERM_VARS_REMAINING;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.STACK_ADDR;

public class PrologInstructionCompiler<T, Q> extends BaseInstructionCompiler <T, Q> {
    private final PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param parser
     */
    public PrologInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                       VariableAndFunctorInterner interner,
                                       PrologDefaultBuiltIn defaultBuiltIn,
                                       PlPrologParser parser ) {
        super(symbolTable, interner, parser);
        this.defaultBuiltIn = defaultBuiltIn;
    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    @Override
    public void endScope () throws SourceCodeException {

    }
}

    /**
     * Allocates stack slots where needed to the variables in a program clause. The algorithm here is fairly complex.
     * <p>
     * <p/>A clause head and first body functor are taken together as the first unit, subsequent clause body functors
     * are taken as subsequent units. A variable appearing in more than one unit is said to be permanent, and must be
     * stored on the stack, rather than a register, otherwise the register that it occupies may be overwritten by calls
     * to subsequent units. These variables are called permanent, which really means that they are local variables on
     * the call stack.
     * <p>
     * <p/>In addition to working out which variables are permanent, the variables are also ordered by reverse position
     * of last body of occurrence, and assigned to allocation slots in this order. The number of permanent variables
     * remaining at each body call is also calculated and recorded against the body functor in the symbol table using
     * column {@link SymbolTableKeys#SYMKEY_PERM_VARS_REMAINING}. This allocation ordering of the variables and the
     * count of the number remaining are used to implement environment trimming.
     *
     * @param clause The clause to allocate registers for.
     */
    private void allocatePermanentProgramRegisters ( HtClause clause ) {
        // A bag to hold variable occurrence counts in.
        Map <Variable, Integer> variableCountBag = new HashMap <>();

        // A mapping from variables to the body number in which they appear last.
        Map <Variable, Integer> lastBodyMap = new HashMap <>();

        // Holds the variable that are in the head and first clause body argument.
        Collection <Variable> firstGroupVariables = new HashSet <>();

        // Get the occurrence counts of variables in all clauses after the initial head and first body grouping.
        // In the same pass, pick out which body variables last occur in.
        if ((clause.getBody() != null)) {
            for (int i = clause.getBody().length - 1; i >= 1; i--) {
                Set <Variable> groupVariables = TermUtils.findFreeVariables(clause.getBody()[i]);

                // Add all their counts to the bag and update their last occurrence positions.
                for (Variable variable : groupVariables) {
                    Integer count = variableCountBag.get(variable);
                    variableCountBag.put(variable, (count == null) ? 1 : (count + 1));

                    if (!lastBodyMap.containsKey(variable)) {
                        lastBodyMap.put(variable, i);
                    }

                    // If the cut level variable is seen, automatically add it to the first group variables,
                    // so that it will be counted as a permanent variable, and assigned a stack slot. This
                    // will only occur for deep cuts, that is where the cut comes after the first group.
                    if (variable instanceof Cut.CutLevelVariable) {
                        firstGroupVariables.add(variable);
                    }
                }
            }
        }

        // Get the set of variables in the head and first clause body argument.
        if (clause.getHead() != null) {
            Set <Variable> headVariables = TermUtils.findFreeVariables(clause.getHead());
            firstGroupVariables.addAll(headVariables);
        }

        if ((clause.getBody() != null) && (clause.getBody().length > 0)) {
            Set <Variable> firstArgVariables = TermUtils.findFreeVariables(clause.getBody()[0]);
            firstGroupVariables.addAll(firstArgVariables);
        }

        // Add their counts to the bag, and set their last positions of occurrence as required.
        for (Variable variable : firstGroupVariables) {
            Integer count = variableCountBag.get(variable);
            variableCountBag.put(variable, (count == null) ? 1 : (count + 1));

            if (!lastBodyMap.containsKey(variable)) {
                lastBodyMap.put(variable, 0);
            }
        }

        // Sort the variables by reverse position of last occurrence.
        List <Map.Entry <Variable, Integer>> lastBodyList = new ArrayList <>(lastBodyMap.entrySet());
        lastBodyList.sort(( o1, o2 ) -> o2.getValue().compareTo(o1.getValue()));

        // Holds counts of permanent variable last appearances against the body in which they last occur.
        int[] permVarsRemainingCount = new int[(clause.getBody() != null) ? clause.getBody().length : 0];

        // Search the count bag for all variable occurrences greater than one, and assign them to stack slots.
        // The variables are examined by reverse position of last occurrence, to ensure that later variables
        // are assigned to lower permanent allocation slots for environment trimming purposes.
        for (Map.Entry <Variable, Integer> entry : lastBodyList) {
            Variable variable = entry.getKey();
            Integer count = variableCountBag.get(variable);
            int body = entry.getValue();

            if ((count != null) && (count > 1)) {
                /*log.fine("Variable " + variable + " is permanent, count = " + count);*/

                int allocation = (numPermanentVars++ & (0xff)) | (STACK_ADDR << 8);
                symbolTable.put(variable.getSymbolKey(), SYMKEY_ALLOCATION, allocation);

                // Check if the variable is the cut level variable, and cache its stack slot in 'cutLevelVarSlot', so that
                // the clause compiler knows which variable to use for the get_level instruction.
                if (variable instanceof Cut.CutLevelVariable) {
                    //cutLevelVarSlot = allocation;
                    cutLevelVarSlot = numPermanentVars - 1;
                    /*log.fine("cutLevelVarSlot = " + cutLevelVarSlot);*/
                }

                permVarsRemainingCount[body]++;
            }
        }

        // Roll up the permanent variable remaining counts from the counts of last position of occurrence and
        // store the count of permanent variables remaining against the body.
        int permVarsRemaining = 0;

        for (int i = permVarsRemainingCount.length - 1; i >= 0; i--) {
            symbolTable.put(clause.getBody()[i].getSymbolKey(), SYMKEY_PERM_VARS_REMAINING, permVarsRemaining);
            permVarsRemaining += permVarsRemainingCount[i];
        }
    }

    /**
     * Allocates stack slots to all free variables in a query clause.
     * <p>
     * <p/>At the end of processing a query its variable bindings are usually printed. For this reason all free
     * variables in a query are marked as permanent variables on the call stack, to ensure that they are preserved.
     *
     * @param clause   The clause to allocate registers for.
     * @param varNames A map of permanent variables to variable names to record the allocations in.
     */
    private void llocatePermanentQueryRegisters ( Term clause, Map <Byte, Integer> varNames ) {
        // Allocate local variable slots for all variables in a query.
        HiTalkInstructionCompiler.QueryRegisterAllocatingVisitor allocatingVisitor = new HiTalkInstructionCompiler.QueryRegisterAllocatingVisitor(symbolTable, varNames, null);

        HtPositionalTermTraverser positionalTraverser = new HtPositionalTermTraverserImpl();
        positionalTraverser.setContextChangeVisitor(allocatingVisitor);

        TermWalker walker = new TermWalker(new DepthFirstBacktrackingSearch <>(), positionalTraverser, allocatingVisitor);

        walker.walk(clause);
    }
