package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.search.Operator;
import com.thesett.common.util.StackQueue;
import org.ltc.hitalk.parser.HtClause;

import java.util.Iterator;
import java.util.LinkedList;

public
class HtPredicate<T extends HtClause> extends BaseTerm implements Term {
    /**
     * The clauses that make up this predicate.
     */
    protected T[] body;

    /**
     * Creates a predicate formed from a set of clauses.
     *
     * @param body The clauses that make up the body of the predicate.
     */
    public
    HtPredicate ( T[] body ) {
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public
    Term getValue () {
        return this;
    }

    /**
     * Gets all of the clauses that make up the body of the predicate.
     *
     * @return All of the clauses that make up the body of the predicate.
     */
    public
    T[] getBody () {
        return body;
    }

    /**
     * {@inheritDoc}
     */
    public
    void free () {
    }

    /**
     * {@inheritDoc}
     */
    public
    Iterator <Operator <Term>> getChildren ( boolean reverse ) {
        if ((traverser != null) && (traverser instanceof PredicateTraverser)) {
            return ((PredicateTraverser) traverser).traverse(this, reverse);
        }
        else {
            LinkedList <Operator <Term>> resultList = null;

            if (!reverse) {
                resultList = new LinkedList <Operator <Term>>();
            }
            else {
                resultList = new StackQueue <Operator <Term>>();
            }

            if (body != null) {
                for (Term bodyTerm : body) {
                    resultList.add(bodyTerm);
                }
            }

            return resultList.iterator();
        }
    }

    /**
     * {@inheritDoc}
     */
    public
    void accept ( TermVisitor visitor ) {
        if (visitor instanceof PredicateVisitor) {
            ((PredicateVisitor) visitor).visit(this);
        }
        else {
            super.accept(visitor);
        }
    }

    /**
     * {@inheritDoc}
     */
    public
    String toString ( VariableAndFunctorInterner interner, boolean printVarName, boolean printBindings ) {
        String result = "";

        if (body != null) {
            for (int i = 0; i < body.length; i++) {
                result +=
                        body[i].toString(interner, printVarName, printBindings) + ((i < (body.length - 1)) ? "\n" : "");
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public
    String toString () {
        String bodyString = "[";

        if (body != null) {
            for (int i = 0; i < body.length; i++) {
                bodyString += body[i].toString() + ((i < (body.length - 1)) ? ", " : "");
            }
        }

        bodyString += "]";

        return "Clause: [ body = " + bodyString + " ]";
    }
}
