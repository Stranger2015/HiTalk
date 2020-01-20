/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.compiler;

import com.thesett.aima.attribute.impl.IdAttribute;
import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.wam.compiler.IFunctor;

public class VafInterner implements IVafInterner {
    /**
     * Holds the interner that turns variable names into indexed integers.
     */
    private final IdAttribute.IdAttributeFactory<String> variableInterningFactory;

    /**
     * Holds the interner that turns functor names and arities into indexed integers.
     */
    private final IdAttribute.IdAttributeFactory<FunctorName> functorInterningFactory;

    /**
     * Creates an interner for variable and functor names, with the names created under the specified name spaces.
     *
     * @param variableNameSpace The name space for variables.
     * @param functorNameSpace  The name space for functors.
     */
    public VafInterner(String variableNameSpace, String functorNameSpace) {
        variableInterningFactory = IdAttribute.getFactoryForClass(variableNameSpace);
        functorInterningFactory = IdAttribute.getFactoryForClass(functorNameSpace);
    }

    public VafInterner(String[] nameSpace) {
        this(nameSpace[0], nameSpace[1]);

    }

    /**
     * {@inheritDoc}
     */
    public IdAttribute.IdAttributeFactory<FunctorName> getFunctorInterner() {
        return functorInterningFactory;
    }

    /**
     * {@inheritDoc}
     */
    public IdAttribute.IdAttributeFactory<String> getVariableInterner() {
        return variableInterningFactory;
    }

    /**
     * {@inheritDoc}
     */
    public int internFunctorName(String name, int numArgs) {
        FunctorName functorName = new FunctorName(name, numArgs);

        return getFunctorInterner().createIdAttribute(functorName).ordinal();
    }

    /**
     * {@inheritDoc}
     */
    public int internFunctorName(FunctorName name) {
        return getFunctorInterner().createIdAttribute(name).ordinal();
    }

    /**
     * {@inheritDoc}
     */
    public int internVariableName(String name) {
        return getVariableInterner().createIdAttribute(name).ordinal();
    }

    /**
     * {@inheritDoc}
     */
    public String getVariableName(int name) {
        if (name < 0) {
            return "_G" + (-name);
        } else {
            return getVariableInterner().getAttributeFromInt(name).getValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getVariableName(HtVariable variable) {
        return getVariableName(variable.getName());
    }

    /**
     * {@inheritDoc}
     */
    public FunctorName getDeinternedFunctorName(int name) {
        return getFunctorInterner().getAttributeFromInt(name).getValue();
    }

    /**
     * {@inheritDoc}
     */
    public String getFunctorName(int name) {
        return getFunctorInterner().getAttributeFromInt(name).getValue().getName();
    }

    /**
     * {@inheritDoc}
     */
    public int getFunctorArity(int name) {
        return getFunctorInterner().getAttributeFromInt(name).getValue().getArity();
    }

    /**
     * {@inheritDoc}
     */
    public FunctorName getFunctorFunctorName(IFunctor functor) {
        return getDeinternedFunctorName(functor.getName());
    }

    /**
     * {@inheritDoc}
     */
    public String getFunctorName(IFunctor functor) {
        return getFunctorName(functor.getName());
    }

    /**
     * {@inheritDoc}
     */
    public int getFunctorArity(IFunctor functor) {
        return getFunctorArity(functor.getName());
    }

    public void toString0(StringBuilder sb) {

    }
}


