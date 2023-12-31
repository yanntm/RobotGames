package fr.lip6.move.robots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import fr.lip6.move.gal.process.CommandLine;
import fr.lip6.move.gal.process.Runner;

public class TraceAnalyzer {

	private static final int DEBUG = 0;

	public static Set<Integer> extractTrace(File workingDir, int nbPos, Map<String, Integer> obsMap, Map<String, Action> rigid, Statistics stats, boolean enlarge) {
		long time = System.currentTimeMillis();
		CommandLine ltsmin = new CommandLine();
		ltsmin.setWorkingDir(workingDir);
		ltsmin.addArg("/home/ythierry/git/LTSmin-BinaryBuilds/ltsmin/src/ltsmin-printtrace/ltsmin-printtrace");
		ltsmin.addArg("trace.gcf");
		ltsmin.addArg("trace.csv");
		int timeout = 10;
		try {
			File outputff = Files.createTempFile("ltsrun", ".out").toFile();
			outputff.deleteOnExit();			
			if (DEBUG >= 1) {
				System.out.println("Running LTSmin : " + ltsmin);
			}
			int status = Runner.runTool(timeout, ltsmin, outputff, true);
			if (status == 137) {
				System.err.println("LTSmin failed to build CSV of trace due to out of memory issue (code 137).");
				return null;
			}
			if (status != 0 && status != 1) {
				Files.lines(outputff.toPath()).forEach(l -> System.err.println(l));
				throw new RuntimeException("Unexpected exception when executing ltsmin :" + ltsmin + "\n" + status);				
			}
			if (DEBUG >= 1) {
				System.out.println("LTSmin run took "+ (System.currentTimeMillis() -time) +" ms.");
				System.out.flush();
			}
			final Set<Integer> result = new HashSet<>();
			int traceLen = 0;
			int rigidLen = 0;
			int lookLen = 0;
			Set<Integer> usedRigid = new HashSet<>(); 
			try (BufferedReader br = new BufferedReader(new FileReader(workingDir.getCanonicalPath()+"/trace.csv"))) {
				for (String line=br.readLine() ; line != null ; line = br.readLine()) 
				{
					String[] words = line.split(",");

					String action = words[words.length -1];
					if (action.endsWith("LOOK\"")) {
						lookLen++;
						int pos = Integer.parseInt(action.split("_")[0].replace("\"tr",""));

						int [] obs = new int [nbPos];
						for (int i=0; i < nbPos; i++) {
							obs[i] = Integer.parseInt(words[ (((pos+i)*6+5) % (6*nbPos)) + 1]);
						}
						if (! ObservationGenerator.isReversedMaximal(obs)) {
							for (int i = 1, j = nbPos- 1; i < j; i++, j--) {
								int temp = obs[i];
								obs[i] = obs[j];
								obs[j] = temp;
							}
						}
						String key = Arrays.toString(obs);
						if (! rigid.containsKey(key)) {
							result.add(obsMap.get(key));
						} else {
							usedRigid.add(obsMap.get(key));
							rigidLen++;
						}
					}
					traceLen ++;
				}
					
				System.out.println("Analyzed a counter-example of " + (traceLen-1) + " steps of which " + lookLen + " uses of the strategy ("+ usedRigid.size() + " fixed), building constraint involving "+ result.size()+ " observations." );
				if (enlarge)
					result.addAll(usedRigid);
				stats.reportTime(Tool.Trace, System.currentTimeMillis() - time);
				return result;



			} catch (IOException e) {
				//error happened
				e.printStackTrace();
			}

		} catch (TimeoutException to) {
			System.out.println("WARNING : LTSmin timed out (>"+timeout+" s) on command " + ltsmin);
			return null;
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 

		return null;
	}


}
