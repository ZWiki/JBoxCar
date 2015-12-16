package org.jboxcar.api;

import static org.jboxcar.util.Globals.world;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;

import com.sun.corba.se.impl.orbutil.DenseIntMapImpl;

public class Wheel{
	private BodyDef bodyDef;
	private Body body;
	private FixtureDef fixtureDef;
	private CircleShape circleShape;
	private float radius;
	private float density;
	private float restitution;
	private float torque;
	
	public Wheel(float radius, float density, float restitution) {
		this.radius = radius;
		this.density = density;
		this.restitution = restitution;
		
		bodyDef = new BodyDef();
		bodyDef.setType(BodyType.DYNAMIC);
		bodyDef.setPosition(new Vec2(0,0));
		
		body = world.createBody(bodyDef);
		fixtureDef = new FixtureDef();
		circleShape = new CircleShape();
		
		circleShape.setRadius(radius);
		fixtureDef.setShape(circleShape);
		fixtureDef.setDensity(density);
		fixtureDef.setFriction(1);
		fixtureDef.setRestitution(restitution);
		fixtureDef.getFilter().groupIndex = -1;
		
		body.createFixture(fixtureDef);
	}
	
	public float getMass() {
		return body.getMass();
	}
	
	public float getRadius() {
		return radius;
	}
	
	public float getDensity() {
		return density;
	}
	
	public float getRestitution() {
		return restitution;
	}
	
	public float getTorque() {
		return torque;
	}
	
	public void setTorque(float torque) {
		this.torque = torque;
	}
	
	public Body getBody() {
		return body;
	}
	
	public Fixture getFixtureList() {
		return body.getFixtureList();
	}
	

}
