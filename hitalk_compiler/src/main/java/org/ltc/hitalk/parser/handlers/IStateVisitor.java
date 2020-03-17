package org.ltc.hitalk.parser.handlers;

/**
 *
 */
public interface IStateVisitor<T extends ParserStateHandler> {

    void visit(T state);

    void visit(ExprAn state) throws Exception;

    void visit(ExprA0 state);

    void visit(Args state);

    void visit(Brace state);

    void visit(Bracket state);

    void visit(Block state);

    void visit(Tail state);

    void visit(DottedPair state);

    void visit(SimpleSeq state);

    void visit(ListSeq state) throws Exception;

    void visit(ExprB state) throws Exception;

    void visit(ExprC state) throws Exception;
    //=======================
//
//    void build(T  state);
//
//    void execute(T  state);
//
//    void buildExprAn(ExprAn  state);
//
//    void executeExprAn(ExprAn  state);
//
//    void buildExprA0(ExprA0  state);
//
//    void executeExprA0(ExprA0  state);
//
//    void buildArgs(Args  state);
//
//    void executeArgs(Args  state);
//
//    void buildBrace(Brace  state);
//
//    void executeBrace(Brace  state);
//
//    void buildBracket(Bracket  state);
//
//    void executeBracket(Bracket  state);
//
//    void buildBlock(Block  state);
//
//    void executeBlock(Block  state);
//
//    void buildTail(Tail  state);
//
//    void executeTail(Tail  state);
//
//    void buildList(List  state);
//
//    void executeList(List  state);
//
//    void buildSimpleSeq(SimpleSeq  state);
//
//    void executeSimpleSeq(SimpleSeq  state);
//
//    void buildListSeq(ListSeq  state);
//
//    void executeListSeq(ListSeq  state);
//
//    void buildExprC(ExprC  state);
//
//    void executeExprC(ExprC  state);
//
//    void buildExprB(ExprB  state);
//
//    void executeExprB(ExprB  state);
}
