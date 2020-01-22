package org.ltc.hitalk.term.io;

import org.ltc.hitalk.term.io.Options.Option;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.function.Predicate;

/**
 *
 */
public class HtMethodDef {

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
    public HtMethodDef(String methodName, Predicate<IFunctor> body, int arity, Option... options) {
        this.methodName = methodName;
        this.arity = arity;
        this.options = options;
    }

    public static HtMethodDef createMethod(String methodName, int arity, Option... options) {
        return new HtMethodDef(methodName, body, arity, options);//foo/n
    }
}
