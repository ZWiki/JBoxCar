package org.jboxcar.algorithm;

public abstract class GeneticAlgorithm {
	protected abstract void crossover();
	protected abstract void mutation();
	protected abstract void selection();
	public abstract void evolvePopulation();
	
}
