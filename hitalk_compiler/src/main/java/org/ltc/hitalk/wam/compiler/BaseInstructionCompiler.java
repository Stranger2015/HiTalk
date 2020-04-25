/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.SizeableList;
import com.thesett.common.util.doublemaps.SymbolKey;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.hitalk.*;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.IPrologBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn.VarIntroduction;
import org.ltc.hitalk.wam.printer.*;
import org.ltc.hitalk.wam.task.PreCompilerTask;

import java.util.*;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.*;
import static com.thesett.aima.search.util.Searches.allSolutions;
import static org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction.HiTalkWAMInstructionSet.*;
import static org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction.REG_ADDR;
import static org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction.STACK_ADDR;

/**
 * BaseMachine provides a base for implementing abstract machines components, such as compilers, interpreters, byte code
 * interpreters and so on, on top of. It encapsulates an extensible symbol table, that allows the mapping of arbitrary
 * fields against symbols and the ability to nest symbols within the scope of other symbols. The symbols may be the
 * interned names of functors or variables in the language.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide a symbol table in which arbitrary fields can be held against symbols in the language.
 * <tr><td> Provide an interner to intern variable and functor names with.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class BaseInstructionCompiler<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate,
        QC extends HiTalkWAMCompiledQuery>
        extends BaseCompiler<T, P, Q, PC, QC> {

    protected ICompilerObserver<P, Q> observer;
    protected final PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the instruction optimizer.
     */
    protected final HiTalkWAMOptimizer optimizer;

    /**
     * Holds a list of all predicates encountered in the current scope.
     */
    protected final Deque<SymbolKey> predicatesInScope = new ArrayDeque<>();

    /**
     * This is used to keep track of the number of permanent variables.
     */
    protected int numPermanentVars;

    /**
     * This is used to keep track of the position of the cut level variable, for deep cuts, if there is one. <tt>-1</tt>
     * means no deep cut exists in the clause, and a value gte to zero means there is one, and references its slot.
     */
    protected int cutLevelVarSlot = -1;

    /**
     * Keeps count of the current compiler scope, to keep symbols in each scope fresh.
     */
    protected int scope;

    /**
     * Holds the current nested compilation scope symbol table.
     */
    protected ISymbolTable<Integer, String, Object> scopeTable;
    protected Collection<Integer> seenRegisters;
    protected int lastAllocatedTempReg;

    /**
     * @return
     */
    public Deque<PreCompilerTask<HtClause>> getTasks() {
        return tasks;
    }

    /**
     *
     */
    public final Deque<PreCompilerTask<HtClause>> tasks = new ArrayDeque<>();

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public BaseInstructionCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                                   IVafInterner interner,
                                   PrologDefaultBuiltIn defaultBuiltIn,
                                   ICompilerObserver<P, Q> observer,
                                   HtPrologParser parser) {
        super(symbolTable, interner, parser, observer);
        optimizer = new HiTalkWAMOptimizer(symbolTable, interner);
        this.defaultBuiltIn = defaultBuiltIn;

    }

    /**
     * Compiles a clause as a query. The clause should have no head, only a body.
     *
     * @param clause lause The clause to compile as a query.
     * @throws HtSourceCodeException If there is an error in the source code preventing its compilation.
     */
    public void compileQuery(HtClause clause) throws Exception {
        if (!checkDirective(clause)) {
            handleDirective(clause);
            return;
        }
        // Used to build up the compiled result in.
        HiTalkWAMCompiledQuery result;

        // A mapping from top stack frame slots to interned variable names is built up in this.
        // This is used to track the stack positions that variables in a query are assigned to.
        Map<Byte, Integer> varNames = new TreeMap<>();

        // Used to keep track of registers as they are seen during compilation. The first time a variable is seen,
        // a variable is written onto the heap, subsequent times its value. The first time a functor is seen,
        // its structure is written onto the heap, subsequent times it is compared with.
        seenRegisters = new TreeSet<>();

        // This is used to keep track of the next temporary register available to allocate.
        lastAllocatedTempReg = findMaxArgumentsInClause(clause);

        // This is used to keep track of the number of permanent variables.
        numPermanentVars = 0;

        // This is used to keep track of the allocation slot for the cut level variable, when needed. -1 means it is
        // not needed, so it is initialized to this.
        cutLevelVarSlot = -1;

        // These are used to generate pre and post instructions for the clause, for example, for the creation and
        // clean-up of stack frames.
        SizeableList<HiTalkWAMInstruction> preFixInstructions = new SizeableLinkedList<>();
        SizeableList<HiTalkWAMInstruction> postFixInstructions = new SizeableLinkedList<>();

        // Find all the free non-anonymous variables in the clause.
        Set<HtVariable> freeVars = TermUtilities.findFreeNonAnonVariables(clause.getT());
        Set<Integer> freeVarNames = new TreeSet<>();

        for (HtVariable var : freeVars) {
            freeVarNames.add(var.getName());
        }

        // Allocate permanent variables for a query. In queries all variables are permanent so that they are preserved
        // on the stack upon completion of the query.
        allocatePermanentQueryRegisters(clause.getT(), varNames);

        // Gather information about the counts and positions of occurrence of variables and constants within the clause.
        gatherPositionAndOccurrenceInfo(clause.getT());

        result = new HiTalkWAMCompiledQuery(varNames, freeVarNames);

        // Generate the prefix code for the clause. Queries require a stack frames to hold their BaseApp.
        /*log.fine("ALLOCATE " + numPermanentVars);*/
        preFixInstructions.add(new HiTalkWAMInstruction(AllocateN, REG_ADDR, (byte) (numPermanentVars & 0xff)));

        // Deep cuts require the current choice point to be kept in a permanent variable, so that it can be recovered
        // once deeper choice points or BaseApps have been reached.
        if (cutLevelVarSlot >= 0) {
            /*log.fine("GET_LEVEL "+ cutLevelVarSlot);*/
            preFixInstructions.add(new HiTalkWAMInstruction(GetLevel, STACK_ADDR, (byte) cutLevelVarSlot));
        }

        result.addInstructions(preFixInstructions);

        // Compile all of the conjunctive parts of the body of the clause, if there are any.
        ListTerm expressions = clause.getT().getBody();

        // The current query does not have a name, so invent one for it.
        HtFunctorName fn = new HtFunctorName("tq", 0);

        for (int i = 0; i < expressions.size(); i++) {
            IFunctor goal = (IFunctor) expressions.getHead(i);
            boolean isFirstBody = i == 0;

            // Select a non-default built-in implementation to compile the functor with, if it is a built-in.
            IPrologBuiltIn builtIn = goal instanceof IPrologBuiltIn ? (IPrologBuiltIn) goal : defaultBuiltIn;

            // The 'isFirstBody' parameter is only set to true, when this is the first functor of a rule, which it
            // never is for a query.
            SizeableLinkedList<HiTalkWAMInstruction> instructions = builtIn.compileBodyArguments(goal, false, fn, i);
            result.addInstructions(goal, instructions);

            // Queries are never chain rules, and as all permanent variables are preserved, bodies are never called
            // as last calls.
            instructions = builtIn.compileBodyCall(goal, isFirstBody, false, false, numPermanentVars);
            result.addInstructions(goal, instructions);
        }

        // Generate the postfix code for the clause.
        /*log.fine("DEALLOCATE");*/
        postFixInstructions.add(new HiTalkWAMInstruction(Suspend));
        postFixInstructions.add(new HiTalkWAMInstruction(Deallocate));

        result.addInstructions(postFixInstructions);

        // Run the optimizer on the output.
        result = optimizer.apply(result);

        displayCompiledQuery(result);

        observer.onQueryCompilation((Q) result);
    }

    private void handleDirective(HtClause clause) {

    }

    /**
     * Gather information about variable counts and positions of occurrence of constants and variable within a clause.
     *
     * @param clause The clause to check the variable occurrence and position of occurrence within.
     */
    private void gatherPositionAndOccurrenceInfo(ITerm clause) {
        IPositionalTermTraverser positionalTraverser = new HtPositionalTermTraverser();
        HtPositionAndOccurrenceVisitor positionAndOccurrenceVisitor =
                new HtPositionAndOccurrenceVisitor(getSymbolTable(), interner, positionalTraverser);
        positionalTraverser.setContextChangeVisitor(positionAndOccurrenceVisitor);

        HtTermWalker walker = new HtTermWalker(new DepthFirstBacktrackingSearch<>(), positionalTraverser,
                positionAndOccurrenceVisitor);

        walker.walk(clause);
    }

    /**
     * Pretty prints a compiled query.
     *
     * @param query The compiled query to pretty print.
     */
    private void displayCompiledQuery(ITerm query) {
        // Pretty print the clause.
        StringBuilder result = new StringBuilder();

        IPositionalTermVisitor displayVisitor = new HtWAMCompiledQueryPrintingVisitor(getSymbolTable(), interner, result);

        HtTermWalkers.positionalWalker(displayVisitor).walk(query);

        /*log.fine(result.toString());*/
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
    private void allocatePermanentQueryRegisters(ITerm clause, Map<Byte, Integer> varNames) {
        // Allocate local variable slots for all variables in a query.
        QueryRegisterAllocatingVisitor allocatingVisitor = new QueryRegisterAllocatingVisitor(getSymbolTable(),
                varNames,
                null);

        HtPositionalTermTraverser positionalTraverser = new HtPositionalTermTraverser();
        positionalTraverser.setContextChangeVisitor(allocatingVisitor);

        HtTermWalker walker = new HtTermWalker(new DepthFirstBacktrackingSearch<>(), positionalTraverser, allocatingVisitor);
        walker.walk(clause);
    }

    private boolean checkDirective(HtClause clause) {
        if (clause instanceof Directive) {
            IFunctor body = ((Directive) clause).getDef();
            final Directive.DirectiveKind kind = ((Directive) clause).getKind();
        }
        return false;
    }

    /**
     * Compiles a program clause, and adds its instructions to a compiled predicate.
     *
     * @param clause            The source clause to compile.
     * @param compiledPredicate The predicate to add instructions to.
     * @param isFirst           <tt>true</tt> iff the clause is the first in the predicate.
     * @param isLast            <tt>true</tt> iff the clause is the last in the predicate.
     * @param multipleClauses   <tt>true</tt> iff the predicate contains >1 clause.
     * @param clauseNumber      The position of the clause within the predicate.
     */
    protected void compileClause(T clause,
                                 P compiledPredicate,
                                 boolean isFirst,
                                 boolean isLast,
                                 boolean multipleClauses,
                                 int clauseNumber) throws Exception {
        // Used to build up the compiled clause in.
        HiTalkWAMCompiledClause result = new HiTalkWAMCompiledClause(clause.getHead(), clause.getBody(),
                (HiTalkWAMCompiledPredicate) compiledPredicate);

        // Check if the clause to compile is a fact (no body).
        boolean isFact = clause.getBody() == null;

        // Check if the clause to compile is a chain rule, (one called body).
        boolean isChainRule = (clause.getBody() != null) && (clause.getBody().size() == 1);

        // Used to keep track of registers as they are seen during compilation. The first time a variable is seen,
        // a variable is written onto the heap, subsequent times its value. The first time a functor is seen,
        // its structure is written onto the heap, subsequent times it is compared with.
        seenRegisters = new TreeSet<>();

        // This is used to keep track of the next temporary register available to allocate.
        lastAllocatedTempReg = findMaxArgumentsInClause(clause);

        // This is used to keep track of the number of permanent variables.
        numPermanentVars = 0;

        // This is used to keep track of the allocation slot for the cut level variable, when needed. -1 means it is
        // not needed, so it is initialized to this.
        cutLevelVarSlot = -1;

        // These are used to generate pre and post instructions for the clause, for example, for the creation and
        // clean-up of stack frames.
        SizeableList<HiTalkWAMInstruction> preFixInstructions = new SizeableLinkedList<>();
        SizeableList<HiTalkWAMInstruction> postFixInstructions = new SizeableLinkedList<>();

        // Find all the free non-anonymous variables in the clause.
        Set<HtVariable> freeVars = TermUtilities.findFreeNonAnonVariables(clause);
        Collection<Integer> freeVarNames = new TreeSet<>();

        for (HtVariable var : freeVars) {
            freeVarNames.add(var.getName());
        }

        // Allocate permanent variables for a program clause. Program clauses only use permanent variables when really
        // needed to preserve variables across calls.
        allocatePermanentProgramRegisters(clause);

        // Gather information about the counts and positions of occurrence of variables and constants within the clause.
        gatherPositionAndOccurrenceInfo(clause);

        // Labels the entry point to each choice point.
        HtFunctorName fn = interner.getFunctorFunctorName(clause.getHead());
        HtWAMLabel entryLabel = new HtWAMLabel(fn, clauseNumber);

        // Label for the entry point to the next choice point, to backtrack to.
        HtWAMLabel retryLabel = new HtWAMLabel(fn, clauseNumber + 1);

        // Create choice point instructions for the clause, depending on its position within the containing predicate.
        // The choice point instructions are only created when a predicate is built from multiple clauses, as otherwise
        // there are no choices to be made.
        if (isFirst && !isLast && multipleClauses) {
            // try me else.
            preFixInstructions.add(new HiTalkWAMInstruction(entryLabel, TryMeElse, retryLabel));
        } else if (!isFirst && !isLast && multipleClauses) {
            // retry me else.
            preFixInstructions.add(new HiTalkWAMInstruction(entryLabel, RetryMeElse, retryLabel));
        } else if (isLast && multipleClauses) {
            // trust me.
            preFixInstructions.add(new HiTalkWAMInstruction(entryLabel, TrustMe));
        }

        // Generate the prefix code for the clause.
        // Rules may chain multiple, so require stack frames to preserve registers across calls.
        // Facts are always leafs so can use the global continuation point register to return from calls.
        // Chain rules only make one call, so also do not need a stack frame.
        if (!(isFact || isChainRule)) {
            // Allocate a stack frame at the start of the clause.
            /*log.fine("ALLOCATE " + numPermanentVars);*/
            preFixInstructions.add(new HiTalkWAMInstruction(Allocate));
        }

        // Deep cuts require the current choice point to be kept in a permanent variable, so that it can be recovered
        // once deeper choice points or BaseApps have been reached.
        if (cutLevelVarSlot >= 0) {
            /*log.fine("GET_LEVEL "+ cutLevelVarSlot);*/
            preFixInstructions.add(new HiTalkWAMInstruction(GetLevel, (byte) cutLevelVarSlot));
        }

        result.addInstructions(preFixInstructions);

        // Compile the clause head.
        IFunctor goal = clause.getHead();

        SizeableLinkedList<HiTalkWAMInstruction> instructions = compileHead(goal);
        result.addInstructions(goal, instructions);

        // Compile all of the conjunctive parts of the body of the clause, if there are any.
        if (!isFact) {
            ListTerm expressions = clause.getBody();

            for (int i = 0; i < expressions.size(); i++) {
                goal = (IFunctor) expressions.getHead(i);

                boolean isLastBody = i == (expressions.size() - 1);
                boolean isFirstBody = i == 0;

                Integer permVarsRemaining = (Integer) getSymbolTable().get(goal.getSymbolKey(), SYMKEY_PERM_VARS_REMAINING);

                // Select a non-default built-in implementation to compile the functor with, if it is a built-in.
                IPrologBuiltIn builtIn;

                if (goal instanceof IPrologBuiltIn) {
                    builtIn = (IPrologBuiltIn) goal;
                } else {
                    builtIn = defaultBuiltIn;
                }

                // The 'isFirstBody' parameter is only set to true, when this is the first functor of a rule.
                instructions = builtIn.compileBodyArguments(goal, i == 0, fn, i);
                result.addInstructions(goal, instructions);

                // Call the body. The number of permanent variables remaining is specified for BaseApp trimming.
                instructions = builtIn.compileBodyCall(goal, isFirstBody, isLastBody, isChainRule, permVarsRemaining);
                result.addInstructions(goal, instructions);
            }
        }

        // Generate the postfix code for the clause. Rules may chain, so require stack frames.
        // Facts are always leafs so can use the global continuation point register to return from calls.
        if (isFact) {
            /*log.fine("PROCEED");*/
            postFixInstructions.add(new HiTalkWAMInstruction(Proceed));
        }

        result.addInstructions(postFixInstructions);
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
     * count of the number remaining are used to implement BaseApp trimming.
     *
     * @param clause The clause to allocate registers for.
     */
    private void allocatePermanentProgramRegisters(HtClause clause) {
        // A bag to hold variable occurrence counts in.
        Map<HtVariable, Integer> variableCountBag = new HashMap<>();

        // A mapping from variables to the body number in which they appear last.
        Map<HtVariable, Integer> lastBodyMap = new HashMap<>();

        // Holds the variable that are in the head and first clause body argument.
        Collection<HtVariable> firstGroupVariables = new HashSet<>();

        // Get the occurrence counts of variables in all clauses after the initial head and first body grouping.
        // In the same pass, pick out which body variables last occur in.
        if ((clause.getBody() != null)) {
            for (int i = clause.getBody().size() - 1; i >= 1; i--) {
                Set<HtVariable> groupVariables = TermUtilities.findFreeVariables(clause.getBody().getHead(i));

                // Add all their counts to the bag and update their last occurrence positions.
                for (HtVariable variable : groupVariables) {
                    Integer count = variableCountBag.get(variable);
                    variableCountBag.put(variable, (count == null) ? 1 : (count + 1));

                    if (!lastBodyMap.containsKey(variable)) {
                        lastBodyMap.put(variable, i);
                    }

                    // If the cut level variable is seen, automatically add it to the first group variables,
                    // so that it will be counted as a permanent variable, and assigned a stack slot. This
                    // will only occur for deep cuts, that is where the cut comes after the first group.
                    if (isCutLevelVariable(variable)) {
                        firstGroupVariables.add(variable);
                    }
                }
            }
        }

        // Get the set of variables in the head and first clause body argument.
        if (clause.getHead() != null) {
            Set<HtVariable> headVariables = TermUtilities.findFreeVariables(clause.getHead());
            firstGroupVariables.addAll(headVariables);
        }

        if ((clause.getBody() != null) && (clause.getBody().size() > 0)) {
            Set<HtVariable> firstArgVariables = TermUtilities.findFreeVariables(clause.getBody().getHead(0));
            firstGroupVariables.addAll(firstArgVariables);
        }

        // Add their counts to the bag, and set their last positions of occurrence as required.
        for (HtVariable variable : firstGroupVariables) {
            Integer count = variableCountBag.get(variable);
            variableCountBag.put(variable, (count == null) ? 1 : (count + 1));

            if (!lastBodyMap.containsKey(variable)) {
                lastBodyMap.put(variable, 0);
            }
        }

        // Sort the variables by reverse position of last occurrence.
        List<Map.Entry<HtVariable, Integer>> lastBodyList = new ArrayList<>(lastBodyMap.entrySet());
        lastBodyList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        // Holds counts of permanent variable last appearances against the body in which they last occur.
        int[] permVarsRemainingCount = new int[(clause.getBody() != null) ? clause.getBody().size() : 0];

        // Search the count bag for all variable occurrences greater than one, and assign them to stack slots.
        // The variables are examined by reverse position of last occurrence, to ensure that later variables
        // are assigned to lower permanent allocation slots for BaseApp trimming purposes.
        for (Map.Entry<HtVariable, Integer> entry : lastBodyList) {
            HtVariable variable = entry.getKey();
            Integer count = variableCountBag.get(variable);
            int body = entry.getValue();

            if ((count != null) && (count > 1)) {
                /*log.fine("HtVariable " + variable + " is permanent, count = " + count);*/

                int allocation = (numPermanentVars++ & (0xff)) | (STACK_ADDR << 8);
                getSymbolTable().put(variable.getSymbolKey(), SYMKEY_ALLOCATION, allocation);

                // Check if the variable is the cut level variable, and cache its stack slot in 'cutLevelVarSlot', so that
                // the clause compiler knows which variable to use for the get_level instruction.
                if (isCutLevelVariable(variable)) {
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
            getSymbolTable().put(clause.getBody().getHead(i).getSymbolKey(),
                    SYMKEY_PERM_VARS_REMAINING,
                    permVarsRemaining);
            permVarsRemaining += permVarsRemainingCount[i];
        }
    }

    private boolean isCutLevelVariable(HtVariable variable) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void endScope() throws Exception {
        // Loop over all predicates in the current scope, found in the symbol table, and consume and compile them.
        for (SymbolKey predicateKey = predicatesInScope.poll(); predicateKey != null; predicateKey = predicatesInScope.poll()) {
            List<T> clauses = (List<T>) scopeTable.get(predicateKey, SYMKEY_PREDICATES);

            // Used to keep track of where within the predicate the current clause is.
            int size = clauses.size();
            int current = 0;
            boolean multipleClauses = size > 1;

            // Used to build up the compiled predicate in.
            P result = null;

            for (Iterator<T> iterator = clauses.iterator(); iterator.hasNext(); iterator.remove()) {
                T clause = iterator.next();

                if (result == null) {
                    result = createResult(clause.getHead().getName());
                }

                // Compile the single clause, adding it to the parent compiled predicate.
                compileClause(clause, result, current == 0, current >= (size - 1), multipleClauses, current);
                current++;
            }

            // Run the optimizer on the output.
            result = optimizer.apply(result);

            displayCompiledPredicate(result);
            observer.onCompilation(result);

            // Move up the low water mark on the predicates table.
            getSymbolTable().setLowMark(predicateKey, SYMKEY_PREDICATES);
        }

        // Clear up the symbol table, and bump the compilation scope up by one.
        getSymbolTable().clearUpToLowMark(SYMKEY_PREDICATES);
        scopeTable = null;
        scope++;
    }

    private P createResult(int name) {
        return (P) new HiTalkWAMCompiledPredicate(name);
    }

    /**
     * Pretty prints a compiled predicate.
     *
     * @param predicate The compiled predicate to pretty print.
     */
    private void displayCompiledPredicate(P predicate) {
        // Pretty print the clause.
        StringBuilder result = new StringBuilder();
        IPositionalTermVisitor displayVisitor =
                new HtWAMCompiledPredicatePrintingVisitor(getSymbolTable(), interner, result);

        HtTermWalkers.positionalWalker(displayVisitor).walk((ITerm) predicate);

        /*log.fine(result.toString());*/
    }


    /**
     * Examines all top-level functors within a clause, including any head and body, and determines which functor has
     * the highest number of arguments.
     *
     * @param clause The clause to determine the highest number of arguments within.
     * @return The highest number of arguments within any top-level functor in the clause.
     */
    private int findMaxArgumentsInClause(HtClause clause) {
        int result = 0;

        IFunctor head = clause.getHead();

        if (head != null) {
            result = head.getArity();
        }

        ListTerm body = clause.getBody();

        if (body != null) {
            final List<ITerm> heads = body.getHeads();
            for (final ITerm iTerm : heads) {
                final IFunctor functor = (IFunctor) iTerm;
                int arity = functor.getArity();
                result = Math.max(arity, result);
            }
        }
        return result;
    }

    /**
     * Compiles the head of a clause into an instruction listing in WAM.
     *
     * @param goal The clause head to compile.
     * @return A listing of the instructions for the clause head in the WAM instruction set.
     */
    private SizeableLinkedList<HiTalkWAMInstruction> compileHead(IFunctor goal) throws Exception {
        // Used to build up the results in.
        SizeableLinkedList<HiTalkWAMInstruction> instructions = new SizeableLinkedList<>();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.
        defaultBuiltIn.allocateArgumentRegisters(goal);
        defaultBuiltIn.allocateTemporaryRegisters(goal);

        // Program instructions are generated in the same order as the registers are assigned, the postfix
        // ordering used for queries is not needed.
        BreadthFirstSearch<ITerm, ITerm> outInSearch = new BreadthFirstSearch<>();
        outInSearch.reset();
        outInSearch.addStartState(goal);

        Iterator<ITerm> treeWalker = allSolutions(outInSearch);

        // Skip the outermost functor.
        treeWalker.next();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.

        // Keep track of processing of the arguments to the outermost functor as get_val and get_var instructions
        // need to be output for variables encountered in the arguments only.
        int numOutermostArgs = goal.getArity();

        for (int j = 0; treeWalker.hasNext(); j++) {
            ITerm nextTerm = treeWalker.next();

            /*log.fine("nextTerm = " + nextTerm);*/

            // For each functor encountered: get_struc.
            if (nextTerm.isFunctor()) {
                IFunctor nextFunctor = (IFunctor) nextTerm;
                int allocation = (Integer) getSymbolTable().get(nextFunctor.getSymbolKey(), SYMKEY_ALLOCATION);

                byte addrMode = (byte) ((allocation & 0xff00) >> 8);
                byte address = (byte) (allocation & 0xff);

                // Ouput a get_struc instruction, except on the outermost functor.
                /*log.fine("GET_STRUC " + interner.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                    ((addrMode == REG_ADDR) ? ", X" : ", Y") + address);*/

                HiTalkWAMInstruction instruction = new HiTalkWAMInstruction(GetStruc,
                        addrMode,
                        address,
                        interner.getFunctorFunctorName(nextFunctor),
                        nextFunctor);
                instructions.add(instruction);

                // For each argument of the functor.
                int numArgs = nextFunctor.getArity();

                for (int i = 0; i < numArgs; i++) {
                    ITerm nextArg = nextFunctor.getArgument(i);
                    allocation = (Integer) getSymbolTable().get(nextArg.getSymbolKey(), SYMKEY_ALLOCATION);
                    addrMode = (byte) ((allocation & 0xff00) >> 8);
                    address = (byte) (allocation & 0xff);

                    /*log.fine("nextArg = " + nextArg);*/

                    // If it is register not seen before: unify_var.
                    // If it is register seen before: unify_val.
                    if (!seenRegisters.contains(allocation)) {
                        /*log.fine("UNIFY_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/

                        seenRegisters.add(allocation);

                        instruction = new HiTalkWAMInstruction(UnifyVar, addrMode, address, nextArg);

                        // Record the way in which this variable was introduced into the clause.
                        getSymbolTable().put(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO, VarIntroduction.Unify);
                    } else {
                        // Check if the variable is 'local' and use a local instruction on the first occurrence.
                        VarIntroduction introduction = (VarIntroduction) getSymbolTable()
                                .get(
                                        nextArg.getSymbolKey(),
                                        SYMKEY_VARIABLE_INTRO);

                        if (defaultBuiltIn.isLocalVariable(introduction, addrMode)) {
                            /*log.fine("UNIFY_LOCAL_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") +
                                address);*/

                            instruction = new HiTalkWAMInstruction(UnifyLocalVal, addrMode, address, nextArg);

                            getSymbolTable().put(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO, null);
                        } else {
                            /*log.fine("UNIFY_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/
                            instruction = new HiTalkWAMInstruction(UnifyVal, addrMode, address, nextArg);
                        }
                    }

                    instructions.add(instruction);
                }
            } else if (j < numOutermostArgs) {
                ITerm nextVar = nextTerm;
                int allocation = (Integer) getSymbolTable().get(nextVar.getSymbolKey(), SYMKEY_ALLOCATION);
                byte addrMode = (byte) ((allocation & 0xff00) >> 8);
                byte address = (byte) (allocation & 0xff);

                HiTalkWAMInstruction instruction;

                // If it is register not seen before: get_var.
                // If it is register seen before: get_val.
                if (!seenRegisters.contains(allocation)) {
                    /*log.fine("GET_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                    seenRegisters.add(allocation);

                    instruction = new HiTalkWAMInstruction(GetVar, addrMode, address, (byte) (j & 0xff));

                    // Record the way in which this variable was introduced into the clause.
                    getSymbolTable().put(nextVar.getSymbolKey(), SYMKEY_VARIABLE_INTRO, VarIntroduction.Get);
                } else {
                    /*log.fine("GET_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                    instruction = new HiTalkWAMInstruction(GetVal, addrMode, address, (byte) (j & 0xff));
                }

                instructions.add(instruction);
            }
        }

        return instructions;
    }

    /**
     * Compiles the arguments to a call to a body of a clause into an instruction listing in WAM.
     * <p>
     * <p/>The name of the clause containing the body, and the position of the body within this clause are passed as
     * arguments, mainly so that these coordinates can be used to help make any labels generated within the generated
     * code unique.
     *
     * @param goal        The clause body to compile.
     * @param isFirstBody <tt>true</tt> iff this is the first body of a program clause.
     * @param clauseName  The name of the clause within which this body appears.
     * @param bodyNumber  The body position within the containing clause.
     * @return A listing of the instructions for the clause body in the WAM instruction set.
     */
    public SizeableLinkedList<HiTalkWAMInstruction> compileBodyArguments(
            IFunctor goal,
            boolean isFirstBody,
            HtFunctorName clauseName,
            int bodyNumber) throws Exception {
        return defaultBuiltIn.compileBodyArguments(goal, isFirstBody, clauseName, bodyNumber);
    }

    /**
     * QueryRegisterAllocatingVisitor visits named variables in a query, and if they are not already allocated to a
     * permanent stack slot, allocates them one. All named variables in queries are stack allocated, so that they are
     * preserved on the stack at the end of the query. Anonymous variables in queries are singletons, and not included
     * in the query results, so can be temporary.
     */
    public class QueryRegisterAllocatingVisitor extends HtDelegatingAllTermsVisitor {
        /**
         * The symbol table.
         */
        protected final ISymbolTable<Integer, String, Object> symbolTable;

        /**
         * Holds a map of permanent variables to variable names to record the allocations in.
         */
        private final Map<Byte, Integer> varNames;

        /**
         * Creates a query variable allocator.
         *
         * @param symbolTable The symbol table.
         * @param varNames    A map of permanent variables to variable names to record the allocations in.
         * @param delegate    The term visitor that this delegates to.
         */
        public QueryRegisterAllocatingVisitor(ISymbolTable<Integer, String, Object> symbolTable,
                                              Map<Byte, Integer> varNames,
                                              IAllTermsVisitor delegate) {
            super(delegate);
            this.symbolTable = symbolTable;
            this.varNames = varNames;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <p/>Allocates unallocated variables to stack slots.
         *
         * @return
         */
        public void visit(HtVariable variable) {
            if (getSymbolTable().get(variable.getSymbolKey(), SYMKEY_ALLOCATION) == null) {
                if (variable.isAnonymous()) {
                    //log.fine("Query variable " + variable + " is temporary.");

                    int allocation = (lastAllocatedTempReg++ & (0xff)) | (REG_ADDR << 8);
                    getSymbolTable().put(variable.getSymbolKey(), SYMKEY_ALLOCATION, allocation);
                    varNames.put((byte) allocation, variable.getName());
                } else {
                    /*log.fine("Query variable " + variable + " is permanent.");*/

                    int allocation = (numPermanentVars++ & (0xff)) | (HiTalkWAMInstruction.STACK_ADDR << 8);
                    getSymbolTable().put(variable.getSymbolKey(), SYMKEY_ALLOCATION, allocation);
                    varNames.put((byte) allocation, variable.getName());
                }
            }

            super.visit(variable);
        }
    }
}