package fr.lip6.move.robots;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Statistics {
	private Map<Tool,Long> time = new HashMap<>();
	
	public void reportTime(Tool tool, long ms) {
		time.compute(tool, (t,k) -> k==null ? ms : k+ms);
	}

	public void print() {
		for (Entry<Tool, Long> ent : time.entrySet()) {
			System.out.println("Tool "+ent.getKey()+" total "+ ent.getValue() +" ms.");
		}
	}
}
