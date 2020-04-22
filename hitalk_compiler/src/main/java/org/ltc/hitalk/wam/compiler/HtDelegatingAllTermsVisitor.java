package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.wam.printer.IAllTermsVisitor;

public class HtDelegatingAllTermsVisitor implements IAllTermsVisitor {
    private final IAllTermsVisitor delegate;

    public HtDelegatingAllTermsVisitor(IAllTermsVisitor delegate) {

        this.delegate = delegate;
    }

    public IAllTermsVisitor getDelegate() {
        return delegate;
    }
}
