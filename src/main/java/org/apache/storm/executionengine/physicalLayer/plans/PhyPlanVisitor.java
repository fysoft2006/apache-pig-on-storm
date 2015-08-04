package org.apache.storm.executionengine.physicalLayer.plans;

import java.util.List;

import org.apache.storm.executionengine.physicalLayer.PhysicalOperator;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.Add;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.ConstantExpression;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.Divide;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.EqualToExpr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.GTOrEqualToExpr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.GreaterThanExpr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.LTOrEqualToExpr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.LessThanExpr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.Mod;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.Multiply;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.NotEqualToExpr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POAnd;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POBinCond;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POCast;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POIsNull;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POMapLookUp;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.PONegative;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.PONot;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POOr;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POProject;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.PORegexp;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POUserComparisonFunc;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.POUserFunc;
import org.apache.storm.executionengine.physicalLayer.expressionOperators.Subtract;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POBind;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POCollectedGroup;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POCombinerPackage;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POCounter;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POCross;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.PODemux;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.PODistinct;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.PODump;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POFRJoin;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POFilter;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POForEach;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POGlobalRearrange;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POGroup;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POJoinPackage;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POLimit;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POLoad;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POLocalRearrange;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POMergeCogroup;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POMergeJoin;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POMultiQueryPackage;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.PONative;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POOptimizedForEach;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POPackage;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POPartialAgg;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POPartition;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POPartitionRearrange;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POPreCombinerLocalRearrange;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.PORank;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POSkewedJoin;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POSort;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POSplit;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POStore;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POStream;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POTap;
import org.apache.storm.executionengine.physicalLayer.relationalOperators.POUnion;
import org.apache.pig.impl.plan.PlanVisitor;
import org.apache.pig.impl.plan.PlanWalker;
import org.apache.pig.impl.plan.VisitorException;

/**
 * The visitor class for the Physical Plan. To use this, create the visitor with
 * the plan to be visited. Call the visit() method to traverse the plan in a
 * depth first fashion.
 * 
 * This class also visits the nested plans inside the operators. One has to
 * extend this class to modify the nature of each visit and to maintain any
 * relevant state information between the visits to two different operators.
 * 
 */
public class PhyPlanVisitor extends PlanVisitor<PhysicalOperator, PhysicalPlan> {

  public PhyPlanVisitor(PhysicalPlan plan,
      PlanWalker<PhysicalOperator, PhysicalPlan> walker) {
    super(plan, walker);
  }

  public void visitGroup(POGroup group) throws VisitorException {
    List<List<PhysicalPlan>> allPlan = group.getAllPlans();
    if (allPlan != null && allPlan.size() > 0) {
      for (List<PhysicalPlan> plans : allPlan) {
        if (plans != null && plans.size() > 0) {
          for (PhysicalPlan plan : plans) {
            pushWalker(mCurrentWalker.spawnChildWalker(plan));
            visit();
            popWalker();
          }
        }
      }
    }
  }

  public void visitLoad(POLoad ld) throws VisitorException {
    // do nothing
  }

  public void visitTap(POTap tap) throws VisitorException {
    // do nothing
  }

  public void visitStore(POStore st) throws VisitorException {
    // do nothing
  }

  public void visitDump(PODump dp) throws VisitorException {
    // do nothing
  }

  public void visitNative(PONative nat) throws VisitorException {
    // do nothing
  }

