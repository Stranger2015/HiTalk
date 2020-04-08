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

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.util.EnumSet;
import java.util.List;

import static java.lang.String.format;
import static org.ltc.hitalk.term.OpSymbolFunctor.Associativity.*;

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
public class OpSymbolFunctor extends HtFunctor implements Comparable<OpSymbolFunctor>, Cloneable {
//    private ITerm result;
//    private ITerm result1;
    /**
     *
     */
    protected boolean builtIn;

    public int leftPriority;
    public int rightPriority;
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

    /**
     * @param name
     * @param textName
     * @param associativity
     * @param priority
     */
    public OpSymbolFunctor(int name, String textName, Associativity associativity, int priority) {
        this(name, textName, associativity, priority, true);
    }

    /**
     * @param name
     * @param priority
     * @param result
     * @param result1
     */
    public OpSymbolFunctor(String name, int priority, ITerm result, ITerm result1) {
        super(-1);
        this.textName = name;
        this.priority = priority;
        args.addHead(result);
        args.addHead(result1);
    }

    public OpSymbolFunctor(String name, ITerm result) {
        this(name, null, -1, result, null);
    }

    public OpSymbolFunctor(String image,
                           Associativity assoc,
                           int priority,
                           ITerm result,
                           ITerm result1) {
        this(image, priority, result, result1);
    }

    /**
     * @param image
     * @param yf
     * @param yf1
     * @param result
     */
    public OpSymbolFunctor(String image,
                           Associativity yf,
                           int yf1,
                           ITerm result) {
        this(image, yf, yf1, result, null);
    }


    public int getLeftPriority() {
        return leftPriority;
    }

    public int getRightPriority() {
        return rightPriority;
    }

    /**
     * @param name
     */
    public OpSymbolFunctor(String name) {
        this(name, null, -1, null, null);
    }

    public OpSymbolFunctor(String name, ITerm result, ITerm result1) {
        this(name, null, -1, result, result1);
    }


    /**
     * @return
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    /**
     * Creates a new operator with the specified name and arguments.
     *
     * @param textName      The name of the operator.
     * @param associativity Specifies the associativity of the operator.
     * @param priority      The operators priority.
     */
    public OpSymbolFunctor(int name,
                           String textName,
                           Associativity associativity,
                           int priority,
                           boolean builtIn) {
        super(name, new ListTerm(associativity.arity));
        this.priority = priority;
        this.builtIn = builtIn;

        // Check that there is at least one and at most two arguments.
        if (args.size() < 1 || args.size() > 2) {
            throw new IllegalArgumentException("An operator has minimum 1 and maximum 2 arguments.");
        }

        this.textName = textName;
        this.priority = priority;
        this.associativity = associativity;
    }

    /**
     *
     */
    public static boolean isCorrectOrder(Associativity l, Associativity r) {
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
     * @return
     */
    public int getArity() {
        return args.getHeads().size();
    }

    /**
     * Sets the arguments of this operator. It can be convenient to be able to set the outside of the constructor, for
     * example, when parsing may want to create the operator first and fill in its arguments later.
     *
     * @param arguments The arguments the operator is applied to.
     */
    @Override
    public void setArguments(List<ITerm> arguments) {
        // Check that there is at least one and at most two arguments.
        if ((arguments.size() < 1) || (arguments.size() > 2)) {
            throw new IllegalArgumentException("An operator has minimum 1 and maximum 2 arguments.");
        }

        this.args.setHeads(arguments);
    }

    public boolean isListTerm() {
        return true;
    }

    public ListTerm getArgs() {
        return args;
    }

    /**
     * Provides the symbols associativity.
     *
     * @return The symbols associativity.
     */
    public Associativity getAssociativity() {
        return associativity;
    }

    /**
     * Provides the symbols priority.
     *
     * @return The symbols priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Provides the symbols textual representation.
     *
     * @return The symbols textual representation.
     */
    public String getTextName() {
        return textName;
    }

    /**
     * Provides the symbols fixity, derived from its associativity.
     *
     * @return The symbols fixity.
     */
    public Fixity getFixity() {
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
                throw new IllegalStateException("Unknown fixity.");
        }
    }

