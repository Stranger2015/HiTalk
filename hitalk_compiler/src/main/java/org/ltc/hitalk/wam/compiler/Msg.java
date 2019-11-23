package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.io.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.ltc.hitalk.parser.HiLogParser.hilogApply;

/**
 *
 */
public class Msg {
    protected final Map <HtVariable, ITerm> dict1 = new HashMap <>();
    protected final Map <HtVariable, ITerm> dict2 = new HashMap <>();

    protected IVafInterner interner = Environment.instance().getInterner();

    /**
     * @param term1
     * @param term2
     * @return
     */
    public ITerm msg ( ITerm term1, ITerm term2 ) {
        ITerm result = null;
        if (term1.isVar() && (term1 == term2)) {
            result = updateDict((HtVariable) term1, term1, term2);
        } else if (term1.isVar() || term2.isVar()) {
            result = updateDictNewVar(term1, term2);
        } else if (!term1.isVar() && !term2.isVar() && !isSimilar(term1, term2)) {
            result = updateDictNewVar(term1, term2);
        } else if (term1.isConstant() && term2.isConstant()) {
            if (term1 == term2) {
                result = term1;
            } else {// for HiLog this is covered by cmp branch
                result = updateDictNewVar(term1, term2);
            }
        } else if (isSimilar(term1, term2) && term1.isCompound()) {
            IFunctor f1 = (IFunctor) term1;
            IFunctor f2 = (IFunctor) term2;
            //f(Xn) f(Yn)
            if (f1.getName() == f2.getName() && f1.getName() >= 0) {
                result = new HtFunctor(f1.getName(), f1.getArity(), 0);
            } else if (f1.isHiLog() && f2.isHiLog()) {
                final ITerm name1 = f1.getArgsAsListTerm().get(0);
                final ITerm name2 = f2.getArgsAsListTerm().get(0);
                final ITerm name = msg(name1, name2);
                final ListTerm lt = msgList(f1.getArgsAsListTerm(), f2.getArgsAsListTerm());
                result = new HtFunctor(hilogApply, new ITerm[]{name, lt});
            } else {
                result = new HtFunctor(hilogApply, new ITerm[]{updateDictNewVar(f1, f2), new ListTerm()});
            }

        } else if (term1.isList() && term2.isList()) {
            ListTerm lt1 = (ListTerm) term1;
            ListTerm lt2 = (ListTerm) term2;
            result = msgList(lt1, lt2);
        }

        return result;
    }

    /**
     * @param lt1
     * @param lt2
     * @return
     */
    private ListTerm msgList ( ListTerm lt1, ListTerm lt2 ) {
        int len = Math.min(lt1.size(), lt2.size());
        final ListTerm lt = new ListTerm(len);
        IntStream.range(0, len).forEachOrdered(i -> lt.setArgument(i, msg(lt1, lt2)));
        lt.newTail(lt1.size() != lt2.size());

        return lt;
    }

    /**
     * @param term1
     * @param term2
     * @return
     */
    private ITerm updateDictNewVar ( ITerm term1, ITerm term2 ) {
        return updateDict(new HtVariable(), term1, term2);
    }

    /**
     * @param term1
     * @param term2
     * @return
     */
    private static boolean isSimilar ( ITerm term1, ITerm term2 ) {
        return (term1.isConstant() && term1 == term2) || (term1.isCompound() && term2.isCompound());
    }

    /**
     * @param newVar
     * @param term1
     * @param term2
     * @return
     */
    private HtVariable updateDict ( HtVariable newVar, ITerm term1, ITerm term2 ) {
        dict1.put(newVar, term1);
        dict2.put(newVar, term2);
        return newVar;
    }
//    ============================================================================================

    /**
     * @return
     */
//    @Override
    public ExecutionContext getContext () {
        return null;
    }

    /**
     * @param context
     */
//    @Override
    public void setContext ( ExecutionContext context ) {

    }

    /**
     * @param max
     * @return
     */
//    @Override
    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    /**
     *
     */
//    @Override
    public void cancel () {

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
//    @Override
    public void run () {

    }
}

