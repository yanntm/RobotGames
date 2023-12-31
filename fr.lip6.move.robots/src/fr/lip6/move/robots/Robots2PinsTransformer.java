package fr.lip6.move.robots;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.logging.Logger;

import android.util.SparseIntArray;
import fr.lip6.move.gal.mcc.properties.PropertyPrinter;
import fr.lip6.move.gal.structural.Property;
import fr.lip6.move.gal.structural.SparsePetriNet;
import fr.lip6.move.gal.structural.expr.AtomicProp;
import fr.lip6.move.gal.structural.expr.AtomicPropManager;
import fr.lip6.move.gal.structural.expr.CExpressionPrinter;
import fr.lip6.move.gal.structural.expr.Expression;
import fr.lip6.move.gal.structural.expr.Op;
import fr.lip6.move.gal.util.IntMatrixCol;

public class Robots2PinsTransformer {

	private static final int DEBUG = 2;
	private int nbRobots;
	private int nbPos;
	private boolean hasPartialOrder=false;
	private boolean useSymmetries;
	
	
	private void buildBodyFile(String path) throws IOException {
		File fpath = new File(path);
		if (DEBUG==0) fpath.deleteOnExit();
		PrintWriter pw = new PrintWriter(path);

		if (!forSpot) {
			pw.println("#include <ltsmin/pins.h>");
			pw.println("#include <ltsmin/pins-util.h>");
			pw.println("#include <ltsmin/ltsmin-standard.h>");
			pw.println("#include <ltsmin/lts-type.h>");
			pw.println("#include \"model.h\"");
			pw.println("#define true 1");
			pw.println("#define false 0\n"
			+ "#define nbPos "+nbPos);
			pw.println("int initial [" + (6*nbPos + 5*nbPos) + "] ;");

			pw.println("int* get_initial_state() {");
			int rob = 0;
			if (nbRobots <= nbPos) {
				for (int i = 0, ie = 6*nbPos; i < ie; i++) {
					pw.println("  // " + (i/6) + ":" + Action.values()[i%6] );
					if ( (i%6==4 || i%6==5) && (rob < nbRobots) ) {
						pw.println("  initial [" + (i) + "] = 1;");
						if (i%6 == 5) {
							rob++;
						}
					} else {
						pw.println("  initial [" + (i) + "] = " + 0 + ";");					
					}
				}
			} else {
				for (int i = 0, ie = 6*nbPos; i < ie; i++) {
					pw.println("  // " + (i/6) + ":" + Action.values()[i%6] );
					if (i==4 || i==5) {
						pw.println("  initial [" + (i) + "] = "+(nbRobots - nbPos + 1) +";");
						if (i == 5) {
							rob += (nbRobots - nbPos);
						}
						
					} else if ( (i%6==4 || i%6==5) && (rob < nbRobots) ) {
						pw.println("  initial [" + (i) + "] = 1;");
						if (i%6 == 5) {
							rob++;
						}
					} else {
						pw.println("  initial [" + (i) + "] = " + 0 + ";");					
					}
				}
			}
			for (int i = 0, ie = 5*nbPos; i < ie; i++) {
				pw.println("  // sched " + (i/5) + ":" + Action.values()[i%5] );
				pw.println("  initial [" + (6*nbPos+i) + "] = " + 0 + ";");									
			}
			pw.println("  return initial;");
			pw.println("}");

			
		}  else {
			pw.println("#include <cstddef>");
			pw.println("#include <cstring>");

			pw.println("extern \"C\" {");
		}

		pw.println("int get_state_variable_count() {");
		pw.println("  return " + (6*nbPos+5*nbPos) + " ;");
		pw.println("}");




		if (forSpot) {
			pw.print("void get_initial_state(void* ss)\n");
			pw.print("{\n  int* initial = (int*)ss;\n");
			for (int i = 0, ie = 6*nbPos; i < ie; i++) {
				pw.println("  // " + (i/6) + ":" + Action.values()[i%6] );
				if (i==4 || i==5) {
					pw.println("  initial [" + (i) + "] = " + nbRobots + ";");
				} else {
					pw.println("  initial [" + (i) + "] = " + 0 + ";");					
				}
			}
			for (int i = 0, ie = 5*nbPos; i < ie; i++) {
				pw.println("  // sched " + (i/5) + ":" + Action.values()[i%5] );
				pw.println("  initial [" + (i) + "] = " + 0 + ";");									
			}
			pw.println("  return initial;");
			pw.println("}");
			
			pw.print("const char* varnames[" + (6*nbPos+5*nbPos) + "] = { ");
			
			for (int i=0, ie = 6*nbPos ; i < ie ; i++) {
				String vname = "pos" + (i/6) + "_" + Action.values()[i%6].toString();
				pw.print("\""+vname+"\"");
				pw.print(", ");
			}
			for (int i=0, ie = 5*nbPos ; i < ie ; i++) {
				String vname = "sched" + (i/5) + "_" + Action.values()[i%5].toString();
				pw.print("\""+vname+"\"");
				if (i < ie -1) {
					pw.print(", ");
				}
			}
			pw.println("\n};");
			//		"extern \"C\"
			pw.print("const char* get_state_variable_name(int i)\n");
			pw.println("{\n  return varnames[i];\n}\n");
			
			pw.println("struct transition_info_t;\n");
			pw.println("typedef void (*TransitionCBSPOT)(void* ctx, transition_info_t* transition_info, int* dst);");


			// extern \"C\" 
			pw.println("int get_state_variable_type(int i)\n{\n  return 0;\n}\n");
			// extern \"C\"
			pw.println("int get_state_variable_type_count()\n{\n  return 1;\n}\n");
			// "extern \"C\"
			pw.println("const char* get_state_variable_type_name(int i)\n{\n  return \"int\";\n}\n");
			// "extern \"C\"
			pw.println("int get_state_variable_type_value_count(int i)\n{\n  return 0;\n}\n");
			// "extern \"C\
			pw.println("const char* get_state_variable_type_value(int a, int b)\n{\n  return \"\";\n}\n");
		}

		printDependencyMatrix(pw);

		printNextState(pw);
		if (forSpot) {
			pw.println("int get_successors(void* model, int* in, TransitionCBSPOT cb, void* ctx)\n{");
			pw.println("  int res=0;");
			pw.println("  state_t cur;");
			pw.println("  memcpy(&cur, in, "+ (6*nbPos + 5*nbPos)+" * sizeof(int));");
			for (int tid=0,tide=(5*nbPos); tid<tide; ++tid) {
				pw.println("  if (transition"+tid+"(&cur)) {\n    cb (ctx, NULL, cur.state);");
				pw.println("    memcpy(&cur, in, "+ (6*nbPos+5*nbPos)+" * sizeof(int));");
				pw.println("    ++res;");
				pw.println("  }");
			}
			pw.println("  return res;");
			pw.println("}");

			pw.println("} // extern C" );
		} else {
			printGB(pw);
		}
		pw.close();
	}

	private void buildHeader(String path) throws IOException {
		File fpath = new File(path);
		if (DEBUG==0) fpath.deleteOnExit();
		PrintWriter pw = new PrintWriter(path);
		pw.print(
				"#include <ltsmin/pins.h>\n"+
						"/**\n"+
						" * @brief calls callback for every successor state of src in transition group \"group\".\n"+
						" */\n"+
						"int next_state(void* model, int group, int *src, TransitionCB callback, void *arg);\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the initial state.\n"+
						" */\n"+
						"int* get_initial_state();\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the read dependency matrix.\n"+
						" */\n"+
						"int* read_matrix(int row);\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the write dependency matrix.\n"+
						" */\n"+
						"int* write_matrix(int row);\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the state label dependency matrix.\n"+
						" */\n"+
						"int* label_matrix(int row);\n"+
						"\n"+
						"/**\n"+
						" * @brief returns whether the state src satisfies state label \"label\".\n"+
						" */\n"+
						"int state_label(void* model, int label, int* src);\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the number of transition groups.\n"+
						" */\n"+
						"int group_count();\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the length of the state.\n"+
						" */\n"+
						"int get_state_variable_count();\n"+
						"\n"+
						"/**\n"+
						" * @brief returns the number of state labels.\n"+
						" */\n"+
						"int label_count();\n"
				);
		pw.flush();
		pw.close();
	}

