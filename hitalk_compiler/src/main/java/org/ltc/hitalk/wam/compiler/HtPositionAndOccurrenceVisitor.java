package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys;
import com.thesett.common.util.doublemaps.SymbolKey;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.IPositionalContext;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

import java.util.*;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_FUNCTOR_NON_ARG;
import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_VAR_LAST_ARG_FUNCTOR;

public class HtPositionAndOccurrenceVisitor extends HtBasePositionalVisitor {
    public IPositionalTermTraverser getPositionalTraverser() {
        return positionalTraverser;
    }

    protected IPositionalTermTraverser positionalTraverser;

//    public HtPositionAndOccurrenceVisitor ( IVafInterner interner,
//                                            SymbolTable <Integer, String, Object> symbolTable,
//                                            IPositionalTermTraverser positionalTraverser ) {
//        super(symbolTable, interner);
//        this.positionalTraverser = positionalTraverser;
//    }

    // Used for debugging.
    /* private static final Logger log = Logger.getLogger(PositionAndOccurrenceVisitor.class.getName()); */

    /**
     * Holds the current top-level body functor. <tt>null</tt> when traversing the head.
     */
    private IFunctor topLevelBodyFunctor;

    /**
     * Holds a set of all constants encountered.
     */
    private final Map<Integer, List<SymbolKey>> constants = new HashMap<>();

    /**
     * Holds a set of all constants found to be in argument positions.
     */
    private final Collection<Integer> argumentConstants = new HashSet<>();

    /**
     * Creates a positional visitor.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     */
    public HtPositionAndOccurrenceVisitor(
            ISymbolTable<Integer, String, Object> symbolTable,
            IVafInterner interner,
            IPositionalTermTraverser traverser) {
        super(symbolTable, interner, traverser);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Counts variable occurrences and detects if the variable ever appears in an argument position.
     */
    protected void enterVariable(HtVariable variable) {
        // Initialize the count to one or add one to an existing count.
        Integer count = (Integer) symbolTable.get(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_OCCURRENCE_COUNT);
        count = (count == null) ? 1 : (count + 1);
        symbolTable.put(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_OCCURRENCE_COUNT, count);

        /*log.fine("Variable " + variable + " has count " + count + ".");*/

        // Get the nonArgPosition flag, or initialize it to true.
        Boolean nonArgPositionOnly =
                (Boolean) symbolTable.get(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_NON_ARG);
        nonArgPositionOnly = (nonArgPositionOnly == null) ? true : nonArgPositionOnly;

        // Clear the nonArgPosition flag if the variable occurs in an argument position.
        nonArgPositionOnly = inTopLevelFunctor(traverser) ? false : nonArgPositionOnly;
        symbolTable.put(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_NON_ARG, nonArgPositionOnly);

        /*log.fine("Variable " + variable + " nonArgPosition is " + nonArgPositionOnly + ".");*/

        // If in an argument position, record the parent body functor against the variable, as potentially being
        // the last one it occurs in, in a purely argument position.
        // If not in an argument position, clear any parent functor recorded against the variable, as this current
        // last position of occurrence is not purely in argument position.
        if (inTopLevelFunctor(traverser)) {
            symbolTable.put(variable.getSymbolKey(), SYMKEY_VAR_LAST_ARG_FUNCTOR, topLevelBodyFunctor);
        } else {
            symbolTable.put(variable.getSymbolKey(), SYMKEY_VAR_LAST_ARG_FUNCTOR, null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Checks if a constant ever appears in an argument position.
     * <p>
     * <p/>Sets the 'inTopLevelFunctor' flag, whenever the traversal is directly within a top-level functors arguments.
     * This is set at the end, so that subsequent calls to this will pick up the state of this flag at the point
     * immediately below a top-level functor.
     */
    protected void enterFunctor(IFunctor functor) throws Exception {
        /*log.fine("Functor: " + functor.getName() + " <- " + symbolTable.getSymbolKey(functor.getName()));*/

        // Only check position of occurrence for constants.
        if (functor.getArity() == 0) {
            // Add the constant to the set of all constants encountered.
            List<SymbolKey> constantSymKeys = constants.get(functor.getName());

            if (constantSymKeys == null) {
                constantSymKeys = new ArrayList<>();
                constants.put(functor.getName(), constantSymKeys);
            }

            constantSymKeys.add(functor.getSymbolKey());

            // If the constant ever appears in argument position, take note of this.
            if (inTopLevelFunctor(traverser)) {
                argumentConstants.add(functor.getName());
            }
        }

        // Keep track of the current top-level body functor.
        if (isTopLevel(traverser) && !traverser.isInHead()) {
            topLevelBodyFunctor = functor;
        }
    }

    /**
     * Upon leaving the clause, sets the nonArgPosition flag on any constants that need it.
     *
     * @param clause The clause being left.
     */
    protected void leaveClause(HtClause clause) {
        // Remove the set of constants appearing in argument positions, from the set of all constants, to derive
        // the set of constants that appear in non-argument positions only.
        constants.keySet().removeAll(argumentConstants);

        // Set the nonArgPosition flag on all symbol keys for all constants that only appear in non-arg positions.
        for (List<SymbolKey> symbolKeys : constants.values()) {
            for (SymbolKey symbolKey : symbolKeys) {
                symbolTable.put(symbolKey, SYMKEY_FUNCTOR_NON_ARG, true);
            }
        }
    }

    /**
     * Checks if the current position is immediately within a top-level functor.
     *
     * @param context The position context to examine.
     * @return <tt>true</tt> iff the current position is immediately within a top-level functor.
     */
    private boolean inTopLevelFunctor(IPositionalContext context) {
        IPositionalContext parentContext = context.getParentContext();

        return parentContext.isTopLevel() || isTopLevel(parentContext);
    }

    /**
     * Functors are considered top-level when they appear at the top-level within a clause, or directly beneath a parent
     * conjunction or disjunction that is considered to be top-level.
     *
     * @param context The position context to examine.
     * @return <tt>true</tt> iff the current position is a top-level functor.
     */
    private boolean isTopLevel(IPositionalContext context) {
        ITerm term = context.getTerm();

        if (term.getSymbolKey() == null) {
            return false;
        }

        Boolean isTopLevel = (Boolean) symbolTable.get(term.getSymbolKey(), SymbolTableKeys.SYMKEY_TOP_LEVEL_FUNCTOR);

        return (isTopLevel == null) ? false : isTopLevel;
    }
}
