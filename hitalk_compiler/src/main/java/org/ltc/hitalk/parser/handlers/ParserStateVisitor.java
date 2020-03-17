package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.PlToken;

import static org.ltc.hitalk.parser.ParserState.*;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;
import static org.ltc.hitalk.parser.PlToken.newToken;

/**
 * @param <T>
 */
public class ParserStateVisitor<T extends ParserStateHandler> extends BaseStateVisitor implements IStateVisitor<T> {

    protected IStateTraverser<T> traverser;

    public IContext<T> getParentContext() {
        return null;
    }

    public void visit(T state) {

    }

    // * exprAn(n) ::=
// *    n > 0
// *    exprB(n)
// *    { op(yfx,n) exprA(n-1) | op(yf,n) }*
    public void visit(ExprAn state) throws Exception {
        if (state.getCurrPriority() > 0) {
            state.create(new ExprB(
                    EXPR_B,
                    state.getAssocs(),
                    state.getDks(),
                    state.getCurrPriority(),
                    newToken(TK_BOF)));
        } else {
            state.create(new ExprA0(EXPR_A0,
                    state.getAssocs(),
                    state.getDks(),
                    0,
                    state.getToken()); ;
        }
    }

    public void visit(ExprA0 state) {

    }

    public void visit(Args state) {

    }

    public void visit(Brace state) {

    }

    public void visit(Bracket state) {

    }

    public void visit(Block state) {

    }

    public void visit(Tail state) {

    }

    public void visit(List state) {

    }

    public void visit(SimpleSeq state) {

    }

    public void visit(ListSeq state) {

    }

    //    exprB(n) ::=
// *    exprC(n-1)
// *    { op(xfx,n) exprAn(n-1) |
// *      op(xfy,n) exprAn(n) |
// *      op(xf,n)
//      }*
// * // exprC is called parseLeftSide in the code
    public void visit(ExprB state) throws Exception {
        final PlToken token = g
        state.create(new ExprC(EXPR_C, state.getAssocs(), state.getDks(), state.getCurrPriority() - 1, token
        ))
    }

    public void visit(ExprC state) {

    }
}
