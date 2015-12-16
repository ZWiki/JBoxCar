package org.jboxcar.algorithm;

import java.util.ArrayList;
import java.util.Arrays;

import org.jbox2d.common.Vec2;
import org.jboxcar.api.Car;
import org.jboxcar.api.Wheel;
import org.jboxcar.util.Util;

public class JBCGeneticAlgorithm extends GeneticAlgorithm{
	private ArrayList<JBCIndividual> population;
	private int populationSize;
	private double probMutation = .01;
	private double probCrossover = .70;
	private boolean usingElitism = true;
	private double maxFitness = 0.0;
	private Individual elite = null;
	
	int bits = 64;
	int nParams = 40;
	
	public static enum Crossover {
		UNIFORM, ONE_POINT, TWO_POINT
	}
	
	public static enum Selection {
		UNIFORM_RANDOM, ROULETTE
	}

	private Crossover crossoverType;
	private Selection selectionType;

	public JBCGeneticAlgorithm(ArrayList<Car> cars, double probCrossover, double probMutation) {
		this.populationSize = cars.size();
		this.probCrossover = probCrossover;
		this.probMutation = probMutation;
		this.population = new ArrayList<JBCIndividual>(populationSize);
		this.crossoverType = Crossover.TWO_POINT;
		this.selectionType = Selection.ROULETTE;
		/**
		 * nParam setup
		 * param 00 - 15 are the vertices X and Y
		 * param 16 - 23 are the densities of vertex 1, 2, 3, ... 8
		 * param 24 - 31 are the wheel densities
		 * param 32 - 39 are the wheel radius
		 * 
		 */
		for (int i = 0; i < populationSize; i++) {
			JBCIndividual individual = new JBCIndividual(cars.get(i), nParams);
			
			ArrayList<Vec2> vertices = cars.get(i).getChassisVertices();
			for (int j = 0; j < vertices.size()-1; j+=2) {
				individual.setParam(j, vertices.get(j).x);
				individual.setParam(j+1, vertices.get(j).y);
			}
			
			float[] chassisDensities = cars.get(i).getChassisDensities();
			int idx = 0;
			for (int j = 16; j <= 23; j++) {
				individual.setParam(j, chassisDensities[idx++]);
			}
			
			idx = 0;
			Wheel[] wheels = cars.get(i).getWheels();
			for (int j = 24; j <= 31; j++) {
				try {
					individual.setParam(j, wheels[idx++].getDensity());
				} catch (Exception e) {
					individual.setParam(j, 0);
				}
			}
			
			idx = 0;
			for (int j = 32; j <= 39; j++) {
				try{
					individual.setParam(j, wheels[idx++].getRadius());
				} catch(Exception e) {
					individual.setParam(j, 0);
				}
			}
			population.add(individual);
		}
	}
	
	public JBCGeneticAlgorithm(Car[] cars, double crossoverRate, double probMutation) {
		this(new ArrayList<Car>(Arrays.asList(cars)), crossoverRate, probMutation);
	}
	
