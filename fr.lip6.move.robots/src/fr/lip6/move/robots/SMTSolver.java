package fr.lip6.move.robots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.smtlib.IResponse;
import org.smtlib.ISolver;
import org.smtlib.SMT;
import org.smtlib.IExpr;
import org.smtlib.IExpr.IFactory;
import org.smtlib.IExpr.ISymbol;
import org.smtlib.command.C_assert;
import org.smtlib.ext.C_get_model;
import org.smtlib.impl.Script;
import org.smtlib.sexpr.ISexpr;
import org.smtlib.sexpr.ISexpr.ISeq;

import fr.lip6.move.gal.structural.smt.SMTUtils;

public class SMTSolver {
	private org.smtlib.SMT smt = new SMT();
	private ISolver solver = SMTUtils.initSolver(smt,"QF_LIA",7200,7200);
	private int [] fixedStrat;
	private int nbSym=0;
	private Statistics stats;
	private boolean enlarge ;

	public SMTSolver(Statistics stats, boolean enlarge) {
		this.stats = stats;
		this.enlarge = enlarge;
	}

	public void declareVariables(List<int[]>[] observations, Map<String, Integer> obsMap, int totalSym) {
		Script script = new Script();
		IFactory ef = smt.smtConfig.exprFactory;
		org.smtlib.ISort.IApplication ints = smt.smtConfig.sortFactory.createSortExpression(ef.symbol("Int"));
		this.nbSym = totalSym;
		
		
		for (int[] obs : observations[0]) {
			ISymbol si = ef.symbol("s"+ obsMap.get(Arrays.toString(obs)));
			script.add(new org.smtlib.command.C_declare_fun(
					si,
					Collections.emptyList(),
					ints								
					));
			script.add(new C_assert(ef.fcn(ef.symbol(">="), si, ef.numeral(Action.MOVE.ordinal()))));
			script.add(new C_assert(ef.fcn(ef.symbol("<="), si, ef.numeral(Action.STAY.ordinal()))));
		}
		for (int[] obs : observations[1]) {
			ISymbol si = ef.symbol("s"+ obsMap.get(Arrays.toString(obs)));
			script.add(new org.smtlib.command.C_declare_fun(
					si,
					Collections.emptyList(),
					ints								
					));
			script.add(new C_assert(ef.fcn(ef.symbol(">="), si, ef.numeral(Action.STAY.ordinal()))));
			script.add(new C_assert(ef.fcn(ef.symbol("<="), si, ef.numeral(Action.RIGHT.ordinal()))));
		}
		SMTUtils.execAndCheckResult(script, solver);
	}

	public void addConstraint(Set<Integer> trace, int [] strat) {
		long time = System.currentTimeMillis();
		IFactory ef = smt.smtConfig.exprFactory;
		Script script = new Script();
		List<IExpr> expr = new ArrayList<>();
		for (Integer obs : trace) {
			if (fixedStrat[obs] != -1 && enlarge) {
				org.smtlib.ISort.IApplication ints = smt.smtConfig.sortFactory.createSortExpression(ef.symbol("Int"));
				// introduce a variable and relax
				if (obs < nbSym) {
					ISymbol si = ef.symbol("s"+ obs);
					script.add(new org.smtlib.command.C_declare_fun(
							si,
							Collections.emptyList(),
							ints								
							));
					script.add(new C_assert(ef.fcn(ef.symbol(">="), si, ef.numeral(Action.MOVE.ordinal()))));
					script.add(new C_assert(ef.fcn(ef.symbol("<="), si, ef.numeral(Action.STAY.ordinal()))));
				} else {
					ISymbol si = ef.symbol("s"+ obs);
					script.add(new org.smtlib.command.C_declare_fun(
							si,
							Collections.emptyList(),
							ints								
							));
					script.add(new C_assert(ef.fcn(ef.symbol(">="), si, ef.numeral(Action.STAY.ordinal()))));
					script.add(new C_assert(ef.fcn(ef.symbol("<="), si, ef.numeral(Action.RIGHT.ordinal()))));
				}
				fixedStrat[obs] = -1;
				System.out.println("Relaxing strategy for "+ obs);
			}
			expr.add(ef.fcn(ef.symbol("not"),ef.fcn(ef.symbol("="), ef.symbol("s"+obs), ef.numeral(strat[obs]))));
		}
		
		script.add(new C_assert(SMTUtils.makeOr(expr)));

		SMTUtils.execAndCheckResult(script, solver);
		stats.reportTime(Tool.SMT, System.currentTimeMillis() - time);
	}

	public int[] readStrategy() throws NoStrategyExistsException {
		long time = System.currentTimeMillis();
		String response = SMTUtils.checkSat(solver);
		if ("unsat".equals(response)) {
			throw new NoStrategyExistsException();
		} else if ("sat".equals(response)) {
			int [] strat = fixedStrat.clone();
			IResponse r = new C_get_model().execute(solver);			
			if (r instanceof ISeq) {
				ISeq seq = (ISeq) r;
				for (ISexpr v : seq.sexprs()) {
					if (v instanceof ISeq) {
						ISeq vseq = (ISeq) v;
						if (vseq.sexprs().get(1).toString().startsWith("s")) {
							int tid = Integer.parseInt( vseq.sexprs().get(1).toString().substring(1) );
							int value = -1;
							try { value = (int) Float.parseFloat( vseq.sexprs().get(vseq.sexprs().size()-1).toString() ); }
							catch (NumberFormatException e) {
								System.err.println("Error parsing value in getModel :" + vseq.sexprs().get(vseq.sexprs().size()-1).toString()); 
							}
							strat[tid] = value;
						} 
					}
				}
			}
			stats.reportTime(Tool.SMT, System.currentTimeMillis() - time);
			return strat;
		} else {
			System.err.println("Failed to check sat, obtained " + response);
			return null;
		}
	}

	public void setRigidStrategy(Map<String, Action> rigid, Map<String, Integer> obsMap) {
		fixedStrat = new int[obsMap.size()];
		for (int i=0; i < fixedStrat.length ; i++) {
			fixedStrat[i] = -1;
		}
		// Iterate through each observation-action pair in the rigid map
		for (Entry<String, Action> entry : rigid.entrySet()) {
			String obsStr = entry.getKey();
			Action action = entry.getValue();

			Integer index = obsMap.get(obsStr);
			if (index != null) {
				// Add a constraint for the given observation to enforce the action in the rigid strategy
				fixedStrat[index] = action.ordinal();
			}
		}

	}


	public void quit() {
		solver.exit();
	}


}
