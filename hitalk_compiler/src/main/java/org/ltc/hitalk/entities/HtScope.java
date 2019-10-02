package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.beans.PropertyChangeListener;

import static org.ltc.hitalk.parser.PrologAtoms.*;

/**
 *
 */
public
class HtScope implements IPropertyOwner {
    protected
    PropertyOwner owner;

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return owner.getPropLength();
    }

    @Override
    public
    void addListener ( PropertyChangeListener listener ) {
        owner.addListener(listener);
    }

    @Override
    public
    void removeListener ( PropertyChangeListener listener ) {
        owner.removeListener(listener);
    }

    @Override
    public
    void fireEvent ( IProperty property, Term value ) {
        owner.fireEvent(property, value);
    }

    @Override
    public
    Term getValue ( HiTalkStream.Properties property ) {
        return owner.getValue(property);
    }

    /**
     * @param property
     * @param value
     */
    @Override
    public
    void setValue ( HiTalkStream.Properties property, Term value ) {
        owner.setValue(property, value);
    }

    /**
     *
     */
    public
    enum Kind {
        PRIVATE("private"),
        PROTECTED("protected"),
        PUBLIC("public");

        private final String s;

        /**
         * @param s
         */
        private
        Kind ( String s ) {
            this.s = s;
        }

        /**
         * @return
         */
        public
        String getS () {
            return s;
        }
    }

    /**
     * @param kind
     */
    public
    HtScope ( Kind kind, HtProperty[] properties ) {
        this.kind = kind;
        this.properties = properties;
    }

    /**
     * @param name
     * @param properties
     */
    public
    HtScope ( String name, HtProperty[] properties ) {
        this.properties = properties;
        switch (name) {
            case PUBLIC:
                kind = Kind.PUBLIC;
                break;
            case PROTECTED:
                kind = Kind.PROTECTED;
                break;
            case PRIVATE:
                kind = Kind.PRIVATE;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + name);
        }
    }

    private final Kind kind;
    private final HtProperty[] properties;

    /**
     * @return
     */
    public
    Kind getKind () {
        return kind;
    }
}