    /**
     * Reports whether this operator is an prefix operator.
     *
     * @return <tt>true <tt>if this operator is an prefix operator.
     */
    public boolean isPrefix() {
        EnumSet<Associativity> prefixOps = EnumSet.of(fx, fy, hx, hy);
        return prefixOps.contains(associativity);
    }

    /**
     * Reports whether this operator is an postfix operator.
     *
     * @return <tt>true <tt>if this operator is an postfix operator.
     */
    public boolean isPostfix() {
        EnumSet<Associativity> postfixOps = EnumSet.of(xf, yf);
        return postfixOps.contains(associativity);
    }

    /**
     * Reports whether this operator is an infix operator.
     *
     * @return <tt>true <tt>if this operator is an infix operator.
     */
    public boolean isInfix() {
        EnumSet<Associativity> infixOps = EnumSet.of(xfx, xfy, yfx);
        return infixOps.contains(associativity);
    }

    /**
     * Reports whether this operator is right associative.
     *
     * @return <tt>true</tt> if this operatis is right associative.
     */
    public boolean isRightAssociative() {
        EnumSet<Associativity> rightOps = EnumSet.of(xfy, fy, hy);
        return rightOps.contains(associativity);
    }

    /**
     * Reports whether this operatis is left associative.
     *
     * @return <tt>true</tt> if this operatis is left associative.
     */
    public boolean isLeftAssociative() {
        EnumSet<Associativity> leftOps = EnumSet.of(yfx, yf);
        return leftOps.contains(associativity);
    }

//    /**
//     * Compares this object with the specified object for order, providing a negative integer, zero, or a positive
//     * integer as this symbols priority is less than, equal to, or greater than the comparator. If this symbol is 'less'
//     * than another that means that it has a lower priority value, which means that it binds more tightly.
//     *
//     * @param o The object to be compared with.
//     * @return A negative integer, zero, or a positive integer as this symbols priority is less than, equal to, or
//     * greater than the comparator.
//     */
//    public int compareTo ( Object o ) {
//        return priority - ((HlOpSymbol) o).priority;
//    }

    /**
     * Provides a copied clone of the symbol. The available symbols that a parser recognizes may be set up in a symbol
     * table. When an instance of a symbol is encountered it may be desireable to copy the symbol from the table. Using
     * this method an HiLogOpSymbol may act both as a template for symbols and as an individual instance of a symbols
     * occurance.
     *
     * @return A shallow copy of the symbol.
     */
    public OpSymbolFunctor copySymbol() {
        try {
            return (OpSymbolFunctor) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException but clone is defined on Operator and " +
                    "should not fail.", e);
        }
    }

    /**
     * Outputs the operator name, associativity, priority, arity and arguments as a string, used mainly for debugging
     * purposes.
     *
     * @return The operator as a string.
     */
    public String toString() {
        return format("%s: { name = %s, priority = %d, associativity = %s }",
                getClass().getSimpleName(), textName, priority, associativity);
    }

    /**
     * Creates a shallow clone of this operator symbol.
     *
     * @return A shallow clone of this object.
     * @throws CloneNotSupportedException If cloning fails.
     */
    protected Object clone() throws CloneNotSupportedException {
        // Create a new state and copy the existing board position into it

        return super.clone();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     *
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    public int compareTo(@NotNull OpSymbolFunctor o) {
        return 0;
    }

    public ITerm getResult() {
        return args.getHead(0);
    }

    public ITerm getResult1() {
        return args.getHead(1);
    }

    /**
     * @return
     */
    public boolean isList() {
        return false;
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
        public int arity;

        Associativity(int arity) {
            this.arity = arity;
        }

        /**
         * @return
         */
        public Associativity round() {
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
        public int lprio() {
            switch (this) {
                case yfx:
                case yf:
                    return 1;
                default:
                    return 0;
            }
        }

        public int rprio() {
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
