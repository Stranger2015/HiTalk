package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.*;

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
interface HtAllTermsVisitor extends TermVisitor,
                                    FunctorVisitor,
                                    VariableVisitor,
                                    HtClauseVisitor,
                                    IntegerTypeVisitor,
                                    LiteralTypeVisitor,
                                    HtPredicateVisitor {
}
