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
package org.ltc.hitalk.term;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.util.EnumSet;

import static java.lang.String.format;
import static org.ltc.hitalk.term.HlOpSymbol.Associativity.*;


/**
 * Operators in first order logic, connect terms into compound units composed of many terms under the semantics of the
 * operator. Some operators are implicit in the language, such as the standard logic operators of 'and', 'or', 'implies'
 * and so on. Others may be invented and given a particular semantics specified in the language of first order logic
 * itself. An operator is a functor; it has a name, a number of arguments and forms a compound of terms. On top of the
 * behaviour of functors, operators also have precedences and associativities, that define in what order they bind to
 * neighbouring terms in the a textual form of the language. The precedences and associativity are not part of the logic
 * as such, they are hints to a parser.
 * <p>
 * <p/>An Operator is a functor, that additionally provides information to a parser as to its parsing priority and
 * associativity. An operator is a restricted case of a functor, in that it can take a minimum of one and a maximum of
 * two arguments.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Combine associativity and priority with a named composite.
 * <tr><td> Provide the textual representation of a symbol.
 * <tr><td> Report a symbols fixity.
 * <tr><td> Provide priority comparison of the symbol with other symbols.
 * <tr><td> Allow a symbol to act as a template to instantiate copies from.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class HlOpSymbol extends HtFunctor implements Comparable, Cloneable {

    public int lprio;
    public int rprio;
    /**
     * Holds the raw text name of this operator.
     */
    protected String textName;
    /**
     * Holds the associativity of this operator.
     */
    protected Associativity associativity;
    /**
     * Holds the priority of this operator.
     */
    protected int priority;
    private int name;

    /**
     * Creates a new operator with the specified name and arguments.
     *
     * @param textName      The name of the operator.
     * @param associativity Specifies the associativity of the operator.
     * @param priority      The operators priority.
     */
    public HlOpSymbol ( int name, String textName, Associativity associativity, int priority ) {
        super(name, null);

        // Check that there is at least one and at most two arguments.
        if ((arguments == null) || (arguments.length < 1) || (arguments.length > 2)) {
            throw new IllegalArgumentException("An operator has minimum 1 and maximum 2 arguments.");
        }

        this.textName = textName;
        this.priority = priority;
        this.associativity = associativity;
    }

    /**
     * 与えられた演算子の並び順が表記上正しいかどうかを判別します。
     */
    public static boolean isCorrectOrder ( Associativity l, Associativity r ) {
        l = l.round();
        r = r.round();
        switch (r) {
            case x:
            case fx:
                return l != x && l != xf;
            case xfx:
            case xf:
                return l != fx && l != xfx;
            default:
                throw new IllegalStateException(l + ", " + r);
        }
    }

    /**
     * Sets the arguments of this operator. It can be convenient to be able to set the outside of the constructor, for
     * example, when parsing may want to create the operator first and fill in its arguments later.
     *
     * @param arguments The arguments the operator is applied to.
     */
    public void setArguments ( Term[] arguments ) {
        // Check that there is at least one and at most two arguments.
        if ((arguments == null) || (arguments.length < 1) || (arguments.length > 2)) {
            throw new IllegalArgumentException("An operator has minimum 1 and maximum 2 arguments.");
        }

        this.arguments = arguments;
        this.arity = arguments.length;
    }

    /**
     * Provides the symbols associativity.
     *
     * @return The symbols associativity.
     */
    public Associativity getAssociativity () {
        return associativity;
    }

    /**
     * Provides the symbols priority.
     *
     * @return The symbols priority.
     */
    public int getPriority () {
        return priority;
    }

    /**
     * Provides the symbols textual representation.
     *
     * @return The symbols textual representation.
     */
    public String getTextName () {
        return textName;
    }

    /**
     * @return
     */
    public int getLprio () {
        return lprio;
    }

    /**
     * @return
     */
    public int getRprio () {
        return rprio;
    }


    /**
     * Provides the symbols fixity, derived from its associativity.
     *
     * @return The symbols fixity.
     */
    public Fixity getFixity () {
        switch (associativity) {
            case hy:
            case hx:

            case fx:
            case fy:
                return Fixity.Pre;

            case xf:
            case yf:
                return Fixity.Post;

            case xfx:
            case xfy:
            case yfx:
                return Fixity.In;

            default:
                throw new IllegalStateException("Unknown associativity.");
        }
    }

    /**
     * Reports whether this operator is an prefix operator.
     *
     * @return <tt>true <tt>if this operator is an prefix operator.
     */
    public boolean isPrefix () {
        EnumSet <Associativity> prefixOps = EnumSet.of(fx, fy, hx, hy);
        return prefixOps.contains(associativity);
    }

    /**
     * Reports whether this operator is an postfix operator.
     *
     * @return <tt>true <tt>if this operator is an postfix operator.
     */
    public boolean isPostfix () {
        EnumSet <Associativity> postfixOps = EnumSet.of(xf, yf);
        return postfixOps.contains(associativity);
    }

    /**
     * Reports whether this operator is an infix operator.
     *
     * @return <tt>true <tt>if this operator is an infix operator.
     */
    public boolean isInfix () {
        EnumSet <Associativity> infixOps = EnumSet.of(xfx, xfy, yfx);
        return infixOps.contains(associativity);
    }

    /**
     * Reports whether this operator is right associative.
     *
     * @return <tt>true</tt> if this operatis is right associative.
     */
    public boolean isRightAssociative () {
        EnumSet <Associativity> rightOps = EnumSet.of(xfy, fy, hy);
        return rightOps.contains(associativity);
    }

    /**
     * Reports whether this operatis is left associative.
     *
     * @return <tt>true</tt> if this operatis is left associative.
     */
    public boolean isLeftAssociative () {
        EnumSet <Associativity> leftOps = EnumSet.of(yfx, yf);
        return leftOps.contains(associativity);
    }

    /**
     * Compares this object with the specified object for order, providing a negative integer, zero, or a positive
     * integer as this symbols priority is less than, equal to, or greater than the comparator. If this symbol is 'less'
     * than another that means that it has a lower priority value, which means that it binds more tightly.
     *
     * @param o The object to be compared with.
     * @return A negative integer, zero, or a positive integer as this symbols priority is less than, equal to, or
     * greater than the comparator.
     */
    public int compareTo ( Object o ) {
        return priority - ((HlOpSymbol) o).priority;
    }

    /**
     * Provides a copied clone of the symbol. The available symbols that a parser recognizes may be set up in a symbol
     * table. When an instance of a symbol is encountered it may be desireable to copy the symbol from the table. Using
     * this method an HiLogOpSymbol may act both as a template for symbols and as an individual instance of a symbols
     * occurance.
     *
     * @return A shallow copy of the symbol.
     */
    public HlOpSymbol copySymbol () {
        try {
            return (HlOpSymbol) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException but clone is defined on Operator and should not fail.", e);
        }
    }

    /**
     * Outputs the operator name, associativity, priority, arity and arguments as a string, used mainly for debugging
     * purposes.
     *
     * @return The operator as a string.
     */
    public String toString () {
        return format("%s: [ name = %s, priority = %d, associativity = %s ]",
                getClass().getSimpleName(), textName, priority, associativity);
    }

    /**
     * Creates a shallow clone of this operator symbol.
     *
     * @return A shallow clone of this object.
     * @throws CloneNotSupportedException If cloning fails.
     */
    protected Object clone () throws CloneNotSupportedException {
        // Create a new state and copy the existing board position into it

        return super.clone();
    }

    public int getName () {
        return name;
    }

    /**
     * Defines the possible operator associativities.
     * /**
     */
    public enum Associativity {

        /**
         * 中置の二項演算子です。
         */
        xfx(2),

        /**
         * 中置の二項演算子です。(右結合)
         */
        xfy(2),

        /**
         * 中置の二項演算子です。(左結合)
         */
        yfx(2),

        /**
         * 前置演算子です。
         */
        fx(1), fy(1),
        hx(1), hy(1),

        /**
         * 前置演算子です。
         */
        xf(1), yf(1),

        /**
         * オペランドを表現します。
         */
        x(0);

        /**
         * この演算子が結合する項数です。
         */
        public final int arity;

        Associativity ( int arity ) {
            this.arity = arity;
        }

        /**
         * @return
         */
        public HlOpSymbol.Associativity round () {
            Associativity result;
            switch (this) {
                case xfy:
                case yfx:
                    result = xfx;
                    break;
                case fy:
                    result = fx;
                    break;
                case yf:
                    result = xf;
                    break;
                default:
                    result = this;
                    break;
            }
            return result;
        }

        /**
         * @return
         */
        public int lprio () {
            switch (this) {
                case yfx:
                case yf:
                    return 1;
                default:
                    return 0;
            }
        }

        public int rprio () {
            switch (this) {
                case xfy:
                case fy:
                    return 1;
                default:
                    return 0;
            }
        }
    }


    /**
     * Defines the possible operator fixities.
     */
    public enum Fixity {
        /**
         * Pre-fix.
         */
        Pre,

        /**
         * Post-fix.
         */
        Post,

        /**
         *
         */
        textName,

        /**
         * In-fix.
         */
        In
    }
}
