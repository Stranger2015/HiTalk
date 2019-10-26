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
package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.*;
import org.ltc.hitalk.wam.machine.HiTalkWAMMachine;

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
public class HiTalkInstructionCompiler extends PrologInstructionCompiler
        implements PrologBuiltIn {

//public final static FunctorName OBJECT = new FunctorName("object", 2);
//public final static FunctorName END_OBJECT = new FunctorName("end_object", 0);
//public final static FunctorName CATEGORY = new FunctorName("category",2);
//public final static FunctorName END_CATEGORY = new FunctorName("end_category",0);
//public final static FunctorName PROTOCOL = new FunctorName("protocol", 2);
//public final static FunctorName END_PROTOCOL =new FunctorName( "end_protocol",0);

    /**
     * Creates a new HiTalkInstructionCompiler.
     *
     * @param symbolTable    The symbol table.
     * @param interner       The machine to translate functor and variable names.
     * @param defaultBuiltIn
     * @param parser
     */
    public HiTalkInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                       VariableAndFunctorInterner interner,
                                       HiTalkDefaultBuiltIn defaultBuiltIn,
                                       LogicCompilerObserver <
                                               HiTalkWAMCompiledPredicate,
                                               HiTalkWAMCompiledQuery> observer,
                                       PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, observer, parser);
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
    public SizeableLinkedList <HiTalkWAMInstruction> compileBodyCall ( Functor expression,
                                                                       boolean isFirstBody,
                                                                       boolean isLastBody,
                                                                       boolean chainRule,
                                                                       int permVarsRemaining ) {
        return defaultBuiltIn.compileBodyCall(expression, isFirstBody, isLastBody, chainRule, permVarsRemaining);
    }
}
