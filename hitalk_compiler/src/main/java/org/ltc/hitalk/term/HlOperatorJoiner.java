package org.ltc.hitalk.term;

import org.ltc.hitalk.parser.ParserException;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.fx;
import static org.ltc.hitalk.term.IdentifiedTerm.isCorrectOrder;

/**
 * 演算子優先順位解析を行います。
 *
 * @author shun
 */
@Deprecated
public abstract class HlOperatorJoiner<T extends ITerm> {

    private final Deque<IdentifiedTerm> operators = new ArrayDeque<>();
    private final Deque<T> operands = new ArrayDeque<>();

    // 最後に追加された演算子のタイプ
    private Associativity associativity = fx;

    /**
     * 指定された演算子を受け付けるかどうかを調べます。
     */
    public boolean accept(Associativity associativity) {
        return isCorrectOrder(this.associativity, associativity);
    }

    /**
     * 式の構成要素となる次の演算子を追加します。
     */
    public void push(IdentifiedTerm operator) throws ParserException {
        resolve(operator.lprio);
        operators.push(operator);
        associativity = operator.getAssociativity();
    }

    /**
     * 式の構成要素となる次の値(オペランド)を追加します。
     */
    public void push ( T operand ) {
        operands.push(operand);
        associativity = Associativity.x;
    }

    /**
     * 式の要素の追加を終了し、設定された式の構成要素から構文木を生成します。
     */
    public T complete() throws ParserException {
        resolve(Integer.MAX_VALUE);
        if (operands.size() != 1) {
            throw new IllegalStateException("operands.size() != 1");
        }
        return operands.getLast();
    }

    void resolve(int priority) throws ParserException {
        while (!operators.isEmpty() && operators.peek().rprio <= priority) {
            if (operators.peek().rprio == priority) {
                throw new ParserException("演算子の優先順位が衝突しました: " + operators.peek());
            }
            ArrayList<T> args = new ArrayList<>(2);
            for (int i = operators.peek().getAssociativity().arity; i > 0; --i) {
                args.add(0, operands.pop());
            }
            operands.push(join(operators.pop().getName(), args));
        }
    }

    /**
     * 構文木のノードを生成します。
     */
    protected abstract T join ( int notation, List <T> args );
}
