package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.function.Predicate;

/**
 *
 */
public class HtMethodDef extends HtProperty {

    public String methodName;
    public Predicate<IFunctor> body;
    public int arity;
    public HtNonVar[] options;

    /**
     * @param methodName
     * @param body
     * @param arity
     * @param options
     */
    public HtMethodDef(IFunctor methodName,
                       int arity,
                       Predicate<IFunctor> body,
                       HtNonVar value,
                       HtNonVar... options) {
        super(methodName, value);
        this.arity = arity;
        this.body = body;
        this.options = options;
    }

    public HtMethodDef(IFunctor functor, int arity, Predicate<IFunctor> body, HtNonVar[] opts) {
        super(functor, arity, body, opts);
    }

    /**
     * @param methodName
     * @param body
     * @param arity
     * @param options
     * @return
     */
    public static HtMethodDef createMethod(IFunctor methodName,
                                           int arity,
                                           Predicate<IFunctor> body,
                                           HtNonVar value,
                                           HtNonVar... options) {
        return new HtMethodDef(methodName, arity, body, value, options);
    }
}
