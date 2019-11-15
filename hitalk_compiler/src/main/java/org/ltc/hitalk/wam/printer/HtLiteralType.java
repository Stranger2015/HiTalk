package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.term.HtBaseTerm;
import org.ltc.hitalk.term.ITermVisitor;

public abstract class HtLiteralType extends HtBaseTerm {
    /**
     * Reports whether or not this term is a constant (a number of a functor of arity zero).
     *
     * @return Always <tt>true</tt>.
     */
    public boolean isConstant () {
        return true;
    }

    /**
     * Reports whether or not this term is a ground term.
     *
     * @return Always <tt>true</tt>.
     */
    public boolean isGround () {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void accept ( ITermVisitor visitor ) {
        if (visitor instanceof ILiteralTypeVisitor) {
            ((ILiteralTypeVisitor) visitor).visit(this);
        } else {
            super.accept(visitor);
        }
    }
}

