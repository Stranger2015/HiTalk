package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.wam.builtins.*;
import com.thesett.aima.logic.fol.wam.compiler.DefaultBuiltIn;
import com.thesett.common.util.Function;
import com.thesett.common.util.ReflectionUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * BuiltInTransform implements a compilation transformation over term syntax trees, that substitutes for functors that
 * map onto Prolog built-ins, an extension of the functor type that implements the built-in.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Transform functors to built in functors where appropriate.
 *     <td> {@link BuiltInFunctor}, {@link VariableAndFunctorInterner}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HiTalkBuiltInTransform implements Function <Functor, Functor> {
    /**
     * Holds a mapping from functor names to built-in implementations.
     */
    private final Map <FunctorName, Class <? extends BuiltInFunctor>> builtIns = new HashMap <>();

    /**
     * Holds the default built in, for standard compilation and interners and symbol tables.
     */
    private final HiTalkDefaultBuiltIn defaultBuiltIn;

    /**
     * Initializes the built-in transformation by population the the table of mappings of functors onto their built-in
     * implementations.
     *
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public
    HiTalkBuiltInTransform ( HiTalkDefaultBuiltIn defaultBuiltIn ) {
        this.defaultBuiltIn = defaultBuiltIn;

        builtIns.put(new FunctorName("true", 0), True.class);
        builtIns.put(new FunctorName("fail", 0), Fail.class);
        builtIns.put(new FunctorName("!", 0), Cut.class);

        /*builtIns.put(new FunctorName("=", 2), Unifies.class);
        builtIns.put(new FunctorName("\\=", 2), NonUnifies.class);*/
        builtIns.put(new FunctorName(";", 2), Disjunction.class);
        builtIns.put(new FunctorName(",", 2), Conjunction.class);
        builtIns.put(new FunctorName("call", 1), Call.class);
        /*builtIns.put(new FunctorName("is", 2), Is.class);
        builtIns.put(new FunctorName(">", 2), GreaterThan.class);
        builtIns.put(new FunctorName(">=", 2), GreaterThanOrEqual.class);
        builtIns.put(new FunctorName("<", 2), LessThan.class);
        builtIns.put(new FunctorName("=<", 2), LessThanOrEqual.class);
        builtIns.put(new FunctorName("integer", 1), IntegerCheck.class);
        builtIns.put(new FunctorName("float", 1), FloatCheck.class);*/
    }

    /**
     * Applies a built-in replacement transformation to functors. If the functor matches built-in, a
     * {@link BuiltInFunctor} is created with a mapping to the functors built-in implementation, and the functors
     * arguments are copied into this new functor. If the functor does not match a built-in, it is returned unmodified.
     *
     * @param functor The functor to attempt to map onto a built-in.
     * @return The functor unmodified, or a {@link BuiltInFunctor} replacement for it.
     */
    public
    Functor apply ( Functor functor ) {
        FunctorName functorName = defaultBuiltIn.getInterner().getFunctorFunctorName(functor);

        Class <? extends BuiltInFunctor> builtInClass;

        builtInClass = builtIns.get(functorName);

        if (builtInClass != null) {
            return ReflectionUtils.newInstance(ReflectionUtils.getConstructor(builtInClass, new Class[]{Functor.class, DefaultBuiltIn.class}), new Object[]{functor, defaultBuiltIn});
        }
        else {
            return functor;
        }
    }
}
