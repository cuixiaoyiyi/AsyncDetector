/* AsyncDetecotr - an Android async component misuse detection tool
 * Copyright (C) 2018 Linjie Pan
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package cn.ac.ios.asyncdetect.summary.analysis;

import java.util.List;

import cn.ac.ios.asyncdetect.summary.ActivityGetFieldMethodSummary;
import cn.ac.ios.asyncdetect.summary.topology.ActivityGetFieldTopologyOperation;
import cn.ac.ios.asyncdetect.summary.topology.TopologyOperation;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.SpecialInvokeExpr;

/**
 * 
 * @author Linjie Pan
 * @version 1.0
 */
public class ActivityGetFieldJimpleAnalysis extends CommonJimpleAnalysis{
	
	
	public boolean setGetFieldMethodSummary(ActivityGetFieldMethodSummary thisSummary,Value originalValue,List<Unit> unitList){
		Value newValue = this.getReferedValue(originalValue);
		if( newValue == null)
			return false;
		for(int i=unitList.size()-2; i >= 0; i--){
			Unit unit = unitList.get(i);
			if( unit instanceof InvokeStmt){//1.Instance of the AsyncTask
				InvokeExpr ie = ((InvokeStmt)unit).getInvokeExpr();
				if( ie instanceof SpecialInvokeExpr && ((SpecialInvokeExpr)ie).getBase() == newValue && ie.getMethod().getName().equals("<init>") ){
					thisSummary.setReturnValue(ie);
					thisSummary.setInitUnitList(unitList.subList(0, i+1));
					return true;
				}
			}
			else if(unit instanceof IdentityStmt && ((IdentityStmt)unit).getLeftOp() == newValue && ((IdentityStmt) unit).getRightOp() instanceof ParameterRef){
				// 3.Parameter of current method
				ParameterRef pr = (ParameterRef) ((IdentityStmt) unit).getRightOp();
				thisSummary.setReturnValue(pr);
				return true;
			}
			else if( unit instanceof AssignStmt && this.isValueEqual(((AssignStmt) unit).getLeftOp(),newValue)){
				Value rightOp = ((AssignStmt) unit).getRightOp();
				/**
				 * If rightOp is an invokeExpr, then we discuss the three conditions of the returnValue of the method invoked by the invokeExpr
				 */
				if( rightOp instanceof InvokeExpr){
					InvokeExpr theExpr = (InvokeExpr)rightOp;
					SootMethod theMethod = theExpr.getMethod();
					String key = ActivityGetFieldTopologyOperation.getGetFieldKey(theMethod);
					ActivityGetFieldMethodSummary subMethodSummary = (ActivityGetFieldMethodSummary) TopologyOperation.getsMethodKeyToSummary().get(key);
					if(subMethodSummary == null){
						continue;
					}
					Value returnValue = subMethodSummary.getReturnValue();
					assert(returnValue != null);
					if( subMethodSummary.getReferedFields().size() > 0 )
						thisSummary.getReferedFields().addAll(subMethodSummary.getReferedFields());
					if( returnValue instanceof ParameterRef){
						int argIndex = ((ParameterRef) returnValue).getIndex();
						return this.setGetFieldMethodSummary(thisSummary,theExpr.getArg(argIndex),unitList.subList(0, i+1));
					}
					else if( returnValue instanceof FieldRef){
						thisSummary.setReturnValue(returnValue);
						return this.setGetFieldMethodSummary(thisSummary, returnValue, unitList.subList(0, i+1));
					}
					else if( returnValue instanceof InvokeExpr){
						thisSummary.setReturnValue(returnValue);
						thisSummary.setInitUnitList(subMethodSummary.getInitUnitList());
						return true;
					}
				}
				else if(rightOp instanceof FieldRef){
					//2.Field of Activity
					thisSummary.addReferedFields(((FieldRef) rightOp).getField());
					thisSummary.setReturnValue(rightOp);
					return this.setGetFieldMethodSummary(thisSummary, rightOp, unitList.subList(0, i+1));
				}
				else if( rightOp instanceof NullConstant)//4.NullConstant
					thisSummary.setReturnValue(rightOp);
				else
					return this.setGetFieldMethodSummary(thisSummary,rightOp, unitList.subList(0, i+1));
			}
		}
		return false;
	}
	
}
