package org.ltc.hitalk.compiler.bktables;

public abstract
class Flag //implements TermConvertable {
{
    private String name;

    public
    Flag ( String name ) {
        this.name = name;
    }

    public
    String getName () {
        return name;
    }

    @Override
    public
    String toString () {
        return getName();
    }


//    @NotNull
//    public
//    Term asTerm() {
//        /*return createAtom(name, VariableAndFunctorInterner interner)*/;
//    }

}
