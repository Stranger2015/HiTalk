
package org.ltc.hitalk.wam.compiler;

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

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverserImpl;
import com.thesett.aima.logic.fol.compiler.TermWalker;
import com.thesett.aima.logic.fol.wam.builtins.Cut;
import com.thesett.aima.logic.fol.wam.compiler.PositionAndOccurrenceVisitor;
import com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.aima.logic.fol.wam.compiler.WAMLabel;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.SizeableList;
import com.thesett.common.util.doublemaps.SymbolKey;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn.VarIntroduction;
import org.ltc.hitalk.wam.machine.HiTalkWAMMachine;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;
import org.ltc.hitalk.wam.printer.HtWAMCompiledPredicatePrintingVisitor;
import org.ltc.hitalk.wam.printer.HtWAMCompiledQueryPrintingVisitor;
import org.slf4j.Logger;

import java.util.*;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.*;
import static com.thesett.aima.search.util.Searches.allSolutions;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.HiTalkWAMInstructionSet.*;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.REG_ADDR;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.STACK_ADDR;

/**
 * WAMCompiled implements a compiler for the logical language, WAM, into a form suitable for passing to an
 * {@link HiTalkWAMMachine}. The HiTalkWAMMachine accepts sentences in the language that are compiled into a byte code form. The
 * byte instructions used in the compiled language are enumerated as constants in the {@link HiTalkWAMInstruction} class.
 * <p>
 * <p/>The compilation process is described in "Warren's Abstract Machine, A Tutorial Reconstruction, by Hassan
 * Ait-Kaci" and is followed as closely as possible to the WAM compiler given there. The description of the L0
 * compilation process is very clear in the text but the WAM compilation is a little ambiguous. It does not fully
 * describe the flattening process and presents some conflicting examples of register assignment. (The flattening
 * process is essentially the same as for L0, except that each argument of the outermost functor is flattened/compiled
 * independently). The register assignment process is harder to fathom, on page 22, the register assignment for p(Z,
 * h(Z,W), f(W)) is presented with the following assignment given:
 *
 * <pre>
 * A1 = Z
 * A2 = h(A1,X4)
 * A3 = f(X4)
 * X4 = W
 * </pre>
 * <p>
 * In figure 2.9 a compilation example is given, from which it can be seen that the assignment should be:
 *
 * <pre>
 * A1 = Z (loaded from X4)
 * A2 = h(X4,X5)
 * A3 = f(X5)
 * X4 = Z
 * X5 = W
 * </pre>
 * <p>
 * <p/>From figure 2.9 it was concluded that argument registers may only be assigned to functors. Functors can be
 * created on the heap and assigned to argument registers directly. Argument registers for variables, should be loaded
 * from a separate register assigned to the variable, that comes after the argument registers; so that a variable
 * assignment can be copied into multiple arguments, where the same variable is presented multiple times in a predicate
 * call. The register assignment process is carried out in two phases to do this, the first pass covers the argument
 * registers and the arguments of the outermost functor, only assigning to functors, the second pass continues for
 * higher numbered registers, starts again at the beginning of the arguments, and assigns to variables and functors (not
 * already assigned) as for the L0 process.
 * <p>
 * <p/>A brief overview of the compilation process is:
 *
 * <pre><p/><ul>
 * <li>Terms to be compiled are allocated registers, breadth first, enumerating from outermost functors down to
 *    innermost atoms or variables.</li>
 * <li>The outermost functor itself is treated specially, and is not allocated to a register. Its i arguments are
 *     allocated to registers, and are additionally associated with the first i argument registers. The outermost functor
 *     is the instigator of a call, in the case of queries, or the recipient of a call, in the case of programs.
 * <li>Queries are 'flattened' by traversing each of their arguments in postfix order of their functors, then exploring
 *     the functors arguments.</li>
 * <li>Programs are 'flattened' by traversing each of their arguments breadth first, the same as for the original
 *     register allocation, then exploring the functors arguments.</li>
 * </ul></pre>
 * <p>
 * <p/>Query terms are compiled into a sequence of instructions, that build up a representation of their argument terms,
 * to be unified, on the heap, and assigning registers to refer to those terms on the heap, then calling the matching
 * program for the query terms name and arity. Program terms are compiled into a sequence of instructions that, when run
 * against the argument registers, attempt to unify all of the arguments with the heap.
 * <p>
 * <p/>The effect of flattening queries using a post fix ordering, is that the values of inner functors and variables
 * are loaded into registers first, before their containing functor is executed, which writes the functor and its
 * arguments onto the heap. Programs do not need to be expanded in this way, they simply match functors followed by
 * their arguments against the heap, so a breadth first traversal is all that is needed.
 * <p>
 * <p/>Evaluating a flattened query consists of doing the following as different query tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For the outermost functor, process all arguments, then make a CALL (functor) to the matching program.
 * <li>For a register associated with an inner functor, push an STR onto the heap and copy that cell into the register.
 *     A put_struc (functor, register) instruction is created for this.</li>
 * <li>For a variable in argument position i in the outermost functor, push a REF onto the heap that refers to itself,
 *     and copy that value into that variables register, as well as argument register i. A put_var (register, register)
 *     instruction is emitted for this.
 * <li>For a register argument of an inner functor, not previously seen, push a REF onto the heap that refers to itself,
 *     and copy that cell into the register. A set_var (register) instruction is emitted for this.</li>
 * <li>For a variables in argument position i in the outermost functor, previously seen, copy its assigned register
 *     into its argument register. A put_val (register, register) instruction is emitted for this.</li>
 * <li>For a register argument previously seen, push a new cell onto the heap and copy into it the register's value.
 *     A set_val (register) instruction is emitted for this.</li>
 * </ol></pre>
 * <p>
 * <p/>Evaluating a flattened program consists of doing the following as different program tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For the outermost functor, process all arguments, then execute a PROCEED instruction to indicate success.
 * <li>For a register associated with an inner functor, load that register with a reference to the functor. A get_struc
 *     (functor, register) instruction is created for this.</li>
 * <li>For a variable in argument position i in the outermost functor, copy its argument register into its assigned
 *     register. A get_var (register, register) instruction is emitted for this.
 * <li>For a register argument of an inner functor, not previously seen, bind that register to its argument. A
 *     unify_var (register) instruction is output for this.</li>
 * <li>For a variable in argument position i in the outermost functor, unify its assigned register with the
 *     argument register. A get_val (register, register) instruction is emitted for this.</li>
 * <li>For a register argument of an inner functor, previously seen, unify that register against the heap. A
 *     unify_val (register) instruction is emitted for this.</li>
 * </ol></pre>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Transform WAM sentences into compiled byte code.
 *     <td> {@link HiTalkWAMMachine}, {@link HiTalkWAMCompiledPredicate}
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HiTalkInstructionCompiler//<T extends HtClause, P extends HtPredicate, Q extends HtClause>
        extends BaseInstructionCompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery>
        implements HiTalkBuiltIn {

//public final static FunctorName OBJECT = new FunctorName("object", 2);
//public final static FunctorName END_OBJECT = new FunctorName("end_object", 0);
//public final static FunctorName CATEGORY = new FunctorName("category",2);
//public final static FunctorName END_CATEGORY = new FunctorName("end_category",0);
//public final static FunctorName PROTOCOL = new FunctorName("protocol", 2);
//public final static FunctorName END_PROTOCOL =new FunctorName( "end_protocol",0);

    private final HiTalkDefaultBuiltIn defaultBuiltIn;

    // Used for debugging.
    /* private static final Logger log = Logger.getLogger(HiTalkInstructionCompiler.class.getName()); */

    /**
     * Holds the instruction optimizer.
     */
    protected HiTalkOptimizer optimizer;
    /**
     * Holds a list of all predicates encountered in the current scope.
     */
    protected Queue <SymbolKey> predicatesInScope = new LinkedList <>();
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
    private SymbolTable <Integer, String, Object> scopeTable;
    private Collection <Integer> seenRegisters;
    private int lastAllocatedTempReg;
    private Logger logger;

    /**
     * Creates a new HiTalkInstructionCompiler.
     *  @param symbolTable The symbol table.
     * @param interner    The machine to translate functor and variable names.
     * @param defaultBuiltIn
     */
    public
    HiTalkInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                VariableAndFunctorInterner interner,
                                HiTalkDefaultBuiltIn defaultBuiltIn ) {
        super(symbolTable, interner);
        optimizer = new HiTalkWAMOptimizer(symbolTable, interner);
        this.defaultBuiltIn = defaultBuiltIn;
    }

    /**
     * {@inheritDoc}
     *
     * @param observer
     */
    public
    void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {
        this.observer = observer;
    }

    /**
     * {@inheritDoc}
     */
    public
    void endScope () throws SourceCodeException {
        // Loop over all predicates in the current scope, found in the symbol table, and consume and compile them.
        for (SymbolKey predicateKey = predicatesInScope.poll(); predicateKey != null; predicateKey = predicatesInScope.poll()) {
            List <HtClause> clauseList = (List <HtClause>) scopeTable.get(predicateKey, SYMKEY_PREDICATES);

            // Used to keep track of where within the predicate the current clause is.
            int size = clauseList.size();
            int current = 0;
            boolean multipleClauses = size > 1;

            // Used to build up the compiled predicate in.
            HiTalkWAMCompiledPredicate result = null;

            for (Iterator <HtClause> iterator = clauseList.iterator(); iterator.hasNext(); iterator.remove()) {
                HtClause clause = iterator.next();

                if (result == null) {
                    result = new HiTalkWAMCompiledPredicate(clause.getHead().getName());
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
            symbolTable.setLowMark(predicateKey, SYMKEY_PREDICATES);
        }

        // Clear up the symbol table, and bump the compilation scope up by one.
        symbolTable.clearUpToLowMark(SYMKEY_PREDICATES);
        scopeTable = null;
        scope++;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Compiles a sentence into a binary form, that provides a Java interface into the compiled structure.
     * <p>
     * <p/>The clausal sentence may be a query, or a program statement. If it is a query, it is compiled immediately. If
     * it is a clause, it is retained against the predicate which it forms part of, and compiled on the
     * {@link #endScope()} method is invoked.
     */
    public
    void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
        /*log.fine("public WAMCompiledClause compile(Sentence<Term> sentence = " + sentence + "): called");*/

        // Extract the clause to compile from the parsed sentence.
        HtClause clause = sentence.getT();

        // Classify the sentence to compile by the different sentence types in the language.
        if (clause.isQuery()) {
            compileQuery(clause);
        }
        else {
            // Initialise a nested symbol table for the current compilation scope, if it has not already been.
            if (scopeTable == null) {
                scopeTable = symbolTable.enterScope(scope);
            }

            // Check in the symbol table, if a compiled predicate with name matching the program clause exists, and if
            // not create it.
            SymbolKey predicateKey = scopeTable.getSymbolKey(clause.getHead().getName());
            List <HtClause> clauseList = (List <HtClause>) scopeTable.get(predicateKey, SYMKEY_PREDICATES);

            if (clauseList == null) {
                clauseList = new ArrayList <>();
                scopeTable.put(predicateKey, SYMKEY_PREDICATES, clauseList);
                predicatesInScope.offer(predicateKey);
            }

            // Add the clause to compile to its parent predicate for compilation at the end of the current scope.
            clauseList.add(clause);
        }
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
    private
    void compileClause ( HtClause clause, HiTalkWAMCompiledPredicate compiledPredicate, boolean isFirst, boolean isLast, boolean multipleClauses, int clauseNumber ) {
        // Used to build up the compiled clause in.
        HiTalkWAMCompiledClause result = new HiTalkWAMCompiledClause((HiTalkWAMCompiledPredicate) compiledPredicate);

        // Check if the clause to compile is a fact (no body).
        boolean isFact = clause.getBody() == null;

        // Check if the clause to compile is a chain rule, (one called body).
        boolean isChainRule = (clause.getBody() != null) && (clause.getBody().length == 1);

        // Used to keep track of registers as they are seen during compilation. The first time a variable is seen,
        // a variable is written onto the heap, subsequent times its value. The first time a functor is seen,
        // its structure is written onto the heap, subsequent times it is compared with.
        seenRegisters = new TreeSet <>();

        // This is used to keep track of the next temporary register available to allocate.
        lastAllocatedTempReg = findMaxArgumentsInClause(clause);

        // This is used to keep track of the number of permanent variables.
        numPermanentVars = 0;

        // This is used to keep track of the allocation slot for the cut level variable, when needed. -1 means it is
        // not needed, so it is initialized to this.
        cutLevelVarSlot = -1;

        // These are used to generate pre and post instructions for the clause, for example, for the creation and
        // clean-up of stack frames.
        SizeableList <HiTalkWAMInstruction> preFixInstructions = new SizeableLinkedList <>();
        SizeableList <HiTalkWAMInstruction> postFixInstructions = new SizeableLinkedList <>();

        // Find all the free non-anonymous variables in the clause.
        Set <Variable> freeVars = TermUtils.findFreeNonAnonymousVariables(clause);
        Collection <Integer> freeVarNames = new TreeSet <>();

        for (Variable var : freeVars) {
            freeVarNames.add(var.getName());
        }

        // Allocate permanent variables for a program clause. Program clauses only use permanent variables when really
        // needed to preserve variables across calls.
        allocatePermanentProgramRegisters(clause);

        // Gather information about the counts and positions of occurrence of variables and constants within the clause.
        gatherPositionAndOccurrenceInfo(clause);

        // Labels the entry point to each choice point.
        FunctorName fn = interner.getFunctorFunctorName(clause.getHead());
        WAMLabel entryLabel = new WAMLabel(fn, clauseNumber);

        // Label for the entry point to the next choice point, to backtrack to.
        WAMLabel retryLabel = new WAMLabel(fn, clauseNumber + 1);

        // Create choice point instructions for the clause, depending on its position within the containing predicate.
        // The choice point instructions are only created when a predicate is built from multiple clauses, as otherwise
        // there are no choices to be made.
        if (isFirst && !isLast && multipleClauses) {
            // try me else.
            preFixInstructions.add(new HiTalkWAMInstruction(entryLabel, TryMeElse, retryLabel));
        }
        else if (!isFirst && !isLast && multipleClauses) {
            // retry me else.
            preFixInstructions.add(new HiTalkWAMInstruction(entryLabel, RetryMeElse, retryLabel));
        }
        else if (isLast && multipleClauses) {
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
        // once deeper choice points or environments have been reached.
        if (cutLevelVarSlot >= 0) {
            /*log.fine("GET_LEVEL "+ cutLevelVarSlot);*/
            preFixInstructions.add(new HiTalkWAMInstruction(GetLevel, (byte) cutLevelVarSlot));
        }

        result.addInstructions(preFixInstructions);

        // Compile the clause head.
        Functor expression = clause.getHead();

        SizeableLinkedList <HiTalkWAMInstruction> instructions = compileHead(expression);
        result.addInstructions(expression, instructions);

        // Compile all of the conjunctive parts of the body of the clause, if there are any.
        if (!isFact) {
            Functor[] expressions = clause.getBody();

            for (int i = 0; i < expressions.length; i++) {
                expression = expressions[i];

                boolean isLastBody = i == (expressions.length - 1);
                boolean isFirstBody = i == 0;

                Integer permVarsRemaining = (Integer) symbolTable.get(expression.getSymbolKey(), SYMKEY_PERM_VARS_REMAINING);

                // Select a non-default built-in implementation to compile the functor with, if it is a built-in.
                HiTalkBuiltIn builtIn;

                if (expression instanceof HiTalkBuiltIn) {
                    builtIn = (HiTalkBuiltIn) expression;
                }
                else {
                    builtIn = this;
                }

                // The 'isFirstBody' parameter is only set to true, when this is the first functor of a rule.
                instructions = builtIn.compileBodyArguments(expression, i == 0, fn, i);
                result.addInstructions(expression, instructions);

                // Call the body. The number of permanent variables remaining is specified for environment trimming.
                instructions = builtIn.compileBodyCall(expression, isFirstBody, isLastBody, isChainRule, permVarsRemaining);
                result.addInstructions(expression, instructions);
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
     * Compiles a clause as a query. The clause should have no head, only a body.
     *
     * @param clause The clause to compile as a query.
     * @throws SourceCodeException If there is an error in the source code preventing its compilation.
     */
    public
    void compileQuery ( HtClause clause ) throws SourceCodeException {
        checkDirective(clause);
        // Used to build up the compiled result in.
        HiTalkWAMCompiledQuery result;

        // A mapping from top stack frame slots to interned variable names is built up in this.
        // This is used to track the stack positions that variables in a query are assigned to.
        Map <Byte, Integer> varNames = new TreeMap <>();

        // Used to keep track of registers as they are seen during compilation. The first time a variable is seen,
        // a variable is written onto the heap, subsequent times its value. The first time a functor is seen,
        // its structure is written onto the heap, subsequent times it is compared with.
        seenRegisters = new TreeSet <>();

        // This is used to keep track of the next temporary register available to allocate.
        lastAllocatedTempReg = findMaxArgumentsInClause(clause);

        // This is used to keep track of the number of permanent variables.
        numPermanentVars = 0;

        // This is used to keep track of the allocation slot for the cut level variable, when needed. -1 means it is
        // not needed, so it is initialized to this.
        cutLevelVarSlot = -1;

        // These are used to generate pre and post instructions for the clause, for example, for the creation and
        // clean-up of stack frames.
        SizeableList <HiTalkWAMInstruction> preFixInstructions = new SizeableLinkedList <>();
        SizeableList <HiTalkWAMInstruction> postFixInstructions = new SizeableLinkedList <>();

        // Find all the free non-anonymous variables in the clause.
        Set <Variable> freeVars = TermUtils.findFreeNonAnonymousVariables(clause);
        Set <Integer> freeVarNames = new TreeSet <>();

        for (Variable var : freeVars) {
            freeVarNames.add(var.getName());
        }

        // Allocate permanent variables for a query. In queries all variables are permanent so that they are preserved
        // on the stack upon completion of the query.
        allocatePermanentQueryRegisters(clause, varNames);

        // Gather information about the counts and positions of occurrence of variables and constants within the clause.
        gatherPositionAndOccurrenceInfo(clause);

        result = new HiTalkWAMCompiledQuery(varNames, freeVarNames);

        // Generate the prefix code for the clause. Queries require a stack frames to hold their environment.
        /*log.fine("ALLOCATE " + numPermanentVars);*/
        preFixInstructions.add(new HiTalkWAMInstruction(AllocateN, REG_ADDR, (byte) (numPermanentVars & 0xff)));

        // Deep cuts require the current choice point to be kept in a permanent variable, so that it can be recovered
        // once deeper choice points or environments have been reached.
        if (cutLevelVarSlot >= 0) {
            /*log.fine("GET_LEVEL "+ cutLevelVarSlot);*/
            preFixInstructions.add(new HiTalkWAMInstruction(GetLevel, STACK_ADDR, (byte) cutLevelVarSlot));
        }

        result.addInstructions(preFixInstructions);

        // Compile all of the conjunctive parts of the body of the clause, if there are any.
        Functor[] expressions = clause.getBody();

        // The current query does not have a name, so invent one for it.
        FunctorName fn = new FunctorName("tq", 0);

        for (int i = 0; i < expressions.length; i++) {
            Functor expression = expressions[i];
            boolean isFirstBody = i == 0;

            // Select a non-default built-in implementation to compile the functor with, if it is a built-in.
            HiTalkBuiltIn builtIn;

            if (expression instanceof HiTalkBuiltIn) {
                builtIn = (HiTalkBuiltIn) expression;
            }
            else {
                builtIn = this;
            }

            // The 'isFirstBody' parameter is only set to true, when this is the first functor of a rule, which it
            // never is for a query.
            SizeableLinkedList <HiTalkWAMInstruction> instructions = builtIn.compileBodyArguments(expression, false, fn, i);
            result.addInstructions(expression, instructions);

            // Queries are never chain rules, and as all permanent variables are preserved, bodies are never called
            // as last calls.
            instructions = builtIn.compileBodyCall(expression, isFirstBody, false, false, numPermanentVars);
            result.addInstructions(expression, instructions);
        }

        // Generate the postfix code for the clause.
        /*log.fine("DEALLOCATE");*/
        postFixInstructions.add(new HiTalkWAMInstruction(Suspend));
        postFixInstructions.add(new HiTalkWAMInstruction(Deallocate));

        result.addInstructions(postFixInstructions);

        // Run the optimizer on the output.
        result = optimizer.apply(result);

        displayCompiledQuery(result);

        observer.onQueryCompilation(result);
    }

    /**
     * @param clause
     */
    public
    void compileClause ( HtClause clause ) {

    }

    /**
     * @param resolver
     */
//    @Override
    public
    void setResolver ( Resolver <HtClause, HiTalkWAMCompiledQuery> resolver ) {

    }

    private
    void checkDirective ( HtClause clause ) {
        Functor[] body = clause.getBody();
        if (body.length == 1) {
            int name = body[0].getName();

            switch (name) {

                default:
                    throw new IllegalStateException("Unexpected value: " + name);
            }
        }
    }

    /**
     * Examines all top-level functors within a clause, including any head and body, and determines which functor has
     * the highest number of arguments.
     *
     * @param clause The clause to determine the highest number of arguments within.
     * @return The highest number of arguments within any top-level functor in the clause.
     */
    private
    int findMaxArgumentsInClause ( HtClause clause ) {
        int result = 0;

        Functor head = clause.getHead();

        if (head != null) {
            result = head.getArity();
        }

        Functor[] body = clause.getBody();

        if (body != null) {
            for (Functor functor : body) {
                int arity = functor.getArity();
                result = Math.max(arity, result);
            }
        }

        return result;
    }

    /**
     * Compiles the head of a clause into an instruction listing in WAM.
     *
     * @param expression The clause head to compile.
     * @return A listing of the instructions for the clause head in the WAM instruction set.
     */
    private
    SizeableLinkedList <HiTalkWAMInstruction> compileHead ( Functor expression ) {
        // Used to build up the results in.
        SizeableLinkedList <HiTalkWAMInstruction> instructions = new SizeableLinkedList <>();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.
        defaultBuiltIn.allocateArgumentRegisters(expression);
        defaultBuiltIn.allocateTemporaryRegisters(expression);

        // Program instructions are generated in the same order as the registers are assigned, the postfix
        // ordering used for queries is not needed.
        BreadthFirstSearch <Term, Term> outInSearch = new BreadthFirstSearch <Term, Term>();
        outInSearch.reset();
        outInSearch.addStartState(expression);

        Iterator <Term> treeWalker = allSolutions(outInSearch);

        // Skip the outermost functor.
        treeWalker.next();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.

        // Keep track of processing of the arguments to the outermost functor as get_val and get_var instructions
        // need to be output for variables encountered in the arguments only.
        int numOutermostArgs = expression.getArity();

        for (int j = 0; treeWalker.hasNext(); j++) {
            Term nextTerm = treeWalker.next();

            /*log.fine("nextTerm = " + nextTerm);*/

            // For each functor encountered: get_struc.
            if (nextTerm.isFunctor()) {
                Functor nextFunctor = (Functor) nextTerm;
                int allocation = (Integer) symbolTable.get(nextFunctor.getSymbolKey(), SYMKEY_ALLOCATION);

                byte addrMode = (byte) ((allocation & 0xff00) >> 8);
                byte address = (byte) (allocation & 0xff);

                // Ouput a get_struc instruction, except on the outermost functor.
                /*log.fine("GET_STRUC " + interner.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                    ((addrMode == REG_ADDR) ? ", X" : ", Y") + address);*/

                HiTalkWAMInstruction instruction = new HiTalkWAMInstruction(GetStruc, addrMode, address, interner.getFunctorFunctorName(nextFunctor), nextFunctor);
                instructions.add(instruction);

                // For each argument of the functor.
                int numArgs = nextFunctor.getArity();

                for (int i = 0; i < numArgs; i++) {
                    Term nextArg = nextFunctor.getArgument(i);
                    allocation = (Integer) symbolTable.get(nextArg.getSymbolKey(), SYMKEY_ALLOCATION);
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
                        symbolTable.put(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO, VarIntroduction.Unify);
                    }
                    else {
                        // Check if the variable is 'local' and use a local instruction on the first occurrence.
                        VarIntroduction introduction = (VarIntroduction) symbolTable.get(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO);

                        if (isLocalVariable(introduction, addrMode)) {
                            /*log.fine("UNIFY_LOCAL_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") +
                                address);*/

                            instruction = new HiTalkWAMInstruction(UnifyLocalVal, addrMode, address, nextArg);

                            symbolTable.put(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO, null);
                        }
                        else {
                            /*log.fine("UNIFY_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/

                            instruction = new HiTalkWAMInstruction(UnifyVal, addrMode, address, nextArg);
                        }
                    }

                    instructions.add(instruction);
                }
            }
            else if (j < numOutermostArgs) {
                Term nextVar = nextTerm;
                int allocation = (Integer) symbolTable.get(nextVar.getSymbolKey(), SYMKEY_ALLOCATION);
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
                    symbolTable.put(nextVar.getSymbolKey(), SYMKEY_VARIABLE_INTRO, VarIntroduction.Get);
                }
                else {
                    /*log.fine("GET_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                    instruction = new HiTalkWAMInstruction(GetVal, addrMode, address, (byte) (j & 0xff));
                }

                instructions.add(instruction);
            }
        }

        return instructions;
    }

    private
    boolean isLocalVariable ( VarIntroduction introduction, byte addrMode ) {
        return false;
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
    private
    void allocatePermanentProgramRegisters ( HtClause clause ) {
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
    private
    void allocatePermanentQueryRegisters ( Term clause, Map <Byte, Integer> varNames ) {
        // Allocate local variable slots for all variables in a query.
        QueryRegisterAllocatingVisitor allocatingVisitor = new QueryRegisterAllocatingVisitor(symbolTable, varNames, null);

        HtPositionalTermTraverser positionalTraverser = new HtPositionalTermTraverserImpl();
        positionalTraverser.setContextChangeVisitor(allocatingVisitor);

        TermWalker walker = new TermWalker(new DepthFirstBacktrackingSearch <>(), positionalTraverser, allocatingVisitor);

        walker.walk(clause);
    }

    /**
     * Gather information about variable counts and positions of occurrence of constants and variable within a clause.
     *
     * @param clause The clause to check the variable occurrence and position of occurrence within.
     */
    private
    void gatherPositionAndOccurrenceInfo ( Term clause ) {
        PositionalTermTraverser positionalTraverser = new PositionalTermTraverserImpl();
        PositionAndOccurrenceVisitor positionAndOccurrenceVisitor = new PositionAndOccurrenceVisitor(interner, symbolTable, positionalTraverser);
        positionalTraverser.setContextChangeVisitor(positionAndOccurrenceVisitor);

        TermWalker walker = new TermWalker(new DepthFirstBacktrackingSearch <>(), positionalTraverser, positionAndOccurrenceVisitor);

        walker.walk(clause);
    }

    /**
     * Pretty prints a compiled predicate.
     *
     * @param predicate The compiled predicate to pretty print.
     */
    private
    void displayCompiledPredicate ( Term predicate ) {
        // Pretty print the clause.
        StringBuilder result = new StringBuilder();
        HtPositionalTermVisitor displayVisitor = new HtWAMCompiledPredicatePrintingVisitor(symbolTable, interner, result);

        HtTermWalkers.positionalWalker(displayVisitor).walk(predicate);

        /*log.fine(result.toString());*/
    }

    /**
     * Pretty prints a compiled query.
     *
     * @param query The compiled query to pretty print.
     */
    private
    void displayCompiledQuery ( Term query ) {
        // Pretty print the clause.
        StringBuilder result = new StringBuilder();

        HtPositionalTermVisitor displayVisitor = new HtWAMCompiledQueryPrintingVisitor(symbolTable, interner, result);

        HtTermWalkers.positionalWalker(displayVisitor).walk(query);

        /*log.fine(result.toString());*/
    }

    /**
     * Compiles the arguments to a call to a body of a clause into an instruction listing in WAM.
     * <p>
     * <p/>The name of the clause containing the body, and the position of the body within this clause are passed as
     * arguments, mainly so that these coordinates can be used to help make any labels generated within the generated
     * code unique.
     *
     * @param expression  The clause body to compile.
     * @param isFirstBody <tt>true</tt> iff this is the first body of a program clause.
     * @param clauseName  The name of the clause within which this body appears.
     * @param bodyNumber  The body position within the containing clause.
     * @return A listing of the instructions for the clause body in the WAM instruction set.
     */
    @Override
    public
    SizeableLinkedList <HiTalkWAMInstruction> compileBodyArguments ( Functor expression, boolean isFirstBody, FunctorName clauseName, int bodyNumber ) {
        return defaultBuiltIn.compileBodyArguments(expression, isFirstBody, clauseName, bodyNumber);
    }

    /**
     * Compiles a call to a body of a clause into an instruction listing in WAM.
     *
     * @param expression        The body functor to call.
     * @param isFirstBody       Iff this is the first body in a clause.
     * @param isLastBody        Iff this is the last body in a clause.
     * @param chainRule         Iff the clause is a chain rule, so has no environment frame.
     * @param permVarsRemaining The number of permanent variables remaining at this point in the calling clause. Used
     *                          for environment trimming.
     * @return A list of instructions for the body call.
     */
    @Override
    public
    SizeableLinkedList <HiTalkWAMInstruction> compileBodyCall ( Functor expression, boolean isFirstBody, boolean isLastBody, boolean chainRule, int permVarsRemaining ) {
        return defaultBuiltIn.compileBodyCall(expression, isFirstBody, isLastBody, chainRule, permVarsRemaining);
    }

    /**
     * @return
     */
    @Override
    public
    Logger getConsole () {
        return logger;
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser getParser () {
        return parser;
    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    /**
     * @return
    /**
     * @param rule
     */
//    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    /**
     * @param query
     */
    @Override
    public
    void compileQuery ( HiTalkWAMCompiledQuery query ) throws SourceCodeException {

    }

    /**
     * QueryRegisterAllocatingVisitor visits named variables in a query, and if they are not already allocated to a
     * permanent stack slot, allocates them one. All named variables in queries are stack allocated, so that they are
     * preserved on the stack at the end of the query. Anonymous variables in queries are singletons, and not included
     * in the query results, so can be temporary.
     */
    public
    class QueryRegisterAllocatingVisitor extends DelegatingAllTermsVisitor {
        /**
         * The symbol table.
         */
        private final SymbolTable <Integer, String, Object> symbolTable;

        /**
         * Holds a map of permanent variables to variable names to record the allocations in.
         */
        private final Map <Byte, Integer> varNames;

        /**
         * Creates a query variable allocator.
         *
         * @param symbolTable The symbol table.
         * @param varNames    A map of permanent variables to variable names to record the allocations in.
         * @param delegate    The term visitor that this delegates to.
         */
        public
        QueryRegisterAllocatingVisitor ( SymbolTable <Integer, String, Object> symbolTable, Map <Byte, Integer> varNames, AllTermsVisitor delegate ) {
            super(delegate);
            this.symbolTable = symbolTable;
            this.varNames = varNames;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <p/>Allocates unallocated variables to stack slots.
         */
        public
        void visit ( Variable variable ) {
            if (symbolTable.get(variable.getSymbolKey(), SYMKEY_ALLOCATION) == null) {
                if (variable.isAnonymous()) {
                    /*log.fine("Query variable " + variable + " is temporary.");*/

                    int allocation = (lastAllocatedTempReg++ & (0xff)) | (REG_ADDR << 8);
                    symbolTable.put(variable.getSymbolKey(), SYMKEY_ALLOCATION, allocation);
                    varNames.put((byte) allocation, variable.getName());
                }
                else {
                    /*log.fine("Query variable " + variable + " is permanent.");*/

                    int allocation = (numPermanentVars++ & (0xff)) | (WAMInstruction.STACK_ADDR << 8);
                    symbolTable.put(variable.getSymbolKey(), SYMKEY_ALLOCATION, allocation);
                    varNames.put((byte) allocation, variable.getName());
                }
            }

            super.visit(variable);
        }
    }
}