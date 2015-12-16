package org.jboxcar.algorithm;

import java.util.Arrays;

import org.jboxcar.api.Car;

import com.sun.xml.internal.stream.buffer.stax.StreamReaderBufferProcessor;

public class JBCIndividual extends Individual {
	private Car car;
	private double fitness;
	private int nParams;
	private int bits;
	/*
	 *  The encoding will be as follows:
	 *           __bits__
	 *          [ | | | ]
	 *  nParams |-+-+-+-|
	 *          [_|_|_|_]
	 *          
	 *  or simply nParams x bits matrix
	 */
	private final byte[][] encoding;
	
	public static enum DataType {
		FLOAT, DOUBLE
	}
	
	private DataType dataType;
	
	public JBCIndividual(Car car, int nParams, DataType dataType) {
		this.car = car;
		this.nParams = nParams;
		this.bits = dataType == DataType.FLOAT ? Float.BYTES*8 : Double.BYTES*8; 
		this.dataType = dataType;
		encoding = new byte[nParams][bits];
		for (int i = 0; i < nParams; i++) {
			for (int j = 0; j < bits; j++) {
				encoding[i][j] = (byte) Math.round(Math.random());
			}
		}
	}
	
	public JBCIndividual(Car car, int nParams) {
		this(car, nParams, DataType.DOUBLE);
	}
	
	public int getnParams() {
		return nParams;
	}
	
	public void setParam(int param, byte[] data) {
		if (param < 0 || param >= nParams) {
			String err = String.format("param must be in range[0,%d]",nParams-1);
			throw new IllegalArgumentException(err);
		}
		if (data.length > bits) {
			String err = String.format("bits must be in range[0, %d]", bits-1);
			throw new IllegalArgumentException(err);
		}
		for (int j = 0; j < data.length; j++) {
			encoding[param][j] = data[j];
		}
	}
	
	public void setParam(int param, double value) {
		byte[] data = new byte[bits];
		int padding = 0;
		String strRep = null;
		if (dataType == DataType.DOUBLE) {
			strRep = Long.toBinaryString(Double.doubleToRawLongBits(value));
			padding = bits-strRep.length();
		} else if (dataType == DataType.FLOAT) {
			strRep = Integer.toBinaryString(Float.floatToRawIntBits((float) value));
			padding = bits-strRep.length();
		}
		
		for (int i = bits-1, j = strRep.length()-1; i >= 0 && j >= 0; i -= 1, j -= 1) {
			data[i] = (byte) (strRep.charAt(j) == '1' ? 1 : 0);
		}
		
		for (int i = 0; i < padding; i++) {
			data[i] = 0;
		}
		
		setParam(param, data);
	}
	
	public float getParamAsFloat(int param) {
		if (param < 0 || param >= nParams) {
			String err = String.format("param must be in range[0,%d]",nParams-1);
			throw new IllegalArgumentException(err);
		}
		String binaryRep = Arrays.toString(encoding[param]).replaceAll("[\\[, \\]]", "");
		return Float.intBitsToFloat(Integer.parseInt(binaryRep, 2));
	}
	
	public double getParamAsDouble(int param) {
		if (param < 0 || param >= nParams) {
			String err = String.format("param must be in range[0,%d]", nParams-1);
			throw new IllegalArgumentException(err);
		}
		// replace all '[', ',', ']' and ' ' with ''
		String binaryRep = Arrays.toString(encoding[param]).replaceAll("[\\[, \\]]", "");
		return Double.longBitsToDouble(Long.parseLong(binaryRep, 2));
	}
	
	public byte[][] getEncoding() {
		return encoding;
	}
	
	@Override
	public double calculateFitness() {
		return car.getPosition().x;
	}
	
	
	public double getFitness() {
		return fitness;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String rowHeading = "Param";
		String colHeading = "Bits:";
		int size = 11;
		sb.append(String.format("%1$" + (size-colHeading.length()-1) + "s", ""));
		sb.append(String.format("%s [1 2 3 ... %d]", colHeading, bits)+System.lineSeparator());
		for (int i = 0; i < encoding.length; i++) {
			String t = String.format("%s %d: ", rowHeading, i+1);
			sb.append(String.format("%1$-" + size + "s", t));
			sb.append("[");
			for (int j = 0; j < encoding[i].length; j++) {
				sb.append(encoding[i][j] + (j == encoding[i].length-1 ? "]" : " "));
			}
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}
	
	public int getnBits() {
		return bits;
	}
	
	public byte[] getParam(int param) {
		if (param < 0 || param >= getnParams()) {
			String err = String.format("param must be between [0, %d]", getnParams()-1);
			throw new IllegalArgumentException(err);
		}
		return encoding[param];
	}
}
