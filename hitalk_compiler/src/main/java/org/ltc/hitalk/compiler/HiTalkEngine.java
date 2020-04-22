package org.ltc.hitalk.compiler;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.task.PreCompilerTask;

import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;


/**
 *
 */

public
class HiTalkEngine<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends HtResolutionEngine<T, PreCompilerTask<T>, P, Q, PC, QC> {

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param compiler
     */
    public HiTalkEngine(
            ITermFactory termFactory,
            ISymbolTable<Integer, String, Object> symbolTable,
            ICompiler<T, P, Q, PC, QC> compiler,
            IResolver<PC, QC> resolver,
            HtPrologParser parser,
            IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler) {

        super(symbolTable, termFactory, compiler, resolver, parser, preCompiler);
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    @Override
    public void reset() {
    }

    @Override
    public void forEach(Consumer action) {
//TODO
    }

    /**
     * Creates a {@link Spliterator} over the elements described by this
     * {@code Iterable}.
     *
     * @return a {@code Spliterator} over the elements described by this
     * {@code Iterable}.
     * @implSpec The default implementation creates an
     * <em><a href="Spliterator.html#binding">early-binding</a></em>
     * spliterator from the iterable's {@code Iterator}.  The spliterator
     * inherits the <em>fail-fast</em> properties of the iterable's iterator.
     * @implNote The default implementation should usually be overridden.  The
     * spliterator returned by the default implementation has poor splitting
     * capabilities, is unsized, and does not report any spliterator
     * characteristics. Implementing classes can nearly always provide a
     * better implementation.
     * @since 1.8
     */
    public Spliterator<Set<HtVariable>> spliterator() {
        return null;//todo
    }
}