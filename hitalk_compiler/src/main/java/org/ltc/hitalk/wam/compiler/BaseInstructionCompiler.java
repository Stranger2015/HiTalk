package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.wam.compiler.WAMLabel;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.SizeableList;
import com.thesett.common.util.doublemaps.SymbolKey;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.task.CompilerTask;

import java.io.IOException;
import java.util.*;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.*;
import static com.thesett.aima.search.util.Searches.allSolutions;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.HiTalkWAMInstructionSet.*;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.REG_ADDR;
import static org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction.STACK_ADDR;

/**
 * @param <P>
 * @param <Q>
 */
public abstract class BaseInstructionCompiler<P, Q> extends BaseCompiler <P, Q> {

    protected final PrologDefaultBuiltIn defaultBuiltIn;
    /**
     * Holds the instruction optimizer.
     */
    protected HiTalkOptimizer optimizer;
    /**
     * Holds a list of all predicates encountered in the current scope.
     */
    protected Deque <SymbolKey> predicatesInScope = new ArrayDeque <>();
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

    /**
     * @return
     */
    public Deque <CompilerTask <HtClause, Term>> getTasks () {
        return tasks;
    }

    /**
     *
     */
    public final Deque <CompilerTask <HtClause, Term>> tasks = new ArrayDeque <>();

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public BaseInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                     VariableAndFunctorInterner interner,
                                     PrologDefaultBuiltIn defaultBuiltIn,
                                     PlPrologParser parser ) {
        super(symbolTable, interner, parser);
        optimizer = new HiTalkWAMOptimizer(symbolTable, interner);
        this.defaultBuiltIn = defaultBuiltIn;

    }

    @Override
    public void compileClause ( HtClause clause ) {
        super.compileClause(clause);
    }

    @Override
    public void compileFile ( String fn, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    /**
     * Compiles a clause as a query. The clause should have no head, only a body.
     *
     * @param clause lause The clause to compile as a query.
     * @throws SourceCodeException If there is an error in the source code preventing its compilation.
     */
    public void compileQuery ( HtClause clause ) throws SourceCodeException {
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
            PrologBuiltIn builtIn;

            if (expression instanceof PrologBuiltIn) {
                builtIn = (PrologBuiltIn) expression;
            } else {
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


    private void checkDirective ( HtClause clause ) {
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
     * Compiles a program clause, and adds its instructions to a compiled predicate.
     *
     * @param clause            The source clause to compile.
     * @param compiledPredicate The predicate to add instructions to.
     * @param isFirst           <tt>true</tt> iff the clause is the first in the predicate.
     * @param isLast            <tt>true</tt> iff the clause is the last in the predicate.
     * @param multipleClauses   <tt>true</tt> iff the predicate contains >1 clause.
     * @param clauseNumber      The position of the clause within the predicate.
     */
    protected void compileClause ( HtClause clause,
                                   HiTalkWAMCompiledPredicate compiledPredicate,
                                   boolean isFirst,
                                   boolean isLast,
                                   boolean multipleClauses,
                                   int clauseNumber ) {
        // Used to build up the compiled clause in.
        HiTalkWAMCompiledClause result = new HiTalkWAMCompiledClause(compiledPredicate);

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
                PrologBuiltIn builtIn;

                if (expression instanceof PrologBuiltIn) {
                    builtIn = (PrologBuiltIn) expression;
                } else {
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
     * {@inheritDoc}
     */
    public void endScope () throws SourceCodeException {
        // Loop over all predicates in the current scope, found in the symbol table, and consume and compile them.
        for (SymbolKey predicateKey = predicatesInScope.poll(); predicateKey != null; predicateKey = predicatesInScope.poll()) {
            List <HtClause> clauses = (List <HtClause>) scopeTable.get(predicateKey, SYMKEY_PREDICATES);

            // Used to keep track of where within the predicate the current clause is.
            int size = clauses.size();
            int current = 0;
            boolean multipleClauses = size > 1;

            // Used to build up the compiled predicate in.
            HiTalkWAMCompiledPredicate result = null;

            for (Iterator <HtClause> iterator = clauses.iterator(); iterator.hasNext(); iterator.remove()) {
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
     * Examines all top-level functors within a clause, including any head and body, and determines which functor has
     * the highest number of arguments.
     *
     * @param clause The clause to determine the highest number of arguments within.
     * @return The highest number of arguments within any top-level functor in the clause.
     */
    private int findMaxArgumentsInClause ( HtClause clause ) {
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
    private SizeableLinkedList <HiTalkWAMInstruction> compileHead ( Functor expression ) {
        // Used to build up the results in.
        SizeableLinkedList <HiTalkWAMInstruction> instructions = new SizeableLinkedList <>();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.
        defaultBuiltIn.allocateArgumentRegisters(expression);
        defaultBuiltIn.allocateTemporaryRegisters(expression);

        // Program instructions are generated in the same order as the registers are assigned, the postfix
        // ordering used for queries is not needed.
        BreadthFirstSearch <Term, Term> outInSearch = new BreadthFirstSearch <>();
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
                        symbolTable.put(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO, HiTalkDefaultBuiltIn.VarIntroduction.Unify);
                    } else {
                        // Check if the variable is 'local' and use a local instruction on the first occurrence.
                        HiTalkDefaultBuiltIn.VarIntroduction introduction = (HiTalkDefaultBuiltIn.VarIntroduction) symbolTable.get(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO);

                        if (isLocalVariable(introduction, addrMode)) {
                            /*log.fine("UNIFY_LOCAL_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") +
                                address);*/

                            instruction = new HiTalkWAMInstruction(UnifyLocalVal, addrMode, address, nextArg);

                            symbolTable.put(nextArg.getSymbolKey(), SYMKEY_VARIABLE_INTRO, null);
                        } else {
                            /*log.fine("UNIFY_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/

                            instruction = new HiTalkWAMInstruction(UnifyVal, addrMode, address, nextArg);
                        }
                    }

                    instructions.add(instruction);
                }
            } else if (j < numOutermostArgs) {
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
                    symbolTable.put(nextVar.getSymbolKey(), SYMKEY_VARIABLE_INTRO, HiTalkDefaultBuiltIn.VarIntroduction.Get);
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
     * @param expression  The clause body to compile.
     * @param isFirstBody <tt>true</tt> iff this is the first body of a program clause.
     * @param clauseName  The name of the clause within which this body appears.
     * @param bodyNumber  The body position within the containing clause.
     * @return A listing of the instructions for the clause body in the WAM instruction set.
     */
    public SizeableLinkedList <HiTalkWAMInstruction> compileBodyArguments (
            Functor expression,
            boolean isFirstBody,
            FunctorName clauseName,
            int bodyNumber ) {
        return defaultBuiltIn.compileBodyArguments(expression, isFirstBody, clauseName, bodyNumber);
    }
}