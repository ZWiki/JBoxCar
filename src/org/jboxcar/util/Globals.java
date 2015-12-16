package org.jboxcar.util;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

public class Globals {
	public static World world;
	public static Vec2 gravity = new Vec2(0, -9.81f);
	// TODO: Should I make motorSpeed part of the GA?
	public static final boolean GET_TACO_BELL = true;
	public static final int maxGradient = 0x000000FF;
	public static float motorSpeed = 15; // Radians per second
	public static long fpsScreen = 60;
	public static long fpsBox2D = 60;
	public static final String version = "v1.0.0";
	public static final String title = "JBoxCar - Computational Intelligence -v" + version;
	public static boolean output = false;
	public static boolean viz = true;
}
