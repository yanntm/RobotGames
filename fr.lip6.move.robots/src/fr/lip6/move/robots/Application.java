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
import java.util.Map.Entry;
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
		int nbRobot=4;
		int nbPos=5;
		
		for (int i=0; i < args.length ; i++) {
			if (NB_ROBOT.equals(args[i])) {
				nbRobot = Integer.parseInt(args[++i]);
			} else if (NB_POS.equals(args[i])) {
				nbPos = Integer.parseInt(args[++i]);
			} 
		}
		boolean rigidFilter = false;
		boolean enlarge = false;
		boolean useSymmetries = true;
		
		System.out.println("Running strategy search for K="+nbRobot +" on a ring of N="+nbPos +" positions.");
		
		Statistics stats = new Statistics();
		
		List<int[]>[] observations = ObservationGenerator.generateSplitObservations(nbPos, nbRobot);
		printSizes(observations);
		// use gperf to index our observations
		
		Map<String, Integer> obsMap = new HashMap<>();
		Map<Integer, String> indexMap = new HashMap<>();
	    {
	        int index = 0;
	        for (List<int[]> elem : observations) {
	            for (int [] obs : elem) {
	            	String key = Arrays.toString(obs);
	            	indexMap.put(index, key);
	                obsMap.put(key, index++);
	            }
	        }
	    }
	    
		Map<String, Action> rigid = new HashMap<>();
		List<int[]>[] redObservations = null;
		if (rigidFilter) {
			redObservations = ObservationGenerator.filterRigidObservations(observations, rigid );
			System.out.println("After filtering, fixed strategy for "+rigid.size()+" observations.");
			printSizes(redObservations);
			System.out.println("Starting from following 'single multiplicity' strategy'");
			for (Entry<String, Action> ent : rigid.entrySet()) {
				System.out.println(ent.getKey() +"->" + ent.getValue());
			}
		}
		
		
		int nbIter=0;
		
		try {
			File workFolder = Files.createTempDirectory("gperf").toFile();
			if (DEBUG < 2) workFolder.deleteOnExit();
			System.out.println("Building all files in "+ workFolder.getCanonicalPath());
			
			GperfRunner.runGperf(observations, workFolder.getCanonicalPath());
			
			
			Robots2PinsTransformer r2t = new Robots2PinsTransformer();
			r2t.transform(workFolder.getCanonicalPath(), false, false, nbPos, nbRobot, null, useSymmetries);

			// setup SMT solver
			SMTSolver solver = new SMTSolver(stats, enlarge);		
			if (rigidFilter) {
				solver.declareVariables(redObservations, obsMap,observations[0].size());
			} else {
				solver.declareVariables(observations, obsMap,observations[0].size());
			}
			solver.setRigidStrategy(rigid, obsMap);

			

			
			try {
			
			int[] strategy = solver.readStrategy();

			LTSminRunner runner = new LTSminRunner(false, 100, workFolder, stats);
			runner.initialize();
			
			stats.reportTime(Tool.setup, System.currentTimeMillis()-time);
			
			while (true) {
				System.out.println("Running iteration "+(nbIter++)+" with a new strategy. Remains "+rigid.size() + " pre asigned elements in strategy.");
				String res = runner.solve(strategy);
				if ("TRUE".equals(res)) {
					System.out.println("Found a winning strategy after "+nbIter+ " iterations in "+(System.currentTimeMillis() - time)+" ms.");
					printStrategy(observations,strategy);		
					break;
				} else {
					
					Set<Integer> used = TraceAnalyzer.extractTrace(workFolder, nbPos, obsMap, rigid, stats, enlarge);					
					if (enlarge && rigidFilter && !rigid.isEmpty()) {
						for (Integer us : used) {
							rigid.remove(indexMap.get(us));
						}
					}
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

		
		stats.print();
		
		return IApplication.EXIT_OK;
	}

	public void printSizes(List<int[]>[] observations) {
		int symSize = observations[0].size();
		int asymSize = observations[1].size();
		BigInteger result = BigInteger.valueOf(2).pow(symSize)
				.multiply(BigInteger.valueOf(3).pow(asymSize));

		
		System.out.println("Total number of observation : symmetric :" + symSize + " asymmetric :"
		        + asymSize + " total :" + (symSize+asymSize));
	

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
