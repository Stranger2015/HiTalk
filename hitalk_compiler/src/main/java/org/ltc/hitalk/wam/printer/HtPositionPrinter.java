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
    package org.ltc.hitalk.wam.printer;

    import com.thesett.aima.logic.fol.Functor;
    import com.thesett.aima.logic.fol.Variable;
    import com.thesett.text.api.model.TextTableModel;
    import org.ltc.hitalk.compiler.IVafInterner;
    import org.ltc.hitalk.core.utils.ISymbolTable;

    import static org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction.STACK_ADDR;

    /**
     * PositionPrinter prints some positional context information about functors and how they relate to their compiled form.
     * The position printer will prints the name of the functor, argument or variable and whether the current position is
     * within the functor head or last body element.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Print the positional context information.
     * </table></pre>
     *
     * @author Rupert Smith
     */
    public
    class HtPositionPrinter extends HtBasePrinter {
        /**
         * @param symbolTable
         * @param interner
         * @param traverser
         * @param i
         * @param printTable
         */
        public HtPositionPrinter ( ISymbolTable <Integer, String, Object> symbolTable,
                                   IVafInterner interner,
                                   IPositionalTermTraverser traverser,
                                   int i, TextTableModel printTable ) {
            super(symbolTable, interner, traverser, i, printTable);

        }

        /**
         * {@inheritDoc}
         */
        protected
        void enterFunctor ( Functor functor ) {
            String head = traverser.isInHead() ? "/head" : "";
            String last = traverser.isLastBodyFunctor() ? "/last" : "";
            String symKey = functor.getSymbolKey().toString();

            if (traverser.isTopLevel()) {
                addLineToRow("functor(" + symKey + ")" + head + last);
            }
            else {
                addLineToRow("arg(" + symKey + ")");
            }

            nextRow();
        }

        /**
         * {@inheritDoc}
         */
        protected
        void leaveFunctor ( Functor functor ) {
            nextRow();
        }

        /**
         * {@inheritDoc}
         */
        protected
        void enterVariable ( Variable variable ) {
            Integer allocation = (Integer) symbolTable.get(variable.getSymbolKey(), "allocation");
            String symKey = variable.getSymbolKey().toString();

            String allocString = "";

            if (allocation != null) {
                int slot = (allocation & (0xff));
                int mode = allocation >> 8;

                allocString = ((mode == STACK_ADDR) ? "Y" : "X") + slot;
            }

            addLineToRow("arg/var(" + symKey + ") " + allocString);
            nextRow();
        }
    }
