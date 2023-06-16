package fr.lip6.move.robots;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeoutException;

import fr.lip6.move.gal.process.CommandLine;
import fr.lip6.move.gal.process.Runner;

public class GperfRunner {

	public static String runGperf(List<int[]>[] observations, String outputDir) throws IOException, InterruptedException {
	    long timeout = 100;
		CommandLine gperf = new CommandLine();
	    gperf.addArg("gperf");
	    gperf.addArg("-C");  // This tells gperf to generate a C file
	    gperf.addArg("-t");  // This tells gperf to include the type information in the output
	    gperf.addArg("-L ANSI-C"); // Generate ANSI-C code.
	    gperf.addArg("-N in_word_set"); // Name of the generated function.
	    gperf.addArg("-H hash_function"); // Name of the generated hash function.
	    gperf.addArg("--output-file="+outputDir+"/observations.h"); // Output file
	    
	    String inputFilename = outputDir+ "/" + "obs.txt";
	    writeObservationsToFile(observations, inputFilename);
	    
	    gperf.addArg(inputFilename);  // This is the input file containing the observations

	    try {
	        File outputff = Files.createTempFile("gperf", ".out").toFile();
	        outputff.deleteOnExit();
	        long time = System.currentTimeMillis();
	        System.out.println("Running gperf : " + gperf);
	        int status = Runner.runTool(timeout, gperf, outputff, true);
	        if (status != 0) {
	            Files.lines(outputff.toPath()).forEach(l -> System.err.println(l));
	            throw new RuntimeException("Unexpected exception when executing gperf :" + gperf + "\n" + status);                
	        }
	        System.out.println("gperf run took "+ (System.currentTimeMillis() -time) +" ms.");
	        System.out.flush();
	        return outputDir+"/observations.h";
	    } catch (TimeoutException to) {
	        System.out.println("WARNING : gperf timed out (>"+timeout+" s) on command " + gperf);
	        return null;
	    }
	}

	
	public static void writeObservationsToFile(List<int[]>[] observations, String filename) {
	    try (FileOutputStream fos = new FileOutputStream(filename);
	         BufferedOutputStream bos = new BufferedOutputStream(fos);
	         PrintWriter pw = new PrintWriter(bos)) {

	        pw.println("%{");
	        pw.println("/* C code */");
	        pw.println("%}");
	        pw.println("struct Record {");
	        pw.println("  const char *key;");
	        pw.println("  int index;");
	        pw.println("};");
	        pw.println("%%");

	        for (int i = 0; i < observations.length; i++) {
	            for (int j = 0; j < observations[i].size(); j++) {
	                int[] observation = observations[i].get(j);
	                StringBuilder sb = new StringBuilder();
	                for (int k = 0; k < observation.length; k++) {
	                    // Subtract 1 to get values in the range 0-254, then add 1 to make it a non-zero char.
	                    char c = (char) ((observation[k] & 0xFF) + 1);
	                    sb.append(c);
	                }
	                pw.println(sb.toString() + ", " + (i * observations[i].size() + j));
	            }
	        }

	        pw.println("%%");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}
