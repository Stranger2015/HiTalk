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

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys;
import com.thesett.aima.logic.fol.wam.optimizer.Matcher;
import com.thesett.aima.logic.fol.wam.optimizer.StateMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction;

import java.util.Deque;
import java.util.LinkedList;

import static java.lang.Boolean.TRUE;
import static org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction.HiTalkWAMInstructionSet.*;

/**
 * Performs an optimization pass for specialized constant instructions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Optimize constant instructions in the head of a clause.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class HtOptimizeInstructions implements StateMachine <HiTalkWAMInstruction, HiTalkWAMInstruction> {

    /**
     * Builds an instruction optimizer.
     *
     * @param symbolTable The symbol table to get instruction analysis from.
     * @param interner    The functor and variable name interner.
     */
    public HtOptimizeInstructions ( ISymbolTable <Integer, String, Object> symbolTable,
                                    IVafInterner interner ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
    }

    /**
     * Defines the possible states that this state machine can be in.
     */
    private enum State {
        /**
         * No Match.
         */
        NM,

        /**
         * UnifyVar to UnifyVoid elimination.
         */
        UVE,

        /**
         * SetVar to SetVoid elimination.
         */
        SVE;
    }

    /**
     * Holds the matcher that is driving this state machine.
     */
    private Matcher <HiTalkWAMInstruction, HiTalkWAMInstruction> matcher;

    /**
     * Holds the current state machine state.
     */
    private HtOptimizeInstructions.State state = HtOptimizeInstructions.State.NM;

    /**
     * Holds a buffer of pending instructions to output.
     */
    private final Deque <HiTalkWAMInstruction> buffer = new LinkedList <>();

    /**
     * The symbol table.
     */
    protected final ISymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the variable and functor name interner for the machine.
     */
    private final IVafInterner interner;

    /**
     * Counts the number of void variables seen in a row.
     */
    private int voidCount;

    /**
     * {@inheritDoc}
     */
    public void apply ( HiTalkWAMInstruction next ) {
        shift(next);

        // Anonymous or singleton variable optimizations.
        if ((UnifyVar == next.getMnemonic()) && isVoidVariable(next)) {
            if (state != HtOptimizeInstructions.State.UVE) {
                voidCount = 0;
            }

            discard((voidCount == 0) ? 1 : 2);

            HiTalkWAMInstruction unifyVoid = new HiTalkWAMInstruction(UnifyVoid, HiTalkWAMInstruction.REG_ADDR, (byte) ++voidCount);
            shift(unifyVoid);
            state = HtOptimizeInstructions.State.UVE;

            /*log.fine(next + " -> " + unifyVoid);*/
        } else if ((SetVar == next.getMnemonic()) && isVoidVariable(next)) {
            if (state != HtOptimizeInstructions.State.SVE) {
                voidCount = 0;
            }

            discard((voidCount == 0) ? 1 : 2);

            HiTalkWAMInstruction setVoid = new HiTalkWAMInstruction(SetVoid, HiTalkWAMInstruction.REG_ADDR, (byte) ++voidCount);
            shift(setVoid);
            state = HtOptimizeInstructions.State.SVE;

            /*log.fine(next + " -> " + setVoid);*/
        } else if ((GetVar == next.getMnemonic()) && (next.getMode1() == HiTalkWAMInstruction.REG_ADDR) &&
                (next.getReg1() == next.getReg2())) {
            discard(1);

            /*log.fine(next + " -> eliminated");*/

            state = HtOptimizeInstructions.State.NM;
        }

        // Constant optimizations.
        else if ((UnifyVar == next.getMnemonic()) && isConstant(next) && isNonArg(next)) {
            discard(1);

            FunctorName functorName = interner.getDeinternedFunctorName(next.getFunctorNameReg1());
            HiTalkWAMInstruction unifyConst = new HiTalkWAMInstruction(UnifyConstant, functorName);
            shift(unifyConst);
            flush();
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> " + unifyConst);*/
        } else if ((GetStruc == next.getMnemonic()) && isConstant(next) && isNonArg(next)) {
            discard(1);
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> eliminated");*/
        } else if ((GetStruc == next.getMnemonic()) && isConstant(next) && !isNonArg(next)) {
            discard(1);

            HiTalkWAMInstruction getConst = new HiTalkWAMInstruction(GetConstant, next.getMode1(), next.getReg1(), next.getFn());
            shift(getConst);
            flush();
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> " + getConst);*/
        } else if ((PutStruc == next.getMnemonic()) && isConstant(next) && isNonArg(next)) {
            discard(1);
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> eliminated");*/
        } else if ((PutStruc == next.getMnemonic()) && isConstant(next) && !isNonArg(next)) {
            discard(1);

            HiTalkWAMInstruction putConst = new HiTalkWAMInstruction(PutConstant, next.getMode1(), next.getReg1(), next.getFn());
            shift(putConst);
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> " + putConst);*/
        } else if ((SetVal == next.getMnemonic()) && isConstant(next) && isNonArg(next)) {
            discard(1);

            FunctorName functorName = interner.getDeinternedFunctorName(next.getFunctorNameReg1());
            HiTalkWAMInstruction setConst = new HiTalkWAMInstruction(SetConstant, functorName);
            shift(setConst);
            flush();
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> " + setConst);*/
        }

        // List optimizations.
        else if ((GetStruc == next.getMnemonic()) &&
                ("cons".equals(next.getFn().getName()) && (next.getFn().getArity() == 2))) {
            discard(1);

            HiTalkWAMInstruction getList = new HiTalkWAMInstruction(GetList, next.getMode1(), next.getReg1());
            shift(getList);
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> " + getList);*/
        } else if ((PutStruc == next.getMnemonic()) &&
                ("cons".equals(next.getFn().getName()) && (next.getFn().getArity() == 2))) {
            discard(1);

            HiTalkWAMInstruction putList = new HiTalkWAMInstruction(PutList, next.getMode1(), next.getReg1());
            shift(putList);
            state = HtOptimizeInstructions.State.NM;

            /*log.fine(next + " -> " + putList);*/
        }

        // Default.
        else {
            state = HtOptimizeInstructions.State.NM;
            flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end () {
        flush();
    }

    /**
     * {@inheritDoc}
     */
    public void setMatcher ( Matcher <HiTalkWAMInstruction, HiTalkWAMInstruction> matcher ) {
        this.matcher = matcher;
    }

    /**
     * Checks if the term argument to an instruction was a constant.
     *
     * @param instruction The instruction to test.
     * @return <tt>true</tt> iff the term argument to an instruction was a constant. <tt>false</tt> will be returned if
     * this information was not recorded, and cannot be determined.
     */
    public boolean isConstant ( HiTalkWAMInstruction instruction ) {
        Integer name = instruction.getFunctorNameReg1();

        if (name != null) {
            FunctorName functorName = interner.getDeinternedFunctorName(name);

            if (functorName.getArity() == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the term argument to an instruction was a singleton, non-argument position variable. The variable must
     * also be non-permanent to ensure that singleton variables in queries are created.
     *
     * @param instruction The instruction to test.
     * @return <tt>true</tt> iff the term argument to the instruction was a singleton, non-argument position variable.
     */
    private boolean isVoidVariable ( HiTalkWAMInstruction instruction ) {
        String symbolKey = instruction.getStringReg1();

        if (symbolKey != null) {
            Integer count = (Integer) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_VAR_OCCURRENCE_COUNT);
            Boolean nonArgPositionOnly = (Boolean) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_VAR_NON_ARG);
            Integer allocation = (Integer) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_ALLOCATION);

            boolean singleton = (count != null) && count.equals(1);
            boolean nonArgPosition = (nonArgPositionOnly != null) && TRUE.equals(nonArgPositionOnly);
            boolean permanent =
                    (allocation != null) && ((byte) ((allocation & 0xff00) >> 8) == HiTalkWAMInstruction.STACK_ADDR);

            if (singleton && nonArgPosition && !permanent) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the term argument to an instruction was in a non-argument position.
     *
     * @param instruction The instruction to test.
     * @return <tt>true</tt> iff the term argument to an instruction was in a non-argument position. <tt>false</tt> will
     * be returned if this information was not recorded, and cannot be determined.
     */
    private boolean isNonArg ( HiTalkWAMInstruction instruction ) {
        String symbolKey = instruction.getStringReg1();

        if (symbolKey != null) {
            Boolean nonArgPositionOnly = (Boolean) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_FUNCTOR_NON_ARG);

            if (TRUE.equals(nonArgPositionOnly)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Discards the specified number of most recent instructions from the output buffer.
     *
     * @param n The number of instructions to discard.
     */
    private void discard ( int n ) {
        for (int i = 0; i < n; i++) {
            buffer.pollLast();
        }
    }

    /**
     * Adds an instruction to the output buffer.
     *
     * @param instruction The instruction to add.
     */
    private void shift ( HiTalkWAMInstruction instruction ) {
        buffer.offer(instruction);
    }

    /**
     * Flushes the output buffer.
     */
    private void flush () {
        matcher.sinkAll(buffer);
    }
}
