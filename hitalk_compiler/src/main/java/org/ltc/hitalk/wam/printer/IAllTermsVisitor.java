package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.IntegerTypeVisitor;
import com.thesett.aima.logic.fol.LiteralTypeVisitor;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.logic.fol.VariableVisitor;
import org.ltc.hitalk.compiler.IPredicateVisitor;
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
        TermVisitor,
        IFunctorVisitor,
        VariableVisitor,
        IClauseVisitor,
        IntegerTypeVisitor,
        LiteralTypeVisitor,
        IPredicateVisitor,
        IPackedDottedPairVisitor {
}
