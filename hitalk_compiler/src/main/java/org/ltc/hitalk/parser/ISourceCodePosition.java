package org.ltc.hitalk.parser;

public interface ISourceCodePosition {

    public void setBeginLine ( int beginLine );

    public void setBeginColumn ( int beginColumn );

    public void setEndLine ( int endLine );

    public void setEndColumn ( int endColumn );

    public int getBeginLine ();

    public int getBeginColumn ();

    public int getEndLine ();

    public int getEndColumn ();

}
