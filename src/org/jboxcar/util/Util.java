package org.jboxcar.util;

import java.util.ArrayList;
import java.util.Collections;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class Util {
	public static float cosd(double degrees){
		return (float) Math.cos(Math.toRadians(degrees));
	}
	
	public static float sind(double degrees) {
		return (float) Math.sin(Math.toRadians(degrees));
	}
	
	
	public static PolygonShape b2SetAsArray(PolygonShape polygon, Vec2 vertices[]) {
		return b2SetAsArray(polygon, vertices, 0);
	}
	
	public static PolygonShape b2SetAsArray(PolygonShape polygon, Vec2 vertices[], int  vertexCount) {
		return b2SetAsVector(polygon, vertices, vertexCount);
	}
	
	public static PolygonShape b2SetAsVector(PolygonShape polygon, Vec2 vertices[], int vertexCount) {
		if (vertexCount == 0) {
			vertexCount = vertices.length;
		}
		
		polygon.set(vertices, vertexCount);
		for (int i = 0; i < vertexCount; i++) {
			polygon.m_vertices[i] = vertices[i];
		}
		
		for (int i = 0; i < polygon.getVertexCount(); ++i) {
			int i2 = i+1 < polygon.getVertexCount() ? i+1 : 0;
			Vec2 edge = b2SubtractVV(polygon.m_vertices[i2], polygon.m_vertices[i]);
			polygon.m_normals[i].set(b2CrossVF(edge, 1.0f));
			polygon.m_normals[i].normalize();
		}
		polygon.computeCentroidToOut(polygon.m_vertices, polygon.m_count, polygon.m_centroid);
		return polygon;
	}
	
	public static Vec2 b2SubtractVV(Vec2 a, Vec2 b) {
		return new Vec2(a.x - b.x, a.y - b.y);
	}
	
	public static Vec2 b2CrossVF(Vec2 a, float s) {
		return new Vec2(s * a.y, (-s * a.x));
	}
	
	public static Vec2 b2CrossVF(Vec2 a) {
		return b2CrossVF(a, 0.0f);
	}
	
	public static PolygonShape b2SetAsVector(PolygonShape polygon, Vec2 vertices[]) {
		return b2SetAsVector(polygon, vertices, 0); 
	}
	
	public static int rand(int min, int max) {
		if (min > max) {
			int t = min;
			min = max;
			max = t;
		}
		return (int) (min + Math.random()*(max-min));
	}
	
	public static float rand(float min, float max) {
		if (min > max) {
			float t = min;
			min = max;
			max = t;
		}
		return (float) (min + Math.random()*(max-min));
	}
	
	public static Color hex2Rgb(String color) {
		int startIdx = 0;
		if (color.startsWith("#")) {
			startIdx = 1;
		}
		int rgb[] = new int[3];
		for (int i = 0; i < rgb.length; i++) {
			rgb[i] = Integer.valueOf(color.substring(startIdx, startIdx+2));
			startIdx+=2;
		}
		return Color.rgb(rgb[0], rgb[1], rgb[2]);
	}
	
	public String rgb2Hex(int r, int g, int b) {
		return String.format("#%02X%02X#02X", r,g,b);
	}
	
	public static ArrayList<Integer> randPerm(int min, int max) {
		if (min > max) {
			int t = min;
			min = max;
			max = t;
		}
		ArrayList<Integer> list = new ArrayList<>(max-min);
		for (int i = min; i < max; i++) {
			list.add(i);
		}
		Collections.shuffle(list);
		return list;
	}
	
	public static int[][] reshape(int[][] matrix, int m, int n) {
		int origM = matrix.length;
		int origN = matrix[0].length;
		if (origM * origN != m*n) {
			throw new IllegalArgumentException("New matrix must contain same number of elements as the old");
		}
		
		int[][] ret = new int[m][n];
		int[] matrix1D = new int[matrix.length*matrix[0].length];
		
		// Turn the 2D matrix into a 1D
		int index = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j =0; j < matrix[0].length; j++) {
				matrix1D[index++] = matrix[i][j];
			}
		}
		
		// Reshape into the size requested
		index = 0;
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < m; i++) {
				ret[i][j] = matrix1D[index++];
			}
		}
		
		return ret;
	}
	
	public static int[][] reshape(int[] array, int m, int n) {
		int[][] t = new int[array.length][1];
		for (int i = 0; i < array.length; i++) {
			t[i][0] = array[i];
		}
		return reshape(t, m, n);
	}
	
	public static int[][] reshape(ArrayList<Integer> array, int m, int n) {
		int[] t = new int[array.size()];
		for (int i = 0; i < array.size(); i++) {
			t[i] = array.get(i);
		}
		return reshape(t, m, n);
	}
	
	/**
	 * 
	 * @param array
	 * @param mask
	 * @param maskType
	 * 		0 : logical OR
	 * 		1 : logical AND
	 * 		2 : logical XOR
	 * @return
	 */
	public static int[] maskArray(int[] array, byte[] mask, int maskType) {
		if (array.length != mask.length) {
			throw new IllegalArgumentException("array and mask must be the same length");
		}
		int[] ret = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			switch(maskType) {
			case 0:
				ret[i] = array[i] | mask[i];
				break;
			case 1:
				ret[i] = array[i] & mask[i];
				break;
			case 2:
				ret[i] = array[i] ^ mask[i];
				break;
			default:
					break;
			}
		}
		return ret;
	}
	
	/**
	 * Creates an array from [start, end] (inclusive) of the cumulated sum
	 * 
	 * @param start
	 * 		The integer to start at (inclusive)
	 * @param end
	 * 		The integer to end at (inclusive)
	 * @return
	 * 		An array of cumulated sum from start to end
	 * 
	 */
	public static int[] cumsum(int start, int end) {
		int[] ret = new int[end-start+1];
		int sum = 0;
		for (int i = start, j=0; i <= end; i++, j++) {
			sum += i;
			ret[j] = sum;
		}
		return ret;
	}
}
