package org.jboxcar.api;

import static org.jboxcar.util.Globals.gravity;
import static org.jboxcar.util.Globals.motorSpeed;
import static org.jboxcar.util.Globals.world;

import java.util.ArrayList;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jboxcar.util.Util;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class Car {
	private int nWheels;
	private boolean isElite;
	private boolean isAlive;
	private final ArrayList<Vec2> chassisVertices;
	private ArrayList<Shock> shocks;
	private float[] chassisDensities;
	private Wheel[] wheels;
	private int[] wheelVertices;
	private float velocity;
	private float maxPosition;
	private Body chassis;
	private float mass;
	private float[] torque;
	private long frames;
	private long timesFailed;
	private long maxAllowedFails;
	private boolean usingShocks;
	public ArrayList<Paint> paints;
	
	/**
	 * 
	 * @param wheels:
	 * 		An array list of Wheels
	 * @param wheelVertices
	 * 		An array list of integers containing which vertices to add the wheels to. 
	 * @param chassisVertices
	 * 		An array list of Vec2 containing the setup of the chassis
	 * @param chassisDensities
	 * 		An array list of floats to set each of the chassisVertices to
	 * @param maxAllowedFails
	 * 		The maximum number of failures a car can experience before extinction
	 */
	public Car(ArrayList<Wheel> wheels, ArrayList<Integer> wheelVertices, ArrayList<Vec2> chassisVertices, ArrayList<Float> chassisDensities, long maxAllowedFails) {
		paints = new ArrayList<Paint>();
		for (int i = 0; i < 8; i++) {
			paints.add(Color.rgb(Util.rand(0, 256), Util.rand(0, 256), Util.rand(0, 256)));
		}
		if (chassisVertices.size() != chassisDensities.size()) {
			throw new IllegalArgumentException(String.format("chassisVertices (size %d) must be the same size as chassisDensities (size %d). Each density corresponds to each vertex", chassisVertices.size(), chassisDensities.size()));
		}
		nWheels = wheels.size();
		torque = new float[nWheels];
		this.wheelVertices = new int[nWheels];
		
		this.wheels = new Wheel[nWheels];
		this.wheels = wheels.toArray(this.wheels);
		
		this.chassisDensities = new float[chassisDensities.size()];
		for (int i = 0; i < chassisDensities.size(); i++)  {
			this.chassisDensities[i] = (chassisDensities.get(i) == null ? 0 : chassisDensities.get(i));
		}
		
		this.maxAllowedFails = maxAllowedFails;
		velocity = 0.0f;
		maxPosition = 0.0f;
		frames = 0;
		isAlive = true;
		timesFailed = 0;
		
		
		for (int i = 0; i < wheelVertices.size(); i++) {
			this.wheelVertices[i] = wheelVertices.get(i);
		}
		
		// create the chassis
		this.chassisVertices = new ArrayList<Vec2>(chassisVertices);
		chassis = createChassis(this.chassisDensities);
		
		// find the car mass
		mass = chassis.getMass();
		for (Wheel wheel : this.wheels) {
			mass += wheel.getMass();
		}
		
		// compute torque for each wheel
		for (Wheel wheel : this.wheels) {
			float torque = (mass * Math.abs(gravity.y)) / wheel.getRadius();
			wheel.setTorque(torque);
		}
		
		/*
		 * There are n wheels and m vertices. This portion creates a random permutation
		 * between [0,m) and assigns some of those vertices a wheel.
		 * 
		 *  @NOTE:
		 *  	If n > m this will cause an error as then there will be more wheels than vertices
		 */
		RevoluteJointDef revoluteJointDef = new RevoluteJointDef();
		for (int i = 0; i < nWheels; i++) {
			Vec2 chassisVertex = this.chassisVertices.get(this.wheelVertices[i]); 
			revoluteJointDef.localAnchorA.set(chassisVertex);
			revoluteJointDef.localAnchorB.set(0,0);
			revoluteJointDef.maxMotorTorque = this.wheels[i].getTorque();
			revoluteJointDef.motorSpeed = -motorSpeed;
			revoluteJointDef.enableMotor = true;
			revoluteJointDef.bodyA = chassis;
			revoluteJointDef.bodyB = this.wheels[i].getBody();
			world.createJoint(revoluteJointDef);
		}
	}
	
	public boolean update() {
		frames++;
		float position = getPosition().x;
		if (position > maxPosition) {
			timesFailed = 0;
			maxPosition = position;
		} else if (position <= maxPosition || chassis.getLinearVelocity().x < .001){
			timesFailed++;
			if (timesFailed > maxAllowedFails) {
				isAlive = false;
			}
		}
		
		if (!isAlive()) {
			destroy();
		}
		return isAlive();
	}
	
	
	public Vec2 getPosition() {
		return chassis.getPosition();
	}
		
	public int getnWheels() {
		return nWheels;
	}
	
	public Wheel[] getWheels() {
		return wheels;
	}
	
	
	public boolean isAlive() {
		return isAlive;
	}
	
	public float[] getChassisDensities() {
		return chassisDensities;
	}
	
	public Body getChassis() {
		return chassis;
	}
	
	public ArrayList<Vec2> getChassisVertices() {
		return chassisVertices;
	}
	
	public void setShocks(ArrayList<Shock> shocks, ArrayList<Vec2> vertices) {
		
	}
	private void destroy() {
		world.destroyBody(chassis);
		for (Wheel wheel : wheels) {
			world.destroyBody(wheel.getBody());
		}
	}
	
	
	private Body createChassis(float[] chassisDensities) {
		return createChassis(this.chassisVertices, chassisDensities);
	}
	
	private Body createChassis(ArrayList<Vec2> vertices, float[] chassisDensities) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.setType(BodyType.DYNAMIC);
		bodyDef.setPosition(new Vec2(0.0f, 4.0f));
		
		Body body = world.createBody(bodyDef);
		for (int i = 0; i < vertices.size(); i++) {
			int endIdx = i+1;
			if (i == vertices.size()-1) {
				endIdx = 0;
			}
			createChassisPart(body, vertices.get(i), vertices.get(endIdx), chassisDensities[i]);
			
		}
		
		return body;
	}
	
	private void createChassisPart(Body body, Vec2 v1, Vec2 v2, float density) {
		Vec2[] vertices = new Vec2[3];
		vertices[0] = v1;
		vertices[1] = v2;
		vertices[2] = new Vec2(0,0);
		
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.setShape(new PolygonShape());
		fixtureDef.setDensity(density);
		fixtureDef.setFriction(10);
		fixtureDef.setRestitution(0.2f);
		fixtureDef.getFilter().groupIndex = -1;
		Util.b2SetAsArray((PolygonShape)fixtureDef.getShape(), vertices, 3);
		
		body.createFixture(fixtureDef);
	}
	
}

