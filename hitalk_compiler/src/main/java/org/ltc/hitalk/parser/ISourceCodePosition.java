package org.ltc.hitalk.parser;

public interface ISourceCodePosition {

    void setBeginLine(int beginLine);

    void setBeginColumn(int beginColumn);

    void setEndLine(int endLine);

    void setEndColumn(int endColumn);

    int getBeginLine();

    int getBeginColumn();

    int getEndLine();

    int getEndColumn();

}
