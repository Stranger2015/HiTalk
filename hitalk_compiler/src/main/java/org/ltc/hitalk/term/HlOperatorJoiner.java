package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ParseException;
import org.ltc.hitalk.term.HlOperator.Associativity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.term.HlOperator.Associativity.fx;

/**
 * 演算子優先順位解析を行います。
 *
 * @author shun
 */
public abstract class HlOperatorJoiner<TERM extends Term> {

    private final ArrayDeque <HlOperator> operators = new ArrayDeque <>();
    private final ArrayDeque <TERM> operands = new ArrayDeque <>();

    // 最後に追加された演算子のタイプ
    private Associativity associativity = fx;

    /**
     * 指定された演算子を受け付けるかどうかを調べます。
     */
    public boolean accept ( Associativity associativity ) {
        return HlOperator.isCorrectOrder(this.associativity, associativity);
    }

    /**
     * 式の構成要素となる次の演算子を追加します。
     */
    public void push ( HlOperator operator ) throws ParseException {
        resolve(operator.lprio);
        operators.push(operator);
        associativity = operator.getAssociativity();
    }

    /**
     * 式の構成要素となる次の値(オペランド)を追加します。
     */
    public void push ( TERM operand ) {
        operands.push(operand);
        associativity = Associativity.x;
    }

    /**
     * 式の要素の追加を終了し、設定された式の構成要素から構文木を生成します。
     */
    public TERM complete () throws ParseException {
        resolve(Integer.MAX_VALUE);
        if (operands.size() != 1) {
            throw new IllegalStateException("operands.size() != 1");
        }
        return operands.getLast();
    }

    void resolve ( int priority ) throws ParseException {
        while (!operators.isEmpty() && operators.peek().rprio <= priority) {
            if (operators.peek().rprio == priority) {
                throw new ParseException("演算子の優先順位が衝突しました: " + operators.peek());
            }
            ArrayList <TERM> args = new ArrayList <>(2);
            for (int i = operators.peek().getAssociativity().arity; i > 0; --i) {
                args.add(0, operands.pop());
            }
            operands.push(join(operators.pop().getName(), args));
        }
    }

    /**
     * 構文木のノードを生成します。
     */
    protected abstract TERM join ( int notation, List <TERM> args );
}
