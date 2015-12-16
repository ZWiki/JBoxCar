package org.jboxcar.api;

import static org.jboxcar.util.Globals.fpsBox2D;
import static org.jboxcar.util.Globals.world;
import static org.jboxcar.util.Util.b2SetAsArray;

import java.util.ArrayList;
import java.util.Random;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jboxcar.algorithm.JBCGeneticAlgorithm;
import org.jboxcar.algorithm.JBCIndividual;
import org.jboxcar.util.SimpleChangeListener;
import org.jboxcar.util.Util;

import com.sun.javafx.geom.Point2D;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class JBoxCarGraphicsAPI {
	private int maxFloorTiles = 200;
	private GraphicsContext gc;
	private Random random;
	private ArrayList<Body> floorTiles;
	private double zoom = 70;
	private float groundFloorHeight = 0.15f;
	private float groundFloorWidth = 1.5f;
	private int lastDrawnTile = 0;
	private Point2D camera;
	private float cameraSpeed = 0.05f;
	private long seed;
	private long maximumAllowedFailures = fpsBox2D*4;
	private Car leader;
	private ArrayList<Car> genBest;
	private JBCGeneticAlgorithm ga;
	private SimpleChangeListener<Integer> carsRemaining;
	private SimpleChangeListener<Vec2> leaderPosition;
	private SimpleChangeListener<Integer> generation;
	private double crossoverRate = .7;
	private double probMutation = .01;
	private Canvas canvas;
	private boolean doOutput = false;
	
	// Car specific constants
	float minChassisAxis = .1f;
	

	float maxChassisAxis = 1.3f;
	int minChassisDensity = 30;
	int maxChassisDensity = 300;
	
	float minWheelRadius = 0.2f;
	float maxWheelRadius = 0.5f;
	int minWheelDensity = 40;
	int maxWheelDensity = 100;
	int minWheels = 0;
	int maxWheels = 8;
	
	
	
	// End car specific constants
	
	// algorithm shit
	private int population;
	private Car[] cars;
	
	

	public JBoxCarGraphicsAPI(GraphicsContext gc, int population, long seed) {
		this.seed = seed;
		this.population = population;
		if (gc != null) {
			this.gc = gc;
			this.canvas = gc.getCanvas();
		}
		cars = new Car[population];
		random = new Random(seed);
		floorTiles = new ArrayList<Body>();
		genBest = new ArrayList<Car>();
		camera = new Point2D();
		leaderPosition =new SimpleChangeListener<Vec2>(new Vec2(-1,0));
		carsRemaining = new SimpleChangeListener<Integer>(population);
		generation = new SimpleChangeListener<Integer>(0);
		
		for (int i = 0; i < population; i++) {
			cars[i] = createRandomCar();
		}
		
		this.ga = new JBCGeneticAlgorithm(cars, crossoverRate, probMutation);
	}
	
	public JBoxCarGraphicsAPI(int population, long seed) {
		this(null, population, seed);
	}
	
	public void step() {
		//TODO: have this step based off timestamp
		world.step(1.0f/fpsBox2D, 10, 6);
		for (Car car : cars) {
			if (!car.isAlive()) {
				continue;
			}
			float pos;
			if (!car.update()) {
				carsRemaining.setValue(carsRemaining.getValue()-1);
				// was the leader killed?
				if(leader.equals(car)) {
					leader = findNewLeader();
					if (leader != null) {
						leaderPosition.setValue(leader.getPosition());
					} else {
						// No cars remaining, start new gen
						if (doOutput) {
							outputInfo();
						}
						genBest.add(findBestCar());
						//TODO: tya
						camera.x = camera.y = 0;
						ga.evolvePopulation();
						updatePopulation();
						
					}
				}
			}
			else if ((pos = car.getPosition().x) > leaderPosition.getValue().x){
				Vec2 lp = leaderPosition.getValue();
				leaderPosition.setValue(new Vec2(pos, lp.y));
				leader = car; // Make the leader the car, because it has been working really hard at it's job and it really needs to be shown that it is appreciated.
			}
			
		}
		if (leader == null){
			if (doOutput) {
				outputInfo();
			}
			camera.x = camera.y = 0;
			updatePopulation();
			return;
		} else {
			setCameraPositionRelativeToCar(leader);
		}
	}
	
	public void drawCars() {
		gc.setStroke(Color.web("#444"));
		gc.setLineWidth(1/zoom);
		
		for (Car car : cars) {
			// draw wheels
			for (Wheel wheel : car.getWheels()) {
				for (Fixture fixture = wheel.getFixtureList(); fixture != null; fixture = fixture.m_next) {
					CircleShape shape = (CircleShape) fixture.getShape();
					int colorRequest = Math.round(255 - (255 * (fixture.getDensity() - minWheelDensity)) / maxWheelDensity);
					Color c = Color.rgb(colorRequest, colorRequest, colorRequest);
					drawCircle(wheel.getBody(), shape.m_p, shape.m_radius, wheel.getBody().m_sweep.a, c);
				}
			}
			gc.beginPath();
			Body chassis = car.getChassis();
			
			int i = 0;
			for (Fixture fixture = chassis.getFixtureList(); fixture != null; fixture = fixture.m_next) {
				double num = (((car.getChassisDensities()[i++] - minChassisDensity) / maxChassisDensity) * 360);
				PolygonShape shape  = (PolygonShape)fixture.getShape();
				drawVirtualPolygon(chassis,shape.getVertices(), shape.getVertexCount(), Color.hsb(num, 1.0, .8));
			}
		}
		return;
	}
	
	public void drawScreen() {
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		gc.save();
		gc.translate(200 - (camera.x * zoom), 200 + (camera.y * zoom));
		gc.scale(zoom,  -zoom);
		drawFloor();
		drawCars();
		gc.restore();
	}
	
	public Car createRandomCar() {
		int nWheels = Util.rand(minWheels, maxWheels+1);
		float restitution = 0.2f; // TODO: Should I add this to the GA?
		
		ArrayList<Wheel> wheels = new ArrayList<Wheel>();
		for (int i = 0; i < nWheels; i++) {
			float wheelRadius = Util.rand(minWheelRadius, maxWheelRadius);
			int wheelDensity = Util.rand(minWheelDensity, maxWheelDensity);
			wheels.add(new Wheel(wheelRadius, wheelDensity, restitution));
		}
		int chassisDensity =  Util.rand(minChassisDensity, maxChassisDensity);
		ArrayList<Vec2> chassisVertices = new ArrayList<Vec2>();
		chassisVertices.add(new Vec2(Util.rand(minChassisAxis, maxChassisAxis), 0));
		chassisVertices.add(new Vec2(Util.rand(minChassisAxis, maxChassisAxis), Util.rand(minChassisAxis,  maxChassisAxis)));
		chassisVertices.add(new Vec2(0, Util.rand(minChassisAxis, maxChassisAxis)));
		chassisVertices.add(new Vec2(-Util.rand(minChassisAxis, maxChassisAxis), Util.rand(minChassisAxis, maxChassisAxis)));
		chassisVertices.add(new Vec2(-Util.rand(minChassisAxis, maxChassisAxis), 0));
		chassisVertices.add(new Vec2(-Util.rand(minChassisAxis, maxChassisAxis), -Util.rand(minChassisAxis, maxChassisAxis)));
		chassisVertices.add(new Vec2(0, -Util.rand(minChassisAxis, maxChassisAxis)));
		chassisVertices.add(new Vec2(Util.rand(minChassisAxis, maxChassisAxis), -Util.rand(minChassisAxis, maxChassisAxis)));

		ArrayList<Float> chassisDensities = new ArrayList<Float>(chassisVertices.size());
		for (int i = 0; i < chassisVertices.size(); i++) {
			chassisDensities.add((float)Util.rand(minChassisDensity, maxChassisDensity));
		}

		ArrayList<Integer> wheelVertices = Util.randPerm(0, nWheels);
		  
		return new Car(wheels, wheelVertices, chassisVertices, chassisDensities, maximumAllowedFailures);
	}
	
	
	public void createFloor() {
		Vec2 tilePosition = new Vec2(-5, 0);
		for (int i = 0; i < maxFloorTiles; i++) {
			Body tile = createFloorTile(tilePosition, (random.nextFloat()*3 - 1.5) * 1.5*i/maxFloorTiles);
			floorTiles.add(tile);
			Fixture fixture = tile.getFixtureList();
			Vec2 worldCoord = tile.getWorldPoint(((PolygonShape)fixture.getShape()).m_vertices[3]);
			tilePosition = worldCoord;
		}
	}
	
	public void setGravity(Vec2 gravity) {
		world.setGravity(gravity);
	}
	
	public SimpleChangeListener<Integer> getCarsRemainingListener() {
		return carsRemaining;
	}
	
	public SimpleChangeListener<Vec2> getLeaderPositionListener() {
		return leaderPosition;
	}
	
	public SimpleChangeListener<Integer> getGenerationListener() {
		return generation;
	}
	
	public Car getLeader() {
		return leader;
	}
	
	
	private void drawFloor() {
		gc.setLineWidth(1/zoom);
		gc.setStroke(Color.BLACK);
		gc.setFill(Color.DARKGREEN);
		gc.beginPath();
		outer : for (int i = Math.max(0, lastDrawnTile-20); i < floorTiles.size(); i++) {
			Body body = floorTiles.get(i);
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.m_next) {
				PolygonShape polyShape = (PolygonShape) fixture.getShape();
				float polyShapePosition = body.getWorldPoint(polyShape.m_vertices[0]).x;
				
				if (polyShapePosition > (camera.x - 5) && polyShapePosition < (camera.x + 10)) {
					drawVirtualPolygon(body, polyShape.getVertices(), polyShape.getVertexCount());
				}
				if (polyShapePosition > (camera.x + 10)) {
					lastDrawnTile = i;
					break outer;
				}
			}
		}
		gc.fill();
		gc.stroke();
	}
	
	private void setCameraPositionRelativeToCar(Car car) {
		Point2D diff = new Point2D(camera.x - car.getPosition().x, camera.y - car.getPosition().y);
		camera.y -= cameraSpeed * diff.y;
		camera.x -= cameraSpeed * diff.x;
	}
	
	private void setCameraPositionRelativeToCar(int index) {
		setCameraPositionRelativeToCar(cars[index]);
	}
	
	private Car findNewLeader() {
		int max = -1;
		Car leader = null;
		for (Car car : cars) {
			if (!car.isAlive()) {
				continue;
			}
			
			if (car.getPosition().x > max) {
				leaderPosition.setValue(car.getPosition());
				leader = car;
			}
		}
		return leader;
	}
	
	private Car findBestCar() {
		float max = -1;
		Car ret = null;
		for (Car car : cars) {
			if (car.getPosition().x > max) {
				max = car.getPosition().x;
				ret = car;
			}
		}
		return ret;
	}
	
	private void nextGeneration() {
		
	}
	
	
	private void drawVirtualPolygon(Body body, Vec2 vertices[], int vertexCount) {
		Vec2 p0 = body.getWorldPoint(vertices[0]);
		gc.moveTo(p0.x, p0.y);
		for (int i = 1; i < vertexCount; i++) {
			Vec2 p = body.getWorldPoint(vertices[i]);
			gc.lineTo(p.x, p.y);
		}
		gc.lineTo(p0.x, p0.y);
	}
	
	private void drawVirtualPolygon(Body body, Vec2 vertices[], int vertexCount, Paint p) {
		gc.setStroke(Color.BLACK);
		gc.setFill(p);
		gc.beginPath();
		drawVirtualPolygon(body, vertices, vertexCount);
		gc.fill();
		gc.stroke();
	}
	
	private Body createFloorTile(Vec2 position, double angle) {
		return createFloorTile(position, (float)angle);
	}
	private Body createFloorTile(Vec2 position, float angle) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.setPosition(position);
		Body body = world.createBody(bodyDef);
		
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.setShape(new PolygonShape());
		fixtureDef.setFriction(0.5f);
		
		Vec2 coords[] = new Vec2[4];
		coords[0] = new Vec2(0,0);
		coords[1] = new Vec2(0, -groundFloorHeight);
		coords[2] = new Vec2(groundFloorWidth, -groundFloorHeight);
		coords[3] = new Vec2(groundFloorWidth, 0);
		
		Vec2 center = new Vec2(0,0);
		Vec2 newCoords[] = new Vec2[4];
		newCoords = rotateFloorTile(coords, center, angle);
		
		b2SetAsArray((PolygonShape)fixtureDef.shape, newCoords);
		body.createFixture(fixtureDef);
		return body;
	}
	
	private Vec2[] rotateFloorTile(Vec2[] coords, Vec2 center, float angle) {
		Vec2[] newCoords = new Vec2[coords.length];
		for (int i = 0; i < coords.length; i++) {
			Vec2 coord = new Vec2();
			coord.x = (float) (Math.cos(angle)*(coords[i].x - center.x) - Math.sin(angle)*(coords[i].y - center.y) + center.x);
			coord.y = (float) (Math.sin(angle)*(coords[i].x - center.x) + Math.cos(angle)*(coords[i].y - center.y) + center.y);
			newCoords[i] = coord;
		}
		return newCoords;
	}
	
	private void drawCircle(Body body, Vec2 center, float radius, double angle, Paint p) {
		Vec2 point = body.getWorldPoint(center);
		
		gc.setFill(p);
		gc.beginPath();
		gc.arc(point.x, point.y, radius, radius, 0, 360);
		gc.moveTo(point.x, point.y);
		gc.lineTo(point.x + radius*Math.cos(angle), point.y + radius*Math.sin(angle));
		gc.fill();
		gc.stroke();
	}
	
	private void updatePopulation() {
		leaderPosition.setValue(new Vec2(-1, 0));
		carsRemaining.setValue(population);
		generation.setValue(generation.getValue()+1);
		ArrayList<JBCIndividual> population = ga.getPopulation();
		for (int i = 0; i < cars.length; i++) {
			cars[i] = createRandomCar();
		}
	}
	
	private void outputInfo() {
		double distance = 0.0;
		double max = -1.0;
		double min = Double.MAX_VALUE;
		for (Car car : cars) {
			double pos = car.getPosition().x;
			if (pos > max) {
				max = pos;
			}
			if (pos < min) {
				min = pos;
			}
			distance += pos;
		}
		String out = String.format("\tMin distance: %.2f\n\tMax distance: %.2f\n\tAvg distance: %.2f\n", min, max, distance / cars.length);
		System.out.println(out);
		System.out.flush();
	}
	
	public float getMinChassisAxis() {
		return minChassisAxis;
	}

	public void setMinChassisAxis(float minChassisAxis) {
		this.minChassisAxis = minChassisAxis;
	}

	public float getMaxChassisAxis() {
		return maxChassisAxis;
	}

	public void setMaxChassisAxis(float maxChassisAxis) {
		this.maxChassisAxis = maxChassisAxis;
	}

	public int getMinChassisDensity() {
		return minChassisDensity;
	}

	public void setMinChassisDensity(int minChassisDensity) {
		this.minChassisDensity = minChassisDensity;
	}

	public int getMaxChassisDensity() {
		return maxChassisDensity;
	}

	public void setMaxChassisDensity(int maxChassisDensity) {
		this.maxChassisDensity = maxChassisDensity;
	}

	public float getMinWheelRadius() {
		return minWheelRadius;
	}

	public void setMinWheelRadius(float minWheelRadius) {
		this.minWheelRadius = minWheelRadius;
	}

	public float getMaxWheelRadius() {
		return maxWheelRadius;
	}

	public void setMaxWheelRadius(float maxWheelRadius) {
		this.maxWheelRadius = maxWheelRadius;
	}

	public int getMinWheelDensity() {
		return minWheelDensity;
	}

	public void setMinWheelDensity(int minWheelDensity) {
		this.minWheelDensity = minWheelDensity;
	}

	public int getMaxWheelDensity() {
		return maxWheelDensity;
	}

	public void setMaxWheelDensity(int maxWheelDensity) {
		this.maxWheelDensity = maxWheelDensity;
	}

	public int getMinWheels() {
		return minWheels;
	}

	public void setMinWheels(int minWheels) {
		this.minWheels = minWheels;
	}

	public int getMaxWheels() {
		return maxWheels;
	}

	public void setMaxWheels(int maxWheels) {
		this.maxWheels = maxWheels;
	}
	
	public void setDoOutput(boolean doOutput) {
		this.doOutput = doOutput;
	}
}
