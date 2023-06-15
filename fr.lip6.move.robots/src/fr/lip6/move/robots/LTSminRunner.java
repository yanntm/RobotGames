package fr.lip6.move.robots;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeoutException;

import fr.lip6.move.gal.ltsmin.BinaryToolsPlugin;
import fr.lip6.move.gal.ltsmin.BinaryToolsPlugin.Tool;
import fr.lip6.move.gal.process.CommandLine;
import fr.lip6.move.gal.process.Runner;

public class LTSminRunner {

	private static final int DEBUG = 2;
	private boolean doPOR;
	private File workFolder;
	private long timeout;
	private List<int[]>[] obs;
	private int[] strategy;

	public LTSminRunner(List<int[]>[] observations, boolean doPOR, long timeout) {
		this.obs = observations;
		this.doPOR = doPOR;
		try {
			this.workFolder = Files.createTempDirectory("ltsmin").toFile();
			if (DEBUG < 2) workFolder.deleteOnExit();
		} catch (IOException e) {
			System.out.println("Unable to create temporary folder.");
		}
		this.timeout = timeout;
	}


	public String solve(int [] strategy) {
		System.out.println("Built C files in : \n" + new File(workFolder + "/"));

		try {
			try {
				compilePINS((int)Math.max(2, timeout/5));
				linkPINS(Math.max(1, timeout/5));
			} catch (TimeoutException to) {
				throw new RuntimeException("Compilation or link of executable timed out." + to);
			}

			return checkProperties(timeout);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("WARNING : LTS min runner thread was asked to interrupt. Dying gracefully.");
		} catch (RuntimeException e) {
			System.out.println("WARNING : LTS min runner thread failed on error :" + e);
			e.printStackTrace();
		}
		return null;
	}

	public String checkProperties(long time) 
			throws IOException, InterruptedException {

		String property = "[]<>LTLAPp0";
		return checkProperty("goal",property,time);
	}

	private String checkProperty(String pname, String pbody, long timeout) throws IOException, InterruptedException {
		CommandLine ltsmin = new CommandLine();
		ltsmin.setWorkingDir(workFolder);
		ltsmin.addArg(BinaryToolsPlugin.getProgramURI(Tool.mc).getPath().toString());
		ltsmin.addArg("./gal.so");

		ltsmin.addArg("--threads=8");
		boolean withPOR = false;
		if (doPOR) {
			ltsmin.addArg("-p");
			ltsmin.addArg("--pins-guards");
			//ltsmin.addArg("--no-V");
			withPOR = true;
		}
		ltsmin.addArg("--when");
		ltsmin.addArg("--ltl");
		ltsmin.addArg(pbody);
		// ltsmin.addArg("--strategy=renault");
		ltsmin.addArg("--buchi-type=spotba");

		// ltsmin.addArg("--ltl-semantics");
		// ltsmin.addArg("spin");
		try {
			File outputff = Files.createTempFile("ltsrun", ".out").toFile();
			outputff.deleteOnExit();
			long time = System.currentTimeMillis();
			System.out.println("Running LTSmin : " + ltsmin);
			int status = Runner.runTool(timeout, ltsmin, outputff, true);
			if (status == 137) {
				System.err.println("LTSmin failed to check property "+ pname + " due to out of memory issue (code 137).");
				return null;
			}
			if (status != 0 && status != 1) {
				Files.lines(outputff.toPath()).forEach(l -> System.err.println(l));
				throw new RuntimeException("Unexpected exception when executing ltsmin :" + ltsmin + "\n" + status);				
			}
			System.out.println("LTSmin run took "+ (System.currentTimeMillis() -time) +" ms.");
			System.out.flush();
			boolean result;

			if (Files.lines(outputff.toPath()).anyMatch(output -> output.contains("Error: tree leafs table full! Change -s/--ratio"))) {
				// this is a real issue : need to bail out, result is not correct
				System.err.println("LTSmin failed to check property "+ pname + " due to out of memory issue.");
				return null;
			}
			// accepting cycle = counter example to
			// formula
			result = ! (status == 1) ; // output.toLowerCase().contains("accepting cycle found") ;
			String ress = (result + "").toUpperCase();
			//doneProps.put(pname,"TRUE".equals(ress),(withPOR ? "PARTIAL_ORDER ":"") + "EXPLICIT LTSMIN SAT_SMT");
			System.out.flush();

			return "trace.gcf";
		} catch (TimeoutException to) {
			System.out.println("WARNING : LTSmin timed out (>"+timeout+" s) on command " + ltsmin);
			return null;
		}
	}


	private void compilePINS(int timeout) throws IOException, TimeoutException, InterruptedException {
		// compile
		long time = System.currentTimeMillis();
		CommandLine clgcc = new CommandLine();
		clgcc.setWorkingDir(workFolder);
		clgcc.addArg(BinaryToolsPlugin.getProgramURI(Tool.limit_time).getPath().toString());		
		clgcc.addArg(Long.toString(timeout));
		clgcc.addArg("gcc");
		clgcc.addArg("-c");
		clgcc.addArg("-I" + BinaryToolsPlugin.getIncludeFolderURI().getPath().toString());
		clgcc.addArg("-I.");
		clgcc.addArg("-std=c99");
		clgcc.addArg("-fPIC");
		// try no opt to limit timeout
		clgcc.addArg("-O0");
		// clgcc.addArg("-O2");
		clgcc.addArg("model.c");

		System.out.println("Running compilation step : " + clgcc);
		File outputff = Files.createTempFile("gccrun", ".out").toFile();
		outputff.deleteOnExit();
		new File(workFolder+"/model.o").deleteOnExit();
		int status = Runner.runTool(timeout, clgcc, outputff, true);
		if (status != 0) {
			Files.lines(outputff.toPath()).forEach(l -> System.err.println(l));
			throw new RuntimeException("Could not compile executable ." + clgcc);
		}
		System.out.println("Compilation finished in "+ (System.currentTimeMillis() -time) +" ms.");
		System.out.flush();
	}

	private void linkPINS(long timeLimit) throws IOException, TimeoutException, InterruptedException {
		// link
		long time = System.currentTimeMillis();
		CommandLine clgcc = new CommandLine();
		File cwd = workFolder;
		clgcc.setWorkingDir(cwd);
		clgcc.addArg("gcc");
		clgcc.addArg("-shared");
		clgcc.addArg("-o");
		clgcc.addArg("gal.so");
		clgcc.addArg("model.o");
		System.out.println("Running link step : " + clgcc);
		File outputff = Files.createTempFile("linkrun", ".out").toFile();
		outputff.deleteOnExit();
		new File(workFolder+"/gal.so").deleteOnExit();
		int status = Runner.runTool(timeout, clgcc, outputff, true);
		if (status != 0) {
			Files.lines(outputff.toPath()).forEach(l -> System.err.println(l));
			throw new RuntimeException("Could not link executable ." + clgcc);
		}
		System.out.println("Link finished in "+ (System.currentTimeMillis() -time) +" ms.");
		System.out.flush();
	}

	public void setStrategy(int [] strategy) {
		this.strategy = strategy;
	}

}
