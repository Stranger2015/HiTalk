package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.io.Options.Option;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.function.Predicate;

/**
 *
 */
public class HtMethodDef extends HtProperty {

    public String methodName;
    public Predicate<IFunctor> body;
    public int arity;
    public Option[] options;

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
                       Option... options) {
        super(methodName, value);
        this.arity = arity;
        this.body = body;
        this.options = options;
    }

    /**
     * @param methodName
     * @param body
     * @param arity
     * @param options
     * @return
     */
    public static HtMethodDef createMethod(IFunctor methodName,
                                           Predicate<IFunctor> body,
                                           HtNonVar value,
                                           int arity,
                                           Option... options) {
        return new HtMethodDef(methodName, arity, body, value, options);
    }
}