	public int[] convertToLine(BitSet bs) {
		int card = bs.cardinality();
		int[] line = new int[card + 1];
		int index = 0;
		line[index++] = card;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			// operate on index i here
			line[index++] = i;
			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}
		return line;
	}

	public BitSet convertToBitSet(int[] line) {
		BitSet b = new BitSet();
		for (int i = 0; i < line.length; i++) {
			if (line[i] == 1) {
				b.set(i);
			}
		}
		return b;
	}
	
	private int[] convertToLine(SparseIntArray bs) {
		int card = bs.size();
		int[] line = new int[card + 1];
		int index = 0;
		line[index++] = card;
		for (int i = 0, ie = bs.size(); i < ie; i++) {
			// operate on index i here
			line[index++] = bs.keyAt(i);
		}
		return line;
	}


	private void printDependencyMatrix(PrintWriter pw) {

		List<int[]> rm = new ArrayList<>();
		List<int[]> wm = new ArrayList<>();
		
		if (! useSymmetries) {
		for (int tindex = 0, te = (5*nbPos); tindex < te; tindex++) {
			int pos = tindex / 5;
			Action a = Action.values()[tindex%5];
			SparseIntArray reads = new SparseIntArray();
			SparseIntArray writes = new SparseIntArray();
			if (a == Action.LOOK) {
				// we read all "total" cells
				for (int i=0,ie=nbPos; i<ie; i++) {
					reads.put(i*6 + Action.TOTAL.ordinal(), 1);
				}
				// we read and write at our position (all cells except TOTAL)
				for (int i=0,ie=5; i<ie; i++) {
					reads.put(i+ pos*6, 1);
					writes.put(i+ pos*6, 1);
				}
				// we update sched
				for (int i=0,ie=5; i<ie; i++) {
					reads.put(i+ pos*5 + nbPos*6, 1);
					writes.put(i+ pos*5 + nbPos*6, 1);
				}
			} else if (a == Action.LEFT) {
				int left = (pos + nbPos -1) % nbPos;
				// we read and update LOOK and TOTAL of pos-1%N
				reads.put(left*6 + Action.LOOK.ordinal(), 1);
				reads.put(left*6 + Action.TOTAL.ordinal(), 1);
				writes.put(left*6 + Action.LOOK.ordinal(), 1);
				writes.put(left*6 + Action.TOTAL.ordinal(), 1);				
				// we read and update LEFT and TOTAL of pos
				reads.put(pos*6 + Action.LEFT.ordinal(), 1);
				reads.put(pos*6 + Action.TOTAL.ordinal(), 1);				
				writes.put(pos*6 + Action.LEFT.ordinal(), 1);
				writes.put(pos*6 + Action.TOTAL.ordinal(), 1);	
				// we update sched of LOOK at left
				reads.put(left*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				reads.put(pos*5 + Action.LEFT.ordinal() + nbPos*6, 1);
				writes.put(left*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				writes.put(pos*5 + Action.LEFT.ordinal() + nbPos*6, 1);

			} else if (a == Action.RIGHT) {
				int right = (pos + 1) % nbPos;
				// we read and update LOOK and TOTAL of pos-1%N
				reads.put(right*6 + Action.LOOK.ordinal(), 1);
				reads.put(right*6 + Action.TOTAL.ordinal(), 1);
				writes.put(right*6 + Action.LOOK.ordinal(), 1);
				writes.put(right*6 + Action.TOTAL.ordinal(), 1);
				// we read and update RIGHT and TOTAL of pos
				reads.put(pos*6 + Action.RIGHT.ordinal(), 1);
				reads.put(pos*6 + Action.TOTAL.ordinal(), 1);				
				writes.put(pos*6 + Action.RIGHT.ordinal(), 1);
				writes.put(pos*6 + Action.TOTAL.ordinal(), 1);				
				// we update sched of r.LOOK and cur.RIGHT
				reads.put(right*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				reads.put(pos*5 + Action.RIGHT.ordinal() + nbPos*6, 1);
				writes.put(right*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				writes.put(pos*5 + Action.RIGHT.ordinal() + nbPos*6, 1);

			} else if (a == Action.STAY) {
				// we read and update LOOK and STAY of pos
				reads.put(pos*6 + Action.LOOK.ordinal(), 1);
				reads.put(pos*6 + Action.STAY.ordinal(), 1);				
				writes.put(pos*6 + Action.LOOK.ordinal(), 1);
				writes.put(pos*6 + Action.STAY.ordinal(), 1);
				// we update sched 
				reads.put(pos*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				reads.put(pos*5 + Action.STAY.ordinal() + nbPos*6, 1);
				writes.put(pos*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				writes.put(pos*5 + Action.STAY.ordinal() + nbPos*6, 1);

				
			} else if (a == Action.MOVE) {
				// we read and update MOVE and TOTAL of pos
				reads.put(pos*6 + Action.MOVE.ordinal(), 1);
				reads.put(pos*6 + Action.TOTAL.ordinal(), 1);				
				writes.put(pos*6 + Action.MOVE.ordinal(), 1);
				writes.put(pos*6 + Action.TOTAL.ordinal(), 1);				

				int left = (pos + nbPos -1) % nbPos;
				// we read and update LOOK and TOTAL of pos-1%N
				reads.put(left*6 + Action.LOOK.ordinal(), 1);
				reads.put(left*6 + Action.TOTAL.ordinal(), 1);
				writes.put(left*6 + Action.LOOK.ordinal(), 1);
				writes.put(left*6 + Action.TOTAL.ordinal(), 1);
								
				int right = (pos + 1) % nbPos;
				// OR we read and update LOOK and TOTAL of pos+1%N
				reads.put(right*6 + Action.LOOK.ordinal(), 1);
				reads.put(right*6 + Action.TOTAL.ordinal(), 1);
				writes.put(right*6 + Action.LOOK.ordinal(), 1);
				writes.put(right*6 + Action.TOTAL.ordinal(), 1);
				
				// updates to sched of src and destination(s)
				reads.put(pos*5 + Action.MOVE.ordinal() + nbPos*6, 1);
				writes.put(pos*5 + Action.MOVE.ordinal() + nbPos*6, 1);
				reads.put(left*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				writes.put(left*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				reads.put(right*5 + Action.LOOK.ordinal() + nbPos*6, 1);
				writes.put(right*5 + Action.LOOK.ordinal() + nbPos*6, 1);								
				
			}
			
			int[] r = convertToLine(reads);
			rm.add(r);

			int[] w = convertToLine(writes);
			wm.add(w);
		}
		{
			// add a reschedule transition : update sched
			SparseIntArray reads = new SparseIntArray();
			SparseIntArray writes = new SparseIntArray();
			for (int i = 0; i < 5 * nbPos; i++) {
				reads.put(6 * nbPos + i, 1);
				writes.put(6 * nbPos + i, 1);
			}
			int[] r = convertToLine(reads);
			rm.add(r);

			int[] w = convertToLine(writes);
			wm.add(w);

		}
		} else {
			// with symmetries : everyone depends on everything
			SparseIntArray reads = new SparseIntArray();
			for (int var=0; var < 11*nbPos ; var++) {
				reads.append(var, 1);
			}
			int [] r = convertToLine(reads);
			for (int tindex = 0, te = (5*nbPos); tindex < te; tindex++) {
				rm.add(r);
				wm.add(r);
			}
		}

		printMatrix(pw, "rm", rm);
		pw.print("int* read_matrix(int row) {\n" + "  return rm[row];\n" + "}\n");

		printMatrix(pw, "wm", wm);
		pw.print("int* write_matrix(int row) {\n" + "  return wm[row];\n" + "}\n");
		
		printLabels(pw, rm, wm);
	}


	private void printGB(PrintWriter pw) {
		// set the name of this PINS plugin
		pw.println("char pins_plugin_name[] = \"GAL\";");

		pw.println("void pins_model_init(model_t m) {");

		pw.println("  // create the LTS type LTSmin will generate");
		pw.println("  lts_type_t ltstype=lts_type_create();");

		pw.println("  // set the length of the state");
		pw.println("  lts_type_set_state_length(ltstype, get_state_variable_count());");

		pw.println("  // add an int type for a state slot");
		pw.println("  int int_type = lts_type_add_type(ltstype, \"int\", NULL);");
		pw.println("  lts_type_set_format (ltstype, int_type, LTStypeDirect);");

		pw.println("  // set state name & type");
		for (int i=0, ie=nbPos*6; i<ie; i++) {
			String vname = "pos" + (i/6) + "_" + Action.values()[i%6].toString();
			pw.println("  lts_type_set_state_name(ltstype," + i + ",\"" + vname + "\");");
			pw.println("  lts_type_set_state_typeno(ltstype," + i + ",int_type);");
		}
		for (int i=0, ie=nbPos*5; i<ie; i++) {
			String vname = "sched" + (i/5) + "_" + Action.values()[i%5].toString();
			pw.println("  lts_type_set_state_name(ltstype," + (i+6*nbPos) + ",\"" + vname + "\");");
			pw.println("  lts_type_set_state_typeno(ltstype," + (i+6*nbPos) + ",int_type);");
		}

		// edge label types : TODO
		pw.println("  // add an action type for edge labels");
		pw.println("  int action_type = lts_type_add_type(ltstype, \"action\", NULL);");
		pw.println("  lts_type_set_format (ltstype, action_type, LTStypeEnum);");

		pw.println("  lts_type_set_edge_label_count (ltstype, 1);");
		pw.println("  lts_type_set_edge_label_name(ltstype, 0, \"action\");");
		pw.println("  lts_type_set_edge_label_type(ltstype, 0, \"action\");");
		pw.println("  lts_type_set_edge_label_typeno(ltstype, 0, action_type);");

		// state label types : TODO

		pw.println("  // add a bool type for state labels");
		pw.println("  int bool_type = lts_type_put_type (ltstype, \"boolean\", LTStypeBool, NULL);");

		pw.println("  lts_type_set_state_label_count (ltstype, "+labelCount()+");");
		
		{			
			for (int ii=0, ie=labelCount() ; ii < ie ; ii++) {
				pw.println("    lts_type_set_state_label_typeno (ltstype, "+ii+", bool_type);");
			}
			
			int labindex = 0;			
//			for (labindex=0 ; labindex < net.getTransitionCount() ; labindex++) {
//				pw.println("    lts_type_set_state_label_name (ltstype, "+labindex+ ", \"enabled" + labindex + "\");");				
//			}
			for (AtomicProp atom : listAtoms) {
				pw.println("    lts_type_set_state_label_name (ltstype, "+labindex+ ", \"LTLAP" + atom.getName() + "\");");
				labindex++;
			}
			for (AtomicProp atom : invAtoms) {
				pw.println("    lts_type_set_state_label_name (ltstype, "+labindex+ ", \"" + atom.getName() + "\");");
				labindex++;
			}		
		}
		
		
		// done with ltstype
		pw.println("  lts_type_validate(ltstype);");

		// make sure to set the lts-type before anything else in the GB
		pw.println("  GBsetLTStype(m, ltstype);");

		// setting all values for all non direct types
		for (int tindex = 0; tindex < (5*nbPos); tindex++) {
			pw.println("  pins_chunk_put(m, action_type, chunk_str(\"tr" + (tindex/5) +"_" + Action.values()[tindex%5] + "\"));");
		}
		if (! useSymmetries) {
			// sched
			pw.println("  pins_chunk_put(m, action_type, chunk_str(\"reschedule\"));");
		}
		

		// pw.println(" GBchunkPut(m, bool_type, chunk_str(LTSMIN_VALUE_BOOL_FALSE));");
		// pw.println(" GBchunkPut(m, bool_type, chunk_str(LTSMIN_VALUE_BOOL_TRUE));");

		// set state variable values for initial state
		pw.println("  GBsetInitialState(m, get_initial_state());");

		// set function pointer for the next-state function
		pw.println("  GBsetNextStateLong(m, (next_method_grey_t) next_state);");

		// set function pointer for the label evaluation function
		pw.println("  GBsetStateLabelLong(m, (get_label_method_t) state_label);");

		// create combined matrix
		pw.println("  matrix_t *cm = malloc(sizeof(matrix_t));");
		pw.println("  dm_create(cm, group_count(), get_state_variable_count());");

		// set the read dependency matrix
		pw.println("  matrix_t *rm = malloc(sizeof(matrix_t));");
		pw.println("  dm_create(rm, group_count(), get_state_variable_count());");
		pw.println("  for (int i = 0; i < group_count(); i++) {");
		pw.println("    int sz = read_matrix(i)[0];");
		pw.println("    for (int j = 1; j < sz + 1; j++) {");
		pw.println("      int indj = read_matrix(i)[j];");
		pw.println("      dm_set(cm, i, indj);");
		pw.println("      dm_set(rm, i, indj);");
		pw.println("    }");
		pw.println("  }");
		pw.println("  GBsetDMInfoRead(m, rm);");

		// set the write dependency matrix
		pw.println("  matrix_t *wm = malloc(sizeof(matrix_t));");
		pw.println("  dm_create(wm, group_count(), get_state_variable_count());");
		pw.println("  for (int i = 0; i < group_count(); i++) {");
		pw.println("    int sz = write_matrix(i)[0];");
		pw.println("    for (int j = 1; j < sz + 1; j++) {");
		pw.println("      int indj = write_matrix(i)[j];");
		pw.println("      dm_set(cm, i, indj);");
		pw.println("      dm_set(wm, i, indj);");
		pw.println("    }");
		pw.println("  }");
		pw.println("  GBsetDMInfoMustWrite(m, wm);");

		// set the combined matrix
		pw.println("  GBsetDMInfo(m, cm);");

		//	    // set the label dependency matrix
		pw.println("  matrix_t *lm = malloc(sizeof(matrix_t));");
		pw.println("  dm_create(lm, label_count(), get_state_variable_count());");
		pw.println("  for (int i = 0; i < label_count(); i++) {\n");
		pw.println("    int sz = label_matrix(i)[0];");
		pw.println("    for (int j = 1; j < sz + 1; j++) {");
		pw.println(
				"      dm_set(lm, i, label_matrix(i)[j]);\n" + "    }\n" + "  }\n" + "  GBsetStateLabelInfo(m, lm);");

		// set guards
//		pw.println("  GBsetGuardsInfo(m,(guard_t**) &guardsPerTrans);");

		pw.println("  int sl_size = label_count();");
//		pw.println("  int nguards = " + (5*nbPos) + ";");
		// set the label group implementation
		pw.println("  sl_group_t* sl_group_all = malloc(sizeof(sl_group_t) + sl_size * sizeof(int));");
		pw.println("  sl_group_all->count = sl_size;");
		pw.println("  for(int i=0; i < sl_group_all->count; i++) sl_group_all->sl_idx[i] = i;");
		pw.println("  GBsetStateLabelGroupInfo(m, GB_SL_ALL, sl_group_all);");
//		pw.println("  if (nguards > 0) {");
//		pw.println("    sl_group_t* sl_group_guards = malloc(sizeof(sl_group_t) + nguards * sizeof(int));");
//		pw.println("    sl_group_guards->count = nguards;");
//		pw.println("    for(int i=0; i < sl_group_guards->count; i++) sl_group_guards->sl_idx[i] = i;");
//		pw.println("    GBsetStateLabelGroupInfo(m, GB_SL_GUARDS, sl_group_guards);");
//		pw.println("  }");
		// get state labels
		pw.println("  GBsetStateLabelsGroup(m, sl_group);");

		if (hasPartialOrder) {
			// NES
			pw.println("  int ngroups = group_count();");
			pw.println("  matrix_t *gnes_info = malloc(sizeof(matrix_t));");
			pw.println("  dm_create(gnes_info, sl_size, ngroups);");
			pw.println("  for(int i = 0; i < sl_size; i++) {");
			pw.println("    int sz = gal_get_label_nes_matrix(i)[0];");
			pw.println("    for (int j = 1; j < sz + 1; j++) {");
			pw.println("      dm_set(gnes_info, i, gal_get_label_nes_matrix(i)[j]);\n");
			pw.println("    }");
			pw.println("  }");
			pw.println("  GBsetGuardNESInfo(m, gnes_info);");

			// NDS : set guard necessary disabling set info
			pw.println("  matrix_t *gnds_info = malloc(sizeof(matrix_t));");
			pw.println("  dm_create(gnds_info, sl_size, ngroups);");
			pw.println("  for(int i = 0; i < sl_size; i++) {");
			pw.println("    int sz = gal_get_label_nds_matrix(i)[0];");
			pw.println("    for (int j = 1; j < sz + 1; j++) {");
			pw.println("      dm_set(gnds_info, i, gal_get_label_nds_matrix(i)[j]);\n");
			pw.println("    }");
			pw.println("  }");
			pw.println("  GBsetGuardNDSInfo(m, gnds_info);");

			// Co-enabling
			pw.println("  matrix_t *coEnab = malloc(sizeof(matrix_t));");
			pw.println("  dm_create(coEnab, group_count(), group_count());");
			pw.println("  for (int i = 0; i < group_count(); i++) {\n");
			pw.println("    int sz = coEnab_matrix(i)[0];");
			pw.println("    for (int j = 1; j < sz + 1; j++) {");
			pw.println("      dm_set(coEnab, i, coEnab_matrix(i)[j]);\n");
			pw.println("    }");
			pw.println("  }");
			pw.println("  GBsetGuardCoEnabledInfo(m, coEnab);");

			// DNA
			pw.println("  matrix_t *dna = malloc(sizeof(matrix_t));");
			pw.println("  dm_create(dna, group_count(), group_count());");
			pw.println("  for (int i = 0; i < group_count(); i++) {\n");
			pw.println("    int sz = dna_matrix(i)[0];");
			pw.println("    for (int j = 1; j < sz + 1; j++) {");
			pw.println("      dm_set(dna, i, dna_matrix(i)[j]);\n");
			pw.println("    }");
			pw.println("  }");
			pw.println("  GBsetDoNotAccordInfo(m, dna);");

		}

		pw.println("}");
	}

	private void printLabels(PrintWriter pw, List<int[]> rm, List<int[]> wm) {
		/*
		List<int[]> guards = new ArrayList<>();

		for (int i = 0; i < (5*nbPos); i++) {
			int[] gl = new int[2];
			gl[0] = 1;
			gl[1] = i;
			guards.add(gl);
		}

		pw.println("int * guardsPerTrans [" + guards.size() + "] =  {");
		for (Iterator<int[]> it = guards.iterator(); it.hasNext();) {
			int[] line = it.next();

			pw.print("  ((int[])");
			printArray(pw, line);
			pw.print(")");
			if (it.hasNext()) {
				pw.print(",");
			}
			pw.println();
		}
		pw.println("};");
		*/
		pw.println("int group_count() {");
		int count = 5*nbPos;
		if (! useSymmetries) {
			count += 1; // the reschedule transition
		}
		pw.println("  return " + count + " ;");
		pw.println("}");

		pw.println("int label_count() {");
		pw.println("  return " + labelCount() + " ;");
		pw.println("}");

		List<int[]> lm = new ArrayList<>(listAtoms.size()+invAtoms.size());
		for (AtomicProp ap : listAtoms) {
			BitSet lr = new BitSet();
			SparsePetriNet.addSupport(ap.getExpression(), lr);
			lm.add(convertToLine(lr));
		}
		for (AtomicProp ap : invAtoms) {
			BitSet lr = new BitSet();
			SparsePetriNet.addSupport(ap.getExpression(), lr);
			lm.add(convertToLine(lr));
		}
		if (listAtoms.size() + invAtoms.size() > 0)
			printMatrix(pw, "lm", lm);
		pw.print("int* label_matrix(int row) { return lm[row];}\n");
//		pw.print("int* label_matrix(int row) {\n" + "  if (row < " + (5*nbPos) + ") return rm[row];\n");
//		if (listAtoms.size() + invAtoms.size() > 0) {
//			pw.print("  else return lm[row-" + (5*nbPos) + "];\n");
//		}
//		pw.print("}\n");

		if (!hasPartialOrder) {
			return;
		}
/*		try {
			nes.init(net);

			IntMatrixCol mayEnable = nes.computeAblingMatrix(false);
			// short scopes for less memory peaks
			{
				// invert the logic for ltsmin
				// List<int[]> mayEnableSparse = mayEnable.stream().map(l ->
				// convertToLine(convertToBitSet(l))).collect(Collectors.toList());
				printMatrix(pw, "mayDisable", mayEnable);
			}

			{
				IntMatrixCol mayDisable = nes.computeAblingMatrix(true);
				// List<int[]> mayDisableSparse = mayDisable.stream().map(l ->
				// convertToLine(convertToBitSet(l))).collect(Collectors.toList());
				// logic is inverted
				printMatrix(pw, "mayEnable", mayDisable);
			}

			{
				List<int[]> ones = new ArrayList<>();
				int[] oneA = new int[(5*nbPos) + 1];
				oneA[0] = (5*nbPos);
				for (int i = 1; i < oneA.length; i++) {
					oneA[i] = i - 1;
				}
				ones.add(oneA);
				printMatrix(pw, "allOnes", ones);
			}

			{
				IntMatrixCol enab = new IntMatrixCol((5*nbPos), 0);
				IntMatrixCol disab = new IntMatrixCol((5*nbPos), 0);
				for (AtomicProp ap : listAtoms) {
					SparseIntArray[] lines = nes.computeAblingsForPredicate(ap.getExpression());
					disab.appendColumn(lines[0]);
					enab.appendColumn(lines[1]);
				}
				for (AtomicProp ap : invAtoms) {
					SparseIntArray[] lines = nes.computeAblingsForPredicate(ap.getExpression());
					disab.appendColumn(lines[0]);
					enab.appendColumn(lines[1]);
				}
				printMatrix(pw, "mayDisableAtom", disab);
				printMatrix(pw, "mayEnableAtom", enab);
			}

			pw.println("const int* gal_get_label_nes_matrix(int g) {");
			pw.println(" if (g <" + (5*nbPos) + ") return mayEnable[g];");
			pw.println(" return mayEnableAtom[g-" + (5*nbPos) + "];");
			pw.println("}");

			pw.println("const int* gal_get_label_nds_matrix(int g) {");
			pw.println(" if (g <" + (5*nbPos) + ") return mayDisable[g];");
			pw.println(" return mayDisableAtom[g-" + (5*nbPos) + "];");
			pw.println("}");

			IntMatrixCol coEnabled = nes.computeCoEnablingMatrix();
			printMatrix(pw, "coenabled", coEnabled);

			pw.println("const int* coEnab_matrix(int g) {");
			pw.println(" return coenabled[g];");
			pw.println("}");

			IntMatrixCol doNotAccord = nes.computeDoNotAccord(mayEnable);			
			printMatrix(pw, "dna", doNotAccord);

			pw.println("const int* dna_matrix(int g) {");
			pw.println(" return dna[g];");
			pw.println("}");

		} catch (Exception e) {
			System.err.println("Skipping mayMatrices nes/nds " + e.getMessage());
			e.printStackTrace();

			hasPartialOrder = false;
			//			pw.println("const int* gal_get_label_nes_matrix(int g) {");
			//			pw.println(" return lm[g];");
			//			pw.println("}");
			//
			//			pw.println("const int* gal_get_label_nds_matrix(int g) {");
			//			pw.println(" return lm[g];");
			//			pw.println("}");
		}
*/
	}

	private int labelCount() {
		// TODO: what AP do we need ?
		return listAtoms.size() ; //((5*nbPos) + listAtoms.size() + invAtoms.size());
	}

	public void printMatrix(PrintWriter pw, String matrixName, IntMatrixCol matrix) {
		pw.println("int *" + matrixName + "[" + matrix.getColumnCount() + "] = {");
		for (int i = 0; i < matrix.getColumnCount(); i++) {
			SparseIntArray line = matrix.getColumn(i);
			pw.print("  ((int[])");
			printArray(pw, line);
			pw.print(")");
			if (i < matrix.getColumnCount() - 1) {
				pw.print(",");
			}
			pw.print("\n");
		}
		pw.println("};");
	}

	private void printArray(PrintWriter pw, SparseIntArray line) {
		int sz = line.size();
		pw.print("{");
		pw.print(sz);
		if (sz != 0) {
			pw.print(",");
		}
		for (int i = 0; i < sz; i++) {
			pw.print(line.keyAt(i));
			if (i < sz - 1)
				pw.print(",");
		}
		pw.print("}");
	}

	private void printMatrix(PrintWriter pw, String matrixName, List<int[]> matrix) {
		pw.println("int *" + matrixName + "[" + matrix.size() + "] = {");
		for (Iterator<int[]> it = matrix.iterator(); it.hasNext();) {
			int[] line = it.next();
			pw.print("  ((int[])");
			printArray(pw, line);
			pw.print(")");
			if (it.hasNext()) {
				pw.print(",");
			}
			pw.print("\n");
		}
		pw.println("};");
	}

	private void printArray(PrintWriter pw, int[] line) {
		pw.print("{");
		for (int i = 0; i < line.length; i++) {
			pw.print(line[i]);
			if (i < line.length - 1)
				pw.print(",");
		}
		pw.print("}");
	}
	
	private void printObservationResolver(PrintWriter pw) {
		pw.println("int is_reversed_maximal(char *observation) {");
		pw.println("  int i, j;");
		pw.println("  for (i = 1, j = "+nbPos+" - 1; i < j; i++, j--) {");
		pw.println("  if (observation[i] > observation[j]) {");
		pw.println("  return true;");
		pw.println("  } else if (observation[i] < observation[j]) {");
		pw.println("  return false;");
		pw.println("  }}");
		pw.println("  return true; // the observation is symmetric");
		pw.println("}");
		
		pw.println("void get_observation_array(int *state, int pos, char *observation, int *isReflected) {");
		pw.println("int i, j;");

		pw.println("// Calculate the number of robots at each position and rotate");
		pw.println("for (i = 0; i < "+nbPos+"; i++) {");
		pw.println("observation[i] = (char)(state[ ((pos+i)*6+5) % "+ (6*nbPos) +" ]  & 0xFF) + 1;  // Convert sum to char and increment by 1");
		pw.println("}");

		pw.println("// If it's not reversed maximal, reflect it");
		pw.println("if (!is_reversed_maximal(observation)) {\n  *isReflected = 1;\n");
		pw.println("for (i = 1, j = "+nbPos+" - 1; i < j; i++, j--) {");
		pw.println("char temp = observation[i];");
		pw.println("observation[i] = observation[j];");
		pw.println("observation[j] = temp;");
		pw.println("}");
		pw.println("} else { *isReflected =0 ; }");

		// Null-terminate the string");
		pw.println("observation["+nbPos+"] = '\\0';");
		pw.println("}");

		// perfect hash
		pw.println("#include \"observations.h\"");
		
		pw.println("int get_observation(int *state, int pos, int *isReflected) {");
		pw.println(" char observation["+(nbPos+1)+"];\n"
				+ "get_observation_array(state, pos, observation, isReflected);\n");
		pw.println("const struct Record * r = in_word_set(observation, "+nbPos+") ; \n "
				+ "if (r) return r->index; else {"
				+ "char bb[256] ; for (int i=0;i<"+nbPos+";i++) bb[i]='0' + (observation[i]-1); bb["+nbPos+"]='\\0';"
				+ "printf(\"Fatal error no observation for %s \\n\",bb); return -1;} \n}\n");
		
		
		// the strat
		pw.println("extern int strat(int);");
		
		// canonization
		if (useSymmetries) {
			pw.println("\n"					
					+ "\n"
					+ "long value (int * state, int pos) {\n"
					+ "  // basically : 10 counters we merge to a single one.\n"
					+ "  long res = 1;\n"
					+ "  for (int i=0; i<5 ;i++) {\n"
					+ "    res = 31*res + state[pos*6+i] + 1;\n"
					+ "    res = 31*res + state[nbPos*6+ pos*5 +i] + 1;\n"
					+ "  }\n"
					+ "  return res;\n"
					+ "}\n"
					+ "\n"
					+ "\n"
					+ "void canonize (int * state) {\n"
					+ " int maxPosition=-1;\n"
					+ " int maxDirection=-1;\n"
					+ " int candidates [nbPos];\n"
					+ " int nbCand = 0;\n"
					+ " int nbSched = 0;\n int wasSched=0;"					
					+ " for (int i=0;i< 5* nbPos;i++) {"
					+ "   nbSched += state[6*nbPos+i];"
					+ " }\n"
					+ " if (nbSched == "+nbRobots+") {"
					+ "   memset(state+6*nbPos,0,5*nbPos*sizeof(int)); wasSched=1;"
					+ " }\n"
					+ "\n"
					+ " long maxVal = value(state, 0);\n"
					+ " candidates[nbCand++]=0;\n"
					+ " for (int i=1, ie=nbPos ; i < ie ; i++) {\n"
					+ "   long val = value(state,i);\n"
					+ "   if (val > maxVal) {\n"
					+ "     nbCand=0;\n"
					+ "     candidates[nbCand++]=i;\n"
					+ "   } else if (val == maxVal) {\n"
					+ "     candidates[nbCand++]=i;\n"
					+ "   }\n"
					+ " }\n"
					+ " struct Candidate {\n"
					+ "   int pos;\n"
					+ "   int dir;\n"
					+ " };\n"
					+ " struct Candidate candDir [2*nbPos]; \n"
					+ " for (int i=0;i<nbCand;i++) {\n"
					+ "   candDir[2*i].pos=candidates[i];\n"
					+ "   candDir[2*i].dir=1;\n"
					+ "   candDir[2*i+1].pos=candidates[i];\n"
					+ "   candDir[2*i+1].dir=-1;\n"
					+ " }\n"
					+ " int nbCandDir = nbCand*2;\n"
					+ "\n"
					+ " struct Candidate nextCandDir [2*nbPos];\n"
					+ " int nbNext=0;\n"
					+ "\n"
					+ " for (int d=1; d < nbPos-1 ; d++) {\n"
					+ "   long maxVal = value(state,(candDir[0].pos + candDir[0].dir * d + nbPos)%nbPos);\n"
					+ "   nextCandDir[nbNext++]=candDir[0];\n"
					+ "   for (int i=1; i < nbCandDir ; i++) {\n"
					+ "     int pos = candDir[i].pos + candDir[i].dir * d;\n"
					+ "     long val = value(state,pos);\n"
					+ "     if (val > maxVal) {\n"
					+ "       nbNext=0;\n"
					+ "       nextCandDir[nbNext++]=candDir[i];\n"
					+ "       maxVal = val;\n"
					+ "     } else if (val == maxVal) {\n"
					+ "       nextCandDir[nbNext++]=candDir[i]; \n"
					+ "     }\n"
					+ "   }\n"
					+ "   memcpy(candDir,nextCandDir,sizeof(candDir));\n"
					+ "   nbCandDir = nbNext;\n"
					+ "   nbNext=0;\n"
					+ "   if (nbCandDir==1) {\n"
					+ "     break;\n"
					+ "   }\n"
					+ " }\n"
					+ " if (!wasSched && candDir[0].pos==0 && candDir[0].dir==1) return;\n"
					+ "\n"
					+ " int tmp[11*nbPos];\n"
					+ "\n"
					+ " for (int i=0, ie = nbPos; i < ie ; i++) {\n"
					+ "   int cur = ( candDir[0].pos + candDir[0].dir * i + nbPos ) % nbPos;\n"
					+ "   memcpy(tmp + i*6, state + cur*6, 6*sizeof(int));\n"
					+ "   memcpy(tmp + nbPos*6 + i*5, state + nbPos*6 + cur*5, 5*sizeof(int));\n"					
					+ " }\n"
					+ " if (candDir[0].dir <0) { for (int i=0;i<nbPos;i++) {"
					+ "   int tt = tmp[i*6+"+Action.LEFT.ordinal()+ "];"
					+ "   tmp[i*6+"+Action.LEFT.ordinal()+ "] = tmp[i*6+"+Action.RIGHT.ordinal()+"];"
					+ "   tmp[i*6+"+Action.RIGHT.ordinal()+"] = tt;"
					+ "   tt = tmp[nbPos*6 + i*5+"+Action.LEFT.ordinal()+ "];"
					+ "   tmp[nbPos*6 + i*5+"+Action.LEFT.ordinal()+ "] = tmp[nbPos*6 + i*5+"+Action.RIGHT.ordinal()+"];"
					+ "   tmp[nbPos*6 + i*5+"+Action.RIGHT.ordinal()+"] = tt;"
					+ "}}\n"
					+ "	\n"
					);
			pw.println("int * src = state;");
			printTrace(pw, "tmp", "CANO", "");
			pw.println(
					" memcpy(state, tmp, 11*nbPos*sizeof(int));\n"
					+ "\n"
					+ "}\n"
					+ "");
			
		}
	}


	private void printNextState(PrintWriter pw) {

		pw.append("typedef struct state {\n");
		pw.append("  int state [" + (11*nbPos) + "];\n");
		pw.append("} state_t ;\n");
		
		printObservationResolver(pw);

		if (!forSpot) {
			pw.println("int next_state(void* model, int group, int *src, TransitionCB callback, void *arg) {");

			pw.println("  // provide transition labels and group number of transition.");
			pw.println("  int action[1];");
			// use the same identifier for group and action
			pw.println("  action[0] = group;");
			pw.println("  transition_info_t transition_info = { action, group };");

			pw.println("  int nbsucc = 0;");
			pw.println("  switch (group) {");
			for (int tindex = 0; tindex < (5*nbPos); tindex++) {
				pw.println("  case " + tindex + " :");
				int pos = tindex / 5;
				Action a = Action.values()[tindex%5];
				pw.println("    // pos="+pos+" "+a);

				if (a == Action.LOOK) {
					pw.append("  if (src ["+ (pos*6+ Action.LOOK.ordinal())+"] > 0) { ");
					pw.println("  state_t cur ;\n");
					pw.println("  memcpy( cur.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");

					pw.println("  int isReflected=0;");
					pw.println("  int obs = get_observation(cur.state,group/5,&isReflected);");
					pw.println("  if (obs<0) {");
					traceTransitions(pw, pos, a, "cur", "ERROR");
					pw.println("  }");
					pw.println("  int act = strat(obs);");
					pw.println("  if (act<0) printf(\"ERROR no strategy for observation index : %d \\n\",obs);");
					pw.println("  if (isReflected && act == 2) { act = 3; } else if (isReflected && act == 3) { act=2 ;}");
					pw.println("   cur.state[" + (pos*6+ Action.LOOK.ordinal()) + "] --;");
					pw.println("   cur.state[" + (pos*6) +"+ act] ++;");
					
					// update sched
					pw.println("  cur.state["+ (nbPos*6 + pos*5) +"+ act] ++; ");
					pw.println("  if (src["+ (nbPos*6 + pos*5 + Action.LOOK.ordinal()) +"] == src["+(pos*6+ Action.LOOK.ordinal())+"])"
							+ "  { cur.state["+ (nbPos*6 + pos*5 + Action.LOOK.ordinal()) +"] --; }");
					
					if (useSymmetries) pw.println("  canonize(cur.state);");
					pw.println("       nbsucc++; ");
					traceTransitions(pw, pos, a, "cur", "");
					pw.println("       callback(arg, &transition_info, cur.state, wm[group]);");
					pw.println("     }");
					pw.println("     break;");
				} else if (a == Action.LEFT) {
					pw.append("  if (src ["+ (pos*6+ Action.LEFT.ordinal())+"] > 0) { ");
					pw.println("  state_t cur ;\n");
					pw.println("  memcpy( cur.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");

					int left = (pos + nbPos-1) %  nbPos ;
					// we read and update LOOK and TOTAL of pos-1%N
					pw.println("   cur.state[ " + (left*6 + Action.LOOK.ordinal()) + "] ++;");
					pw.println("   cur.state[ " + (left*6 + Action.TOTAL.ordinal()) + "] ++;");
					// we read and update LEFT and TOTAL of pos
					pw.println("   cur.state[ " + (pos*6 + Action.LEFT.ordinal()) + "] --;");
					pw.println("   cur.state[ " + (pos*6 + Action.TOTAL.ordinal()) + "] --;");

					// update sched
					pw.println("  cur.state["+ (nbPos*6 + left*5 + Action.LOOK.ordinal())+ "] ++; ");
					pw.println("  if (src["+ (nbPos*6 + pos*5 + Action.LEFT.ordinal()) +"] == src["+(pos*6+ Action.LEFT.ordinal())+"])"
							+ "  { cur.state["+ (nbPos*6 + pos*5 + Action.LEFT.ordinal()) +"] --; }");

					if (useSymmetries) pw.println("  canonize(cur.state);");
					pw.println("       nbsucc++; ");
					traceTransitions(pw, pos, a, "cur", "");
					pw.println("       callback(arg, &transition_info, cur.state, wm[group]);");
					pw.println("     }");
					pw.println("     break;");
					} else if (a == Action.RIGHT) {
						pw.append("  if (src ["+ (pos*6+ Action.RIGHT.ordinal())+"] > 0) { ");						
						pw.println("  state_t cur ;\n");
						pw.println("  memcpy( cur.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");
						int right = (pos + 1) % nbPos;
						// we read and update LOOK and TOTAL of pos+1%N
						pw.println("   cur.state[ " + (right*6 + Action.LOOK.ordinal()) + "] ++;");
						pw.println("   cur.state[ " + (right*6 + Action.TOTAL.ordinal()) + "] ++;");
						// we read and update RIGHT and TOTAL of pos
						pw.println("   cur.state[ " + (pos*6 + Action.RIGHT.ordinal()) + "] --;");
						pw.println("   cur.state[ " + (pos*6 + Action.TOTAL.ordinal()) + "] --;");
						
						// update sched
						pw.println("  cur.state["+ (nbPos*6 + right*5 + Action.LOOK.ordinal())+ "] ++; ");
						pw.println("  if (src["+ (nbPos*6 + pos*5 + Action.RIGHT.ordinal()) +"] == src["+(pos*6+ Action.RIGHT.ordinal())+"])"
								+ "  { cur.state["+ (nbPos*6 + pos*5 + Action.RIGHT.ordinal()) +"] --; }");

						
						if (useSymmetries) pw.println("  canonize(cur.state);");
						pw.println("       nbsucc++; ");
						traceTransitions(pw, pos, a, "cur", "");
						pw.println("       callback(arg, &transition_info, cur.state, wm[group]);");
						pw.println("     }");
						pw.println("     break;");
					} else if (a == Action.STAY) {
						pw.append("  if (src ["+ (pos*6+ Action.STAY.ordinal())+"] > 0) { ");
						pw.println("  state_t cur ;\n");
						pw.println("  memcpy( cur.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");

						// we read and update LOOK and STAY of pos
						pw.println("   cur.state[ " + (pos*6 + Action.STAY.ordinal()) + "] --;");
						pw.println("   cur.state[ " + (pos*6 + Action.LOOK.ordinal()) + "] ++;");
						
						// update sched
						pw.println("  cur.state["+ (nbPos*6 + pos*5 + Action.LOOK.ordinal())+ "] ++; ");
						pw.println("  if (src["+ (nbPos*6 + pos*5 + Action.STAY.ordinal()) +"] == src["+(pos*6+ Action.STAY.ordinal())+"])"
								+ "  { cur.state["+ (nbPos*6 + pos*5 + Action.STAY.ordinal()) +"] --; }");

						
						if (useSymmetries) pw.println("  canonize(cur.state);");
						pw.println("       nbsucc++; ");
						traceTransitions(pw, pos, a, "cur", "");
						pw.println("       callback(arg, &transition_info, cur.state, wm[group]);");
						pw.println("     }");
						pw.println("     break;");
						
					} else if (a == Action.MOVE) {
						// we read and update MOVE and TOTAL of pos
						pw.append("  if (src ["+ (pos*6+ Action.MOVE.ordinal())+"] > 0) { ");
						
						pw.println("  state_t cur ;\n");
						pw.println("  memcpy( cur.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");

						int left = (pos + nbPos-1) %  nbPos ;
						pw.println("   cur.state[ " + (pos*6 + Action.MOVE.ordinal()) + "] --;");
						pw.println("   cur.state[ " + (pos*6 + Action.TOTAL.ordinal()) + "] --;");
						// we read and update LOOK and TOTAL of pos-1%N
						pw.println("   cur.state[ " + (left*6 + Action.LOOK.ordinal()) + "] ++;");
						pw.println("   cur.state[ " + (left*6 + Action.TOTAL.ordinal()) + "] ++;");
						
						// update sched
						pw.println("  cur.state["+ (nbPos*6 + left*5 + Action.LOOK.ordinal())+ "] ++; ");
						pw.println("  if (src["+ (nbPos*6 + pos*5 + Action.MOVE.ordinal()) +"] == src["+(pos*6+ Action.MOVE.ordinal())+"])"
								+ "  { cur.state["+ (nbPos*6 + pos*5 + Action.MOVE.ordinal()) +"] --; }");

						if (useSymmetries) pw.println("  canonize(cur.state);");
						pw.println("       nbsucc++; ");
						traceTransitions(pw, pos, a, "cur", "");
						pw.println("       callback(arg, &transition_info, cur.state, wm[group]);");
						
						
						
						pw.println("  state_t cur2 ;\n");
						pw.println("  memcpy( cur2.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");

						int right = (pos + 1) % nbPos;
						pw.println("   cur2.state[ " + (pos*6 + Action.MOVE.ordinal()) + "] --;");
						pw.println("   cur2.state[ " + (pos*6 + Action.TOTAL.ordinal()) + "] --;");
						
						// we read and update LOOK and TOTAL of pos+1%N
						pw.println("   cur2.state[ " + (right*6 + Action.LOOK.ordinal()) + "] ++;");
						pw.println("   cur2.state[ " + (right*6 + Action.TOTAL.ordinal()) + "] ++;");

						// update sched
						pw.println("  cur2.state["+ (nbPos*6 + right*5 + Action.LOOK.ordinal())+ "] ++; ");
						pw.println("  if (src["+ (nbPos*6 + pos*5 + Action.MOVE.ordinal()) +"] == src["+(pos*6+ Action.MOVE.ordinal())+"])"
								+ "  { cur2.state["+ (nbPos*6 + pos*5 + Action.MOVE.ordinal()) +"] --; }");

						if (useSymmetries) pw.println("  canonize(cur2.state);");
						pw.println("       nbsucc++; ");
						traceTransitions(pw, pos, a, "cur2", "");
						pw.println("       callback(arg, &transition_info, cur2.state, wm[group]);");
						pw.println("     }");
						pw.println("     break;");
					}
				
			}
			if (!useSymmetries) {
			pw.println("case "+(5*nbPos) +":   // reschedule");
			pw.print("  if (");
			for (int i=0;i < 5*nbPos ; i++) {
				if (i!=0) pw.print("+");
				pw.print(" src["+ (6*nbPos+i) + "]");
			}
			pw.println(" == "+nbRobots +") {");
			pw.println("  state_t cur ;\n");
			pw.println("  memcpy( cur.state, src,  sizeof(int)* " + (11*nbPos) + ");\n");

			for (int i=0;i < 5*nbPos ; i++) {
				pw.println("cur.state[" +(6*nbPos+i)+ "]=0;");
			}
			
			pw.println("       nbsucc++; ");
			printTrace(pw, "cur.state", "", "[    SCHED    ]");
			pw.println("       callback(arg, &transition_info, cur.state, wm[group]);");
			pw.println("  }");
			pw.println("  break;");
			}
			
			pw.println("  default : return 0 ;");
			pw.println("  } // end switch(group) ");
			pw.println("  return nbsucc; // return number of successors");
			pw.println("}");

			CExpressionPrinter printer = new CExpressionPrinter(pw, "src");
			/////// Handle Labels similarly
			pw.println("int state_label(void* model, int label, int* src) {");
			// labels
			List<AtomicProp> alist = new ArrayList<>(listAtoms);
			alist.addAll(invAtoms);
			

			pw.println("  switch (label) {");
			// guards 
//			for (int tindex = 0, tie = (5*nbPos); tindex < tie; tindex++) {
//				pw.println("  case " + tindex + " : ");
//				pw.println("     return " + buildGuard(net.getFlowPT().getColumn(tindex), "src") + ";");
//			}
//			// labels
//			for (int tindex = (5*nbPos); tindex < (5*nbPos) + alist.size(); tindex++) {
			for (int tindex =0 ; tindex < alist.size(); tindex++) {
				pw.println("      case " + tindex + " : ");
				pw.append("        return ");
				alist.get(tindex).getExpression().accept(printer);
				pw.println(";");
			}
			pw.println("  default : return 0 ;");
			pw.println("  } // end switch(group) ");

			pw.println("}");

			pw.println("int state_label_many(void* model, int * src, int * label, int guards_only) {");
			pw.println("  (void)model;");
//			for (int tindex = 0; tindex < (5*nbPos); tindex++) {
//				pw.println("  label[" + tindex + "] = " + buildGuard(net.getFlowPT().getColumn(tindex), "src") + ";");
//			}
//			pw.println("  if (guards_only) return 0; ");
//			for (int tindex = (5*nbPos); tindex < (5*nbPos) + alist.size(); tindex++) {
//				pw.println("  label[" + tindex + "] = ");
//				alist.get(tindex - (5*nbPos)).getExpression().accept(printer);
//				pw.println(" ;");
//			}
			pw.println("  return 0; // return number of successors");
			pw.println("}");

			pw.println("void sl_group (model_t model, sl_group_enum_t group, int*src, int *label)");
			pw.println("  {");
			pw.println("  state_label_many (model, src, label, group == GB_SL_GUARDS);");
			pw.println("  (void) group; // Both groups overlap, and start at index 0!");
			pw.println("  }");

			pw.println("void sl_all (model_t model, int*src, int *label)");
			pw.println("  {");
			pw.println("  state_label_many (model, src, label, 0);");
			pw.println("  }");
		} else  {
			CExpressionPrinter printer = new CExpressionPrinter(pw, "src");
			// For Spot : generate a state labeling function just for APs
			pw.println("int eval_state_label(int label, const int* src) {");
			// labels
			pw.println("    switch (label) {");
			Iterator<AtomicProp> ap = listAtoms.iterator();
			for (int tindex = 0, te = listAtoms.size() ; tindex < te ; tindex++) {
				pw.println("      case " + tindex + " : ");
				pw.append("        return ");
				ap.next().getExpression().accept(printer);
				pw.println(";");
			}			
			pw.println("    }");
			pw.println("  }");
			//int (*get_state_size)();
		    //const char* (*get_state_variable_name)(int var);
		    
		    pw.println("int get_state_label_count() { return "+listAtoms.size() + ";}");
			
		    pw.println("const char * labelnames ["+listAtoms.size()+"] = {");
		    int ind = 0;
		    for (AtomicProp a : listAtoms) {
		    	pw.print("  \"LTLAP" + a.getName()+ "\"");
		    	if (ind < listAtoms.size()) {
		    		pw.print(",");
		    	}
		    	pw.println();
	    		++ind;
		    }
		    pw.println("};");
			pw.println("const char * get_state_label_name(int label) { return labelnames[label] ;} ");
		
		}
	}

	public void traceTransitions(PrintWriter pw, int pos, Action a, String cur, String reason) {
	    
	    	
	    	String tname = "[ pos=" + pos + " A=" + a + "]";
	    	
	    	printTrace(pw, cur+".state", reason, tname);
	    
	}

	public void printTrace(PrintWriter pw, String cur, String reason, String tname) {
		if (DEBUG < 2) { return; }
		
		pw.println("{");
		
		pw.println("  char buffer[2048];");
		pw.println("  int offset = 0;");
		if (reason.length() >0) {
			pw.println("    offset += snprintf(buffer + offset, sizeof(buffer) - offset, \""+ reason +" :\");");		        
		}
		traceState(pw,"src");
		pw.println("  offset += snprintf(buffer + offset, sizeof(buffer) - offset, \" " + tname + " \");");
		traceState(pw, cur);
		pw.println("  offset += snprintf(buffer + offset, sizeof(buffer) - offset, \"\\n\");");
		pw.println("  printf(\"%s\", buffer);");
		pw.println("}");
	}

	public void traceState(PrintWriter pw, String cur) {
		pw.println("  for (int i=0; i < " + (nbPos) + "; i++) {");
		pw.println("  for (int j=0; j < 6 ; j++) {");
		pw.println("    offset += snprintf(buffer + offset, sizeof(buffer) - offset, \"%d,\", " + cur + "[i*6+j]);");
		pw.println("  }");
		pw.println("    offset += snprintf(buffer + offset, sizeof(buffer) - offset, \" | \");");
		pw.println("  }");
		pw.println("    offset += snprintf(buffer + offset, sizeof(buffer) - offset, \" :: \");");
		pw.println("  for (int i=0; i < " + (nbPos) + "; i++) {");
		pw.println("  for (int j=0; j < 5 ; j++) {");
		pw.println("    offset += snprintf(buffer + offset, sizeof(buffer) - offset, \"%d,\", " + cur + "[nbPos*6 + i*5 +j]);");
		pw.println("  }");
		pw.println("    offset += snprintf(buffer + offset, sizeof(buffer) - offset, \" | \");");
		pw.println("  }");		
	}



	public String buildGuard(SparseIntArray pt, String prefix) {
		if (pt.size() == 0) {
			return "true";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0, ie = pt.size(); i < ie; i++) {
			sb.append(prefix + "[" + pt.keyAt(i) + "] >=" + pt.valueAt(i));
			if (i < ie - 1) {
				sb.append(" && ");
			}
		}
		String guard = sb.toString();
		return guard;
	}

	public String printLTLProperty(Property prop) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		PropertyPrinter pp = new PropertyPrinter(pw, "src", forSpot);
		atoms.getAPformula(prop.getName()).accept(pp);
		pw.close();
		return baos.toString();
	}

	private AtomicPropManager atoms = new AtomicPropManager();
	private List<AtomicProp> listAtoms = new ArrayList<>();
	private boolean forSpot;
	private List<AtomicProp> invAtoms = new ArrayList<>();
	
	public void transform(String cwd, boolean withPorMatrix, boolean forSpot, int nbPos, int nbRobots, List<AtomicProp> listAtoms, boolean useSymmetries) {
		this.nbPos = nbPos;
		this.nbRobots = nbRobots;
		this.forSpot = forSpot;
		this.useSymmetries = useSymmetries;
		//		if ( spec.getMain() instanceof GALTypeDeclaration ) {
		//			Logger.getLogger("fr.lip6.move.gal").fine("detecting pure GAL");
		//		} else {
		//			Logger.getLogger("fr.lip6.move.gal").fine("Error transformation does not support hierarchy yet.");
		//			return;
		//		}
		long time = System.currentTimeMillis();

		
		if (listAtoms != null) {
			this.listAtoms = listAtoms;
		} 
		
		{
			List<Expression> expr = new ArrayList<>();
			for (int i=0; i< nbPos ; i++) {
				expr.add(Expression.op(Op.EQ, Expression.var(6*i + Action.TOTAL.ordinal()), Expression.constant(nbRobots)));
			}		
			this.listAtoms.add(new AtomicProp("tower", Expression.nop(Op.OR,expr)));
		
		}
		{
			List<Expression> expr = new ArrayList<>();
			for (int i=0; i< 5*nbPos ; i++) {
				expr.add(Expression.op(Op.EQ, Expression.var(6*nbPos + i), Expression.constant(0)));
			}
			this.listAtoms.add(new AtomicProp("sched", Expression.nop(Op.AND,expr)));
		
		}
		hasPartialOrder = withPorMatrix;
		try {
			buildBodyFile(cwd + "/model.c");
			if (!forSpot)
				buildHeader(cwd + "/model.h");

			Logger.getLogger("fr.lip6.move.gal").info("Built C files in " + (System.currentTimeMillis() - time)
					+ "ms conformant to PINS "+(forSpot?"(SPOT variant)":"(ltsmin variant)")+"in folder :" + cwd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
