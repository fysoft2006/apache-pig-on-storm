package org.apache.storm.executionengine.physicalLayer.expressionOperators;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.storm.executionengine.physicalLayer.POStatus;
import org.apache.storm.executionengine.physicalLayer.PhysicalOperator;
import org.apache.storm.executionengine.physicalLayer.Result;
import org.apache.storm.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.VisitorException;
import org.apache.pig.pen.Illustrator;

/**
 * A base class for all types of expressions. All expression operators must
 * extend this class.
 * 
 */

public abstract class ExpressionOperator extends PhysicalOperator {
  private static final Log log = LogFactory.getLog(ExpressionOperator.class);
  private static final long serialVersionUID = 1L;

  public ExpressionOperator(OperatorKey k) {
    this(k, -1);
  }

  public ExpressionOperator(OperatorKey k, int rp) {
    super(k, rp);
  }

  @Override
  public void setIllustrator(Illustrator illustrator) {
    this.illustrator = illustrator;
  }

  @Override
  public boolean supportsMultipleOutputs() {
    return false;
  }

  @Override
  public Result getNextDataBag() throws ExecException {
    return new Result();
  }

  @Override
  public abstract void visit(PhyPlanVisitor v) throws VisitorException;

  /**
   * Make a deep copy of this operator. This is declared here to make it
   * possible to call clone on ExpressionOperators.
   * 
   * @throws CloneNotSupportedException
   */
  @Override
  public ExpressionOperator clone() throws CloneNotSupportedException {
    String s = "This expression operator does not implement clone.";
    log.error(s);
    throw new CloneNotSupportedException(s);
  }

  /**
   * Get the sub-expressions of this expression. This is called if reducer is
   * run as accumulative mode, all the child expression must be called if they
   * have any UDF to drive the UDF.accumulate()
   */
  protected abstract List<ExpressionOperator> getChildExpressions();

  /**
   * check whether this expression contains any UDF this is called if reducer is
   * run as accumulative mode in this case, all UDFs must be called
   */
  public boolean containUDF() {
    if (this instanceof POUserFunc) {
      return true;
    }

    List<ExpressionOperator> l = getChildExpressions();
    if (l != null) {
      for (ExpressionOperator e : l) {
        if (e.containUDF()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Drive all the UDFs in accumulative mode
   */
  protected Result accumChild(List<ExpressionOperator> child, byte dataType)
      throws ExecException {
    try {
      if (isAccumStarted()) {
        if (child == null) {
          child = getChildExpressions();
        }
        Result res = null;
        if (child != null) {
          for (ExpressionOperator e : child) {
            if (e.containUDF()) {
              res = e.getNext(dataType);
              if (res.returnStatus != POStatus.STATUS_BATCH_OK) {
                return res;
              }
            }
          }
        }

        res = new Result();
        res.returnStatus = POStatus.STATUS_BATCH_OK;

        return res;
      }

      return null;
    } catch (RuntimeException e) {
      throw new ExecException("Exception while executing " + this.toString()
          + ": " + e.toString(), e);
    }
  }

  @Override
  public String toString() {
    return "[" + this.getClass().getSimpleName() + " " + super.toString()
        + " children: " + getChildExpressions() + " at "
        + getOriginalLocations() + "]";
  }
}
