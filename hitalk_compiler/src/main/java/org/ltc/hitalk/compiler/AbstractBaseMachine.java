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
package org.ltc.hitalk.compiler;

import com.thesett.common.util.doublemaps.SymbolTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class AbstractBaseMachine {

    final protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * Holds the machines symbol table.
     */
    protected SymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the machines symbol name interner.
     */
    protected IVafInterner interner;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public AbstractBaseMachine ( SymbolTable <Integer, String, Object> symbolTable, IVafInterner interner ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
    }

    public AbstractBaseMachine () {

    }

    /**
     * Provides the symbol table.
     *
     * @return The symbol table.
     */
    public SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    /**
     * Provides the interner.
     *
     * @return The interner.
     */
    public IVafInterner getInterner () {
        return interner;
    }
}

