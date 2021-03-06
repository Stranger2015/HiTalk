package org.ltc.hitalk.wam.compiler.hitalk;

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

import com.thesett.aima.attribute.impl.IdAttribute.IdAttributeFactory;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.common.util.Sizeable;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.SizeableList;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.wam.compiler.IWAMOptimizeableListing;
import org.ltc.hitalk.wam.machine.HiTalkWAMMachine;
import org.ltc.hitalk.wam.machine.HiTalkWAMResolvingMachine;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * A compiled term is a handle onto a compiled down to binary code term. Compiled terms are not Java code and may be
 * executed outside of the JVM, but the Java retains a handle on them and provides sufficient wrapping around them that
 * they can be made to look as if they are a transparent abstract syntax tree within the JVM. This requires an ability
 * to decompile a clause back into its abstract syntax tree.
 * <p>
 * <p/>Decompilation of functors requires access to a mapping from registers to variables, but the variable to register
 * assignment is created at compile time accross a whole clause. Each functor that forms part of the clause head or definition
 * must therefore hold a link to its containing functor in order to access this mapping.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Decompile/decode a binary term to restore its abstract syntax tree.
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo For each functor in the head and definition, set this as the containing clause. A mapping from variables to
 * registers is maintained in the clause, and the functors need to be able to access this mapping.
 */
public
class HiTalkWAMCompiledPredicate extends HtPredicate
        implements Sentence<HiTalkWAMCompiledPredicate>, Sizeable, IWAMOptimizeableListing {
    /**
     * @return
     */
    @Override
    public boolean isHiLog() {
        return false;
    }
    //Used for debugging.
    //private static final Logger log = Logger.getLogger(WAMCompiledClause.class.getName());

    /**
     * Defines the possible states of compiled code, unlinked, or linked into a machine.
     */
    public
    enum LinkStatus {
        /**
         * The code is not yet linked into a binary machine.
         */
        Unlinked,

        /**
         * The code is linked into a binary machine.
         */
        Linked
    }

    /**
     * Holds the interned name of this predicate.
     */
    private final int name;

    /**
     * Holds the state of the byte code in this compiled functor, whether it has been linked    or not.
     */
    protected LinkStatus status;

    /**
     * Holds the register to variable id mapping for the functor.
     */
    protected Map <Byte, Integer> varNames;

    /**
     * Holds a listing of all the free non-anonymous variables in the query, used to decide which variables to report in
     * the query results.
     */
    protected Set <Integer> nonAnonymousFreeVariables;

    /**
     * Holds the byte code instructions for the clause, when it is not linked or when it has been disassembled.
     */
    protected SizeableList <HiTalkWAMInstruction> instructions = new SizeableLinkedList <>();

    /**
     * Holds the original unoptimized instruction listing.
     */
    protected SizeableList <HiTalkWAMInstruction> unoptimizedInstructions;

    /**
     * Holds the offset of the compiled code for the clause within the machine it is compiled to.
     */
    protected WAMCallPoint callPoint;

    /**
     * Holds a reference to the byte code machine, that provides the symbol table for the code.
     */
    protected HiTalkWAMMachine machine;

    /**
     * Holds a reference to the functor interner, that maps functors onto names.
     */
    protected IdAttributeFactory <FunctorName> functorInterner;

    /**
     * @param name
     */
    public HiTalkWAMCompiledPredicate ( int name ) {
        super(null);
        this.name = name;
    }


    /**
     * Provides the interned name of this predicate.
     *
     * @return The interned name of this predicate.
     */
    public
    int getName () {
        return name;
    }

    /**
     * Adds a definition clause to this predicate, plus instructions to implement it.
     *
     * @param definition   A definition clause to add to this predicate.
     * @param instructions A list of instructions to add to the definition.
     */
    public void addInstructions ( HtPredicate definition,
                                  SizeableList <HiTalkWAMInstruction> instructions ) {
//        int oldLength=0;

        merge(definition);
        addInstructions(instructions);
    }

    private void merge ( HtPredicate predicate ) {
        //todo
    }

    /**
     * Adds some instructions sequentially, after any existing instructions, to the clause.
     *
     * @param instructions The instructions to add to the clause.
     */
    public void addInstructions ( Collection <HiTalkWAMInstruction> instructions ) {
        this.instructions.addAll(instructions);
    }

    /**
     * Gets the wrapped sentence in the logical language over WAMCompiledClauses.
     *
     * @return The wrapped sentence in the logical language.
     */
    public
    HiTalkWAMCompiledPredicate getT () {
        return this;
    }

    /**
     * Provides the mapping from registers to variable names for this compiled clause.
     *
     * @return The mapping from registers to variable names for this compiled clause.
     */
    public
    Map <Byte, Integer> getVarNames () {
        return varNames;
    }

    /**
     * Provides the set of variables in the clause that are not anonymous or bound.
     *
     * @return The set of variables in the clause that are not anonymous or bound.
     */
    public
    Set <Integer> getNonAnonymousFreeVariables () {
        return nonAnonymousFreeVariables;
    }

    /**
     * {@inheritDoc}
     */
    public
    long sizeof () {
        return instructions.sizeof();
    }

    /**
     * {@inheritDoc}
     */
    public
    List <HiTalkWAMInstruction> getInstructions () {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * {@inheritDoc}
     */
    public
    void setOptimizedInstructions ( SizeableList <HiTalkWAMInstruction> instructions ) {
        unoptimizedInstructions = this.instructions;
        this.instructions = instructions;
    }

    /**
     * {@inheritDoc}
     */
    public
    List <HiTalkWAMInstruction> getUnoptimizedInstructions () {
        return unoptimizedInstructions;
    }

    /**
     * Emits the binary byte code for the clause into a machine, writing into the specified byte array. The state of
     * this clause is changed to 'Linked' to indicate that it has been linked into a binary machine.
     *
     * @param buffer    The code buffer to write to.
     * @param machine   The binary machine to resolve call-points against, and to record as being linked into.
     * @param callPoint The call point within the machine, at which the code is to be stored.
     * @throws LinkageException If required symbols to link to cannot be found in the binary machine.
     */
    public void emitCode(ByteBuffer buffer,
                         HiTalkWAMResolvingMachine<HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> machine,
                         WAMCallPoint callPoint) throws LinkageException {
        // Ensure that the size of the instruction listing does not exceed max int (highly unlikely).
        if (sizeof() > Integer.MAX_VALUE) {
            throw new IllegalStateException("The instruction listing size exceeds Integer.MAX_VALUE.");
        }

        // Used to keep track of the size of the emitted code, in bytes, as it is written.
        int length = 0;

        // Insert the compiled code into the byte code machine's code area.
        for (HiTalkWAMInstruction instruction : instructions) {
            instruction.emitCode(buffer, machine);
            length += instruction.sizeof();
        }

        // Keep record of the machine that the code is hosted in, and the call point of the functor within the machine.
        this.machine = machine;
        this.callPoint = callPoint;

        // Record the fact that the code is now linked into a machine.
        this.status = LinkStatus.Linked;
    }
}

