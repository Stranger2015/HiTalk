package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.compiler.IPredicateVisitor;
import org.ltc.hitalk.term.ITermVisitor;
import org.ltc.hitalk.term.IVariableVisitor;
import org.ltc.hitalk.wam.compiler.IFunctorVisitor;

/**
 * AllTermsVisitor defines a composite visitor made up of the visitors over all types of terms.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Visit a term.
 * <tr><td> Visit a predicate.
 * <tr><td> Visit a clause.
 * <tr><td> Visit a functor.
 * <tr><td> Visit a variable.
 * <tr><td> Visit a literal.
 * <tr><td> Visit an integer literal.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
interface IAllTermsVisitor extends
        ITermVisitor,
        IFunctorVisitor,
        IVariableVisitor,
        IClauseVisitor,
        IIntegerVisitor,
        ILiteralTypeVisitor,
        IPredicateVisitor,
        IListTermVisitor {
}
