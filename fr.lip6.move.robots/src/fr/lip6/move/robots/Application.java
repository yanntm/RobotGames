package fr.lip6.move.robots;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;


public class Application implements IApplication {
	private static final String APPARGS = "application.args";
	private static final String NB_ROBOT = "-K";
	private static final String NB_POS = "-N";
	private static final int DEBUG=0;
	
	public Object start(IApplicationContext context) {
		
		String [] args = (String[]) context.getArguments().get(APPARGS);

		int timeout = 3600;
		long time = System.currentTimeMillis();
		int nbRobot=6;
		int nbPos=7;
		
		for (int i=0; i < args.length ; i++) {
			if (NB_ROBOT.equals(args[i])) {
				nbRobot = Integer.parseInt(args[++i]);
			} else if (NB_POS.equals(args[i])) {
				nbPos = Integer.parseInt(args[++i]);
			} 
		}
		System.out.println("Running strategy search for K="+nbRobot +" on a ring of N="+nbPos +" positions.");
		List<int[]>[] observations = ObservationGenerator.generateSplitObservations(nbPos, nbRobot);
		printSizes(observations);
		// use gperf to index our observations
		
		Map<String, Integer> obsMap = new HashMap<>();
	    {
	        int index = 0;
	        for (List<int[]> elem : observations) {
	            for (int [] obs : elem) {
	                obsMap.put(Arrays.toString(obs), index++);
	            }
	        }
	    }
	    int totalObs = observations[0].size() + observations[1].size();
		
		Map<int[], Action> rigid = new HashMap<>();
		List<int[]>[] redObservations = ObservationGenerator.filterRigidObservations(observations, rigid );
		System.out.println("After filtering, found "+rigid.size()+" observations.");
		printSizes(redObservations);
		
		
		int nbIter=0;
		
		try {
			File workFolder = Files.createTempDirectory("gperf").toFile();
			if (DEBUG < 2) workFolder.deleteOnExit();
			System.out.println("Building all files in "+ workFolder.getCanonicalPath());
			
			GperfRunner.runGperf(observations, workFolder.getCanonicalPath());
			
			
			Robots2PinsTransformer r2t = new Robots2PinsTransformer();
			r2t.transform(workFolder.getCanonicalPath(), false, false, nbPos, nbRobot, null);

			// setup SMT solver
			SMTSolver solver = new SMTSolver();		
			solver.declareVariables(observations);
			solver.setRigidStrategy(rigid, obsMap);
			
			try {
			
			int[] strategy = solver.readStrategy();

			LTSminRunner runner = new LTSminRunner(false, 100, workFolder);
			runner.initialize();
			
			while (true) {
				System.out.println("Running iteration "+(nbIter++)+" with a new strategy.");
				String res = runner.solve(strategy);
				if ("TRUE".equals(res)) {
					System.out.println("Found a winning strategy after "+nbIter+ " iterations in "+(System.currentTimeMillis() - time)+" ms.");
					printStrategy(observations,strategy);		
					break;
				} else {
					
					Set<Integer> used = TraceAnalyzer.extractTrace(workFolder, nbPos, obsMap);
					System.out.println("Found a counter example involving "+used.size()+"/" + totalObs +" observations ");
					
					solver.addConstraint(used, strategy);
					strategy = solver.readStrategy();
				}
			}
			
			} catch (NoStrategyExistsException e) {
				System.out.println("Process concluded that no strategy exists after "+nbIter + " iterations in "+(System.currentTimeMillis() - time)+" ms." );
			}
			
			solver.quit();

		} catch (IOException e) {
			System.out.println("Unable to create temporary folder.");
			e.printStackTrace();
		}

		
		
		
		return IApplication.EXIT_OK;
	}

	public void printSizes(List<int[]>[] observations) {
		BigInteger symSize = BigInteger.valueOf(observations[0].size());
		BigInteger asymSize = BigInteger.valueOf(observations[1].size());

		
		System.out.println("Total number of observation : symmetric :" + symSize + " asymmetric :"
		        + asymSize + " total :" + symSize.add(asymSize));

		BigInteger result;
		if (observations[0].size() == 0) {
			result = symSize.pow(2);
		} else if (observations[1].size() == 0) {
			result = asymSize.pow(3);
		} else {
			result = symSize.pow(2).multiply(asymSize.pow(3));
		}

		System.out.println("Estimated size of strategy search space : " + result);
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
