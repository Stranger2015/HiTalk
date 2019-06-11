package org.ltc.hitalk.wam.machine;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMReservedLabel;
import com.thesett.aima.logic.fol.wam.machine.WAMCodeView;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract
class HiTalkWAMBaseMachine extends VariableAndFunctorInternerImpl implements HiTalkWAMMachine, WAMCodeView {
    /* Used for debugging. */
    private static final Logger log = Logger.getLogger(HiTalkWAMBaseMachine.class.getName());

    /**
     * The symbol table key for call points.
     */
    protected static final String SYMKEY_CALLPOINTS = "call_points";

    /**
     * Holds the symbol table.
     */
    protected SymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the reverse symbol table to look up names by addresses.
     */
    protected Map <Integer, Integer> reverseTable = new HashMap <>();

    /**
     * Creates the base machine, providing variable and functor symbol tables.
     *
     * @param symbolTable The symbol table.
     */
    protected
    HiTalkWAMBaseMachine ( SymbolTable <Integer, String, Object> symbolTable ) {
        super("WAM_Variable_Namespace", "WAM_Functor_Namespace");
        this.symbolTable = symbolTable;
    }

    /**
     * {@inheritDoc}
     */
    public abstract
    void emitCode ( WAMCompiledPredicate predicate ) throws LinkageException;

    /**
     * {@inheritDoc}
     */
    public abstract
    void emitCode ( WAMCompiledQuery query ) throws LinkageException;

    /**
     * {@inheritDoc}
     */
    public abstract
    void emitCode ( int offset, int address );

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param callPoint The call table entry giving the location and length of the code.
     * @return The byte code at the specified location.
     */
    public abstract
    byte[] retrieveCode ( WAMCallPoint callPoint );

    /**
     * Looks up the offset of the start of the code for the named functor.
     *
     * @param functorName The interned name of the functor to find the start address of the code for.
     * @return The call table entry of the functors code within the code area of the machine, or an invalid address if
     * the functor is not known to the machine.
     */
    public
    WAMCallPoint resolveCallPoint ( int functorName ) {
        /*log.fine("public WAMCallPoint resolveCallPoint(int functorName): called");*/

        WAMCallPoint result = (WAMCallPoint) symbolTable.get(functorName, SYMKEY_CALLPOINTS);

        if (result == null) {
            result = new WAMCallPoint(-1, 0, functorName);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public
    void reserveReferenceToLabel ( int labelName, int offset ) {
        // Create call point with label name if it does not already exist.
        WAMReservedLabel label = (WAMReservedLabel) symbolTable.get(labelName, SYMKEY_CALLPOINTS);

        if (label == null) {
            label = new WAMReservedLabel(labelName);
            symbolTable.put(labelName, SYMKEY_CALLPOINTS, label);
        }

        // Add to the mapping from the label to referenced from addresses to fill in later.
        label.referenceList.add(offset);
    }

    /**
     * {@inheritDoc}
     */
    public
    void resolveLabelPoint ( int labelName, int address ) {
        // Create the label with resolved address, if it does not already exist.
        WAMReservedLabel label = (WAMReservedLabel) symbolTable.get(labelName, SYMKEY_CALLPOINTS);

        if (label == null) {
            label = new WAMReservedLabel(labelName);
            symbolTable.put(labelName, SYMKEY_CALLPOINTS, label);
        }

        label.entryPoint = address;

        // Fill in all references to the label with the correct value. This does nothing if the label was just created.
        for (Integer offset : label.referenceList) {
            emitCode(offset, label.entryPoint);
        }

        // Keep a reverse lookup from address to label name.
        reverseTable.put(address, labelName);
    }

    /**
     * Provides read access to the machines bytecode buffer.
     *
     * @param start  The start offset within the buffer to read.
     * @param length Then length within the buffer to read.
     * @return The requested portion of the machines bytecode buffer.
     */
    @Override
    public
    ByteBuffer getCodeBuffer ( int start, int length ) {
//        return null;
        throw new Error();
    }


    /**
     * {@inheritDoc}
     */
    public
    Integer getNameForAddress ( int address ) {
        return reverseTable.get(address);
    }

    /**
     * Adds compiled byte code to the code area of the machine.
     *
     * @param predicate The compiled predicate to add byte code to the machine for.
     * @throws LinkageException If the predicate to be added to the machine, cannot be added to it, because it depends
     *                          on the existence of other callable predicate heads which are not in the machine.
     *                          Implementations may elect to raise this as an error at the time the functor is added to
     *                          the machine, or during execution, or simply to fail to find a solution.
     */
    @Override
    public
    void emitCode ( HiTalkWAMCompiledPredicate predicate ) throws LinkageException {
//JMXConnectorServerFactory.logger.info();
        throw new Error("emoitcode");
    }

    /**
     * Adds compiled byte code for a query to the code area of the machine.
     *
     * @param query The compiled query to add byte code to the machine for.
     * @throws LinkageException If the query to be added to the machine, cannot be added to it, because it depends on
     *                          the existence of other callable query heads which are not in the machine.
     *                          Implementations may elect to raise this as an error at the time the functor is added to
     *                          the machine, or during execution, or simply to fail to find a solution.
     */
    @Override
    public
    void emitCode ( HiTalkWAMCompiledQuery query ) throws LinkageException {

    }

//    /**
//     * Adds an address into code at a specified offset. This will happen when working with forward references. When code
//     * is initially added, a forward reference may not yet reside at a known address, in which case the address can be
//     * filled in with a dummy value, and the offset recorded. This can be used to fill in the correct value later.
//     *
//     * @param offset  The offset into the code area to write the address.
//     * @param address The address to fill in.
//     */
//    @Override
//    public
//    void emitCode ( int offset, int address ) {
//
//    }

    /**
     * Resets the machine, to its initial state. This should clear any programs from the machine, and clear all of its
     * stacks and heaps.
     */
    public
    void reset () {
        // Clear the entire symbol table.
        symbolTable.clear();
        reverseTable.clear();
    }

    /**
     * Records the offset of the start of the code for the named functor.
     *
     * @param functorName The interned name of the functor to find the start address of the code for.
     * @param offset      The offset of the start of the functors code within the code area.
     * @param length      The size of the code to set the address for.
     * @return The call table entry for the functors code within the code area of the machine.
     */
    protected
    WAMCallPoint setCodeAddress ( int functorName, int offset, int length ) {
        WAMCallPoint entry = new WAMCallPoint(offset, length, functorName);
        symbolTable.put(functorName, SYMKEY_CALLPOINTS, entry);

        // Keep a reverse lookup from address to functor name.
        reverseTable.put(offset, functorName);

        return entry;
    }

    /**
     * Records the id of an internal function for the named functor. The method name uses the word 'address' but this is
     * not really accurate, the address field is used to hold an id of the internal function to be invoked. This method
     * differs from {@link #setCodeAddress(int, int, int)}, as it does not set the reverse mapping from the address to
     * the functor name, since an address is not really being used.
     *
     * @param functorName The interned name of the functor to find the start address of the code for.
     * @param id          The offset of the start of the functors code within the code area.
     * @return The call table entry for the functors code within the code area of the machine.
     */
    protected
    WAMCallPoint setInternalCodeAddress ( int functorName, int id ) {
        WAMCallPoint entry = new WAMCallPoint(id, 0, functorName);
        symbolTable.put(functorName, SYMKEY_CALLPOINTS, entry);

        return entry;
    }
}
