package org.jboxcar.api;

import static org.jboxcar.util.Globals.world;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;

public class Shock {
	private Body chassis;
	private float hx;
	private float hy;
	private float angle;
	private Vec2 center;
	private PolygonShape axleContainerShape;
	private PolygonShape axleShape;
	private FixtureDef axleContainerFixtureDef;
	private FixtureDef axleFixtureDef;
	private Body body;
	
	public Shock(Body chassis, float hx, float hy, Vec2 center, float angle) {
		this.chassis = chassis;
		this.hx = hx;
		this.hy = hy;
		this.center = center;
		this.angle = angle;
		createAxleContainer(3, 3, .3f);
		body = createAxle(.5f, 3, 0);
	}
	
	private void createAxleContainer(float density, float friction, float restitution) {
		axleContainerShape = new PolygonShape();
		axleContainerShape.setAsBox(hx, hy, center, (float)Math.toRadians(angle));
		axleContainerFixtureDef = new FixtureDef();
		axleContainerFixtureDef.setDensity(density);
		axleContainerFixtureDef.setFriction(friction);
		axleContainerFixtureDef.getFilter().groupIndex = -1;
		axleContainerFixtureDef.setShape(axleContainerShape);
		chassis.createFixture(axleContainerFixtureDef);
	}
	private Body createAxle(float density, float friction, float restitution) {
		axleShape = new PolygonShape();
		axleShape.setAsBox(hx/2, hy, new Vec2(0,0), (float)Math.toRadians(angle));
		axleFixtureDef = new FixtureDef();
		axleFixtureDef.setDensity(density);
		axleFixtureDef.setFriction(friction);
		axleFixtureDef.getFilter().groupIndex = -1;
		axleFixtureDef.setShape(axleShape);
		BodyDef bodyDef = new BodyDef();
		bodyDef.setType(BodyType.DYNAMIC);
		Body body = world.createBody(bodyDef);
		body.createFixture(axleFixtureDef);
		return body;
	}
}
