package org.apache.storm.executionengine.physicalLayer.expressionOperators;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.PigException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.storm.executionengine.physicalLayer.POStatus;
import org.apache.storm.executionengine.physicalLayer.Result;
import org.apache.storm.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.data.DataType;
import org.apache.pig.impl.plan.NodeIdGenerator;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.VisitorException;

public class NotEqualToExpr extends BinaryComparisonOperator {

  /**
     *
     */
  private static final long serialVersionUID = 1L;
  transient private final Log log = LogFactory.getLog(getClass());

  public NotEqualToExpr(OperatorKey k) {
    this(k, -1);
  }

  public NotEqualToExpr(OperatorKey k, int rp) {
    super(k, rp);
    resultType = DataType.BOOLEAN;
  }

  @Override
  public String name() {
    return "Not Equal To" + "[" + DataType.findTypeName(resultType) + "]"
        + " - " + mKey.toString();
  }

  @Override
  public void visit(PhyPlanVisitor v) throws VisitorException {
    v.visitNotEqualTo(this);
  }

  @Override
  public Result getNextBoolean() throws ExecException {
    Result left, right;

    switch (operandType) {
    case DataType.BYTEARRAY:
    case DataType.DOUBLE:
    case DataType.FLOAT:
    case DataType.BOOLEAN:
    case DataType.INTEGER:
    case DataType.BIGINTEGER:
    case DataType.BIGDECIMAL:
    case DataType.LONG:
    case DataType.DATETIME:
    case DataType.CHARARRAY:
    case DataType.TUPLE:
    case DataType.MAP: {
      Result r = accumChild(null, operandType);
      if (r != null) {
        return r;
      }
      left = lhs.getNext(operandType);
      right = rhs.getNext(operandType);
      return doComparison(left, right);
    }
    default: {
      int errCode = 2067;
      String msg = this.getClass().getSimpleName() + " does not know how to "
          + "handle type: " + DataType.findTypeName(operandType);
      throw new ExecException(msg, errCode, PigException.BUG);
    }

    }
  }

  @SuppressWarnings("unchecked")
  private Result doComparison(Result left, Result right) throws ExecException {
    if (left.returnStatus != POStatus.STATUS_OK) {
      return left;
    }
    if (right.returnStatus != POStatus.STATUS_OK) {
      return right;
    }
    // if either operand is null, the result should be
    // null
    if (left.result == null || right.result == null) {
      left.result = null;
      left.returnStatus = POStatus.STATUS_NULL;
      return left;
    }

    if (left.result instanceof Comparable && right.result instanceof Comparable) {
      if (((Comparable) left.result).compareTo(right.result) != 0) {
        left.result = Boolean.TRUE;
      } else {
        left.result = Boolean.FALSE;
      }
    } else if (left.result instanceof HashMap
        && right.result instanceof HashMap) {
      HashMap leftMap = (HashMap) left.result;
      HashMap rightMap = (HashMap) right.result;
      if (leftMap.equals(rightMap)) {
        left.result = Boolean.FALSE;
      } else {
        left.result = Boolean.TRUE;
      }
    } else {
      throw new ExecException(
          "The left side and right side has the different types");
    }
    illustratorMarkup(null, left.result, (Boolean) left.result ? 0 : 1);
    return left;
  }

  @Override
  public NotEqualToExpr clone() throws CloneNotSupportedException {
    NotEqualToExpr clone = new NotEqualToExpr(new OperatorKey(mKey.scope,
        NodeIdGenerator.getGenerator().getNextNodeId(mKey.scope)));
    clone.cloneHelper(this);
    return clone;
  }
}