	@Override
	protected void crossover() {
		// Create a bitmask
		int[] bitmask = new int[population.get(0).getnBits()];
		switch (crossoverType) {
		case ONE_POINT:
			int x = Util.rand(1, bitmask.length-1);
			for (int i =x; i < bitmask.length; i++) {
				bitmask[i] = 1;
			}
			break;
		case UNIFORM:
			for (int i = 0; i < bitmask.length; i++) {
				if (Math.random() <= .5) {
					bitmask[i] = 1;
				}
			}
			
			break;
		// This two point crossover allows for wrap around
		case TWO_POINT:
		default:
			int a = Util.rand(1, bitmask.length);
			int b;
			while((b = Util.rand(1, bitmask.length)) == a) {};
			// [0, ..., a, ... b, ...]
			// Swap bits between [a,b]
			if (a < b) {
				for (int i = a; i < b; i++) {
					bitmask[i] = 1;
				}
			} else {
				// [0, ... b, ..., a, ...]
				// Swap bits between [0, b) and [a,bitmask.length)
				for (int i = 0; i < b; i++) {
					bitmask[i] = 1;
				}
				for (int i = a; i < bitmask.length; i++) {
					bitmask[i] = 1;
				}
				
			}
		}
		
		//ArrayList<JBCIndividual> newPopulation = new ArrayList<JBCIndividual>(population);
		
		// Create a (populationSize/2) x 2 matrix of randomly generated parents
		// where each row will contain two columns of the parents that will attempt
		// crossover
		int[][] parents;
		ArrayList<Integer> parentPermutation = Util.randPerm(0, population.size());
		// reshape the parentPermutation into the parent matrix
		parents = Util.reshape(parentPermutation, population.size()/2, 2);
		// Create a mask for the crossover to determine which parents will crossover
		int[] crossoverMask = new int[population.size()/2];
		for (int i = 0; i < crossoverMask.length; i++) {
			crossoverMask[i] = (Math.random() < probCrossover) ? 1 : 0;
		}
		
		for (int group = 0; group < parents.length; group++) {
			// if the mask is 0 for these parents, they are not marked for crossover
			if (crossoverMask[group] == 0) {
				continue;
			}
			
			// Grab the two individuals
			JBCIndividual p1, p2;
			p1 = population.get(parents[group][0]);
			p2 = population.get(parents[group][1]);
			
			// For each param (or dimension) in the individual
			for (int param = 0; param < p1.getnParams(); param++) {
				byte[] t = p1.getParam(param);
				for (int i = 0; i < bitmask.length; i++) {
					// If the mask has a 1 at the location, we swap the bits
					if (bitmask[i] == 1) {
						p1.getEncoding()[param][i] = p2.getEncoding()[param][i];
						p2.getEncoding()[param][i] = t[i];
					}
				}
			}
			
			// Swap old chromosomes for new ones
			population.set(parents[group][0], p1);
			population.set(parents[group][1], p2);
		}
		
		
	}

	@Override
	protected void mutation() {
		for (JBCIndividual individual : population) {
			// do not mutate the elite
			if (!individual.equals(elite)) {
				if (Math.random() < probMutation) {
					for (int i = 0; i < individual.getnParams(); i++) {
						byte[] bits = new byte[individual.getnBits()];
						for (int j = 0; j < individual.getnBits(); j++) {
							if (Math.random() < .5) {
								bits[j] = 1;
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void selection() {
		int[] idx = new int[population.size()];
		switch(selectionType) {
		case UNIFORM_RANDOM:
			for (int i = 0; i < idx.length; i++) {
				idx[i] = Util.rand(0, population.size());
			}
			break;
		case ROULETTE:
		default:
			double totalFitness = 0.0;
			double[] probs = new double[population.size()];
			for (JBCIndividual individual : population) {
				totalFitness += individual.getFitness();
			}
			for (int i = 0; i < probs.length; i++) {
				probs[i] = (population.get(i).getFitness())/totalFitness;
			}
			
			double[] cumsum = new double[population.size()];
			double sum = 0;
			for (int i = 0; i < probs.length; i++) {
				sum += probs[i];
				cumsum[i] = sum;
			}
			
			for (int i = 0; i < idx.length; i++) {
				double r = Math.random();
				for (int j = 0; j < cumsum.length; j++) {
					if (cumsum[j] > r) {
						idx[i] = j;
						break;
					}
				}
			}
			break;
		}
		
		ArrayList<JBCIndividual> newPopulation = new ArrayList<JBCIndividual>();
		for (int i = 0; i < idx.length; i++) {
			newPopulation.add(population.get(idx[i]));
		}
		
		//TODO: use elitism
		population = new ArrayList<JBCIndividual>(newPopulation);
		
	}
	
	@Override
	public void evolvePopulation() {
		selection();
		crossover();
		mutation();
	}
	

	
	public void setUsingElitsm(boolean usingElitism) {
		
	}
	
	public ArrayList<JBCIndividual> getPopulation() {
		return population;
	}
	
	public Selection getSelectionType() {
		return selectionType;
	}
	
	public void setSelectionType(Selection selectionType) {
		this.selectionType = selectionType;
	}
	private JBCIndividual getFittest() {
		JBCIndividual fittest = population.get(0);
		for (JBCIndividual individual : population) {
			if (individual.getFitness() > fittest.getFitness()) {
				fittest = individual;
			}
		}
		return fittest;
	}


}