  public void visitPartition(POPartition pt) throws VisitorException {
    // pushWalker(mCurrentWalker.spawnChildWalker(pt.getPlan()));
    // visit();
    // popWalker();

    List<PhysicalPlan> inpPlans = pt.getPlans();
    for (PhysicalPlan plan : inpPlans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitFilter(POFilter fl) throws VisitorException {
    pushWalker(mCurrentWalker.spawnChildWalker(fl.getPlan()));
    visit();
    popWalker();
  }

  public void visitCollectedGroup(POCollectedGroup mg) throws VisitorException {
    List<PhysicalPlan> inpPlans = mg.getPlans();
    for (PhysicalPlan plan : inpPlans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitLocalRearrange(POLocalRearrange lr) throws VisitorException {
    List<PhysicalPlan> inpPlans = lr.getPlans();
    for (PhysicalPlan plan : inpPlans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitGlobalRearrange(POGlobalRearrange gr)
      throws VisitorException {
    // do nothing
  }

  public void visitPackage(POPackage pkg) throws VisitorException {
    // do nothing
  }

  public void visitCombinerPackage(POCombinerPackage pkg)
      throws VisitorException {
    // do nothing
  }

  public void visitMultiQueryPackage(POMultiQueryPackage pkg)
      throws VisitorException {
    // do nothing
  }

  public void visitPOForEach(POForEach nfe) throws VisitorException {
    List<PhysicalPlan> inpPlans = nfe.getInputPlans();
    for (PhysicalPlan plan : inpPlans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitUnion(POUnion un) throws VisitorException {
    // do nothing
  }

  public void visitBind(POBind bi) throws VisitorException {
    // do nothing
  }

  public void visitSplit(POSplit spl) throws VisitorException {
    List<PhysicalPlan> plans = spl.getPlans();
    for (PhysicalPlan plan : plans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitDemux(PODemux demux) throws VisitorException {
    List<PhysicalPlan> plans = demux.getPlans();
    for (PhysicalPlan plan : plans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitCounter(POCounter poCounter) throws VisitorException {
    // do nothing
  }

  public void visitRank(PORank rank) throws VisitorException {
    // do nothing
  }

  public void visitDistinct(PODistinct distinct) throws VisitorException {
    // do nothing
  }

  public void visitSort(POSort sort) throws VisitorException {
    List<PhysicalPlan> inpPlans = sort.getSortPlans();
    for (PhysicalPlan plan : inpPlans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  public void visitConstant(ConstantExpression cnst) throws VisitorException {
    // do nothing
  }

  public void visitProject(POProject proj) throws VisitorException {
    // do nothing
  }

  public void visitGreaterThan(GreaterThanExpr grt) throws VisitorException {
    // do nothing
  }

  public void visitLessThan(LessThanExpr lt) throws VisitorException {
    // do nothing
  }

  public void visitGTOrEqual(GTOrEqualToExpr gte) throws VisitorException {
    // do nothing
  }

  public void visitLTOrEqual(LTOrEqualToExpr lte) throws VisitorException {
    // do nothing
  }

  public void visitEqualTo(EqualToExpr eq) throws VisitorException {
    // do nothing
  }

  public void visitNotEqualTo(NotEqualToExpr eq) throws VisitorException {
    // do nothing
  }

  public void visitRegexp(PORegexp re) throws VisitorException {
    // do nothing
  }

  public void visitIsNull(POIsNull isNull) throws VisitorException {
  }

  public void visitAdd(Add add) throws VisitorException {
    // do nothing
  }

  public void visitSubtract(Subtract sub) throws VisitorException {
    // do nothing
  }

  public void visitMultiply(Multiply mul) throws VisitorException {
    // do nothing
  }

  public void visitDivide(Divide dv) throws VisitorException {
    // do nothing
  }

  public void visitMod(Mod mod) throws VisitorException {
    // do nothing
  }

  public void visitAnd(POAnd and) throws VisitorException {
    // do nothing
  }

  public void visitOr(POOr or) throws VisitorException {
    // do nothing
  }

  public void visitNot(PONot not) throws VisitorException {
    // do nothing
  }

  public void visitBinCond(POBinCond binCond) {
    // do nothing

  }

  public void visitNegative(PONegative negative) {
    // do nothing

  }

  public void visitUserFunc(POUserFunc userFunc) throws VisitorException {
    // do nothing
  }

  public void visitComparisonFunc(POUserComparisonFunc compFunc)
      throws VisitorException {
    // do nothing
  }

  public void visitMapLookUp(POMapLookUp mapLookUp) {
    // TODO Auto-generated method stub

  }

  public void visitJoinPackage(POJoinPackage joinPackage)
      throws VisitorException {
    // do nothing
  }

  public void visitCast(POCast cast) {
    // TODO Auto-generated method stub

  }

  public void visitLimit(POLimit lim) throws VisitorException {
    // do nothing
  }

  public void visitCross(POCross cross) throws VisitorException {
    // do nothing
  }

  public void visitFRJoin(POFRJoin join) throws VisitorException {
    // do nothing
  }

  public void visitMergeJoin(POMergeJoin join) throws VisitorException {
    // do nothing
  }

  public void visitMergeCoGroup(POMergeCogroup mergeCoGrp)
      throws VisitorException {

  }

  /**
   * @param stream
   * @throws VisitorException
   */
  public void visitStream(POStream stream) throws VisitorException {
    // TODO Auto-generated method stub

  }

  public void visitSkewedJoin(POSkewedJoin sk) throws VisitorException {

  }

  public void visitPartitionRearrange(POPartitionRearrange pr)
      throws VisitorException {
    List<PhysicalPlan> inpPlans = pr.getPlans();
    for (PhysicalPlan plan : inpPlans) {
      pushWalker(mCurrentWalker.spawnChildWalker(plan));
      visit();
      popWalker();
    }
  }

  /**
   * @param optimizedForEach
   */
  public void visitPOOptimizedForEach(POOptimizedForEach optimizedForEach)
      throws VisitorException {
    // TODO Auto-generated method stub

  }

  /**
   * @param preCombinerLocalRearrange
   */
  public void visitPreCombinerLocalRearrange(
      POPreCombinerLocalRearrange preCombinerLocalRearrange) {
    // TODO Auto-generated method stub
  }

  public void visitPartialAgg(POPartialAgg poPartialAgg) {
  }

}
