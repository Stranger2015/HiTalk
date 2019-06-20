package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.hitalk.entities.HtEntityIdentifier;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public
class HtEntityIterator<FN extends FunctorName, T extends HtEntityIdentifier.HtEntity> implements Iterator <T> {
    protected final Map <FN, T> entityTable;
    protected HtEntityIdentifier.HtEntity currentEntity;

    public
    HtEntityIterator ( Map <FN, T> entityTable ) {
        this.entityTable = entityTable;
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public
    boolean hasNext () {
        return entityTable.containsKey(currentEntity.getName());
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public
    T next () {
        final T entity = entityTable.get(currentEntity.getName());
        if (entity == null) {
            throw new NoSuchElementException();
        }
        currentEntity = entity;
        return entity;
    }
}
