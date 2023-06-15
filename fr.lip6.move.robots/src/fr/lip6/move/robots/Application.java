package fr.lip6.move.robots;

import java.util.Arrays;
import java.util.List;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;


public class Application implements IApplication {
	private static final String APPARGS = "application.args";
	private static final String NB_ROBOT = "-K";
	private static final String NB_POS = "-N";
	
	public Object start(IApplicationContext context) throws Exception {
		
		String [] args = (String[]) context.getArguments().get(APPARGS);

		int timeout = 3600;
		
		int nbRobot=3;
		int nbPos=4;
		
		for (int i=0; i < args.length ; i++) {
			if (NB_ROBOT.equals(args[i])) {
				nbRobot = Integer.parseInt(args[++i]);
			} else if (NB_POS.equals(args[i])) {
				nbPos = Integer.parseInt(args[++i]);
			} 
		}
		
		List<int[]>[] observations = ObservationGenerator.generateSplitObservations(nbPos, nbRobot);
			
		// setup SMT solver
		SMTSolver solver = new SMTSolver();		
		solver.declareVariables(observations);
		
		// setup LTSMin
		LTSminRunner ltsmin = new LTSminRunner(observations, false, timeout);
		
		
		
		int[] strategy = solver.readStrategy();
		
		printStrategy(observations,strategy);		
		
		
		solver.quit();
		return IApplication.EXIT_OK;
	}
	
	private void printStrategy(List<int[]>[] observations, int[] strategy) {
		int nbSym = observations[0].size();
		for (int i=0; i < strategy.length; i++) {
			if (i<nbSym) {
				System.out.println(Arrays.toString(observations[0].get(i))+"->" + Action.values()[strategy[i]]);
			} else {
				System.out.println(Arrays.toString(observations[1].get(i - nbSym))+"->" + Action.values()[strategy[i]]);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
	}
}
