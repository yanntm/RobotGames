package fr.lip6.move.robots;

/**
 * 
 * The actions of a robot.
 * The order in which they appear in this list is semantic, so do not reorder.
 *
 */
public enum Action {
	// when robot is lost, but wants to move
	MOVE,
	// no movement desired
	STAY,
	// when view is asymmetric
	LEFT,
	RIGHT
}
