package org.spout.engine.entity.component;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.dynamics.character.KinematicCharacterController;

import org.spout.api.math.MathHelper;
import org.spout.api.math.Vector3;

import org.spout.engine.entity.SpoutPlayer;
import org.spout.engine.world.SpoutRegion;

public class SpoutPlayerPhysicsComponent extends SpoutPhysicsComponent {
	private final PairCachingGhostObject object;
	private KinematicCharacterController controller;
	private Vector3 angularVelocity = Vector3.ZERO;
	private Vector3 linearVelocity = Vector3.ZERO;
	private boolean dirty = false;

	public SpoutPlayerPhysicsComponent() {
		object = new PairCachingGhostObject();
	}

	@Override
	public void onAttached() {
		if (!(getOwner() instanceof SpoutPlayer)) {
			throw new IllegalStateException("This component may only be attached to the client player.");
		}
	}

	@Override
	public boolean isDetachable() {
		return false;
	}

	@Override
	public float getRestitution() {
		synchronized (((SpoutRegion) getOwner().getRegion()).getSimulation()) {
			return object.getRestitution();
		}
	}

	@Override
	public void setRestitution(float restitution) {
		synchronized (((SpoutRegion) getOwner().getRegion()).getSimulation()) {
			object.setRestitution(restitution);
		}
	}

	@Override
	public float getAngularDamping() {
		throw new UnsupportedOperationException("Players do not have damping");
	}

	@Override
	public float getLinearDamping() {
		throw new UnsupportedOperationException("Players do not have damping");
	}

	@Override
	public void setDamping(float linearDamping, float angularDamping) {
		throw new UnsupportedOperationException("Players do not have damping");
	}

	@Override
	public float getFriction() {
		synchronized (((SpoutRegion) getOwner().getRegion()).getSimulation()) {
			return object.getFriction();
		}
	}

	@Override
	public void setFriction(float friction) {
		synchronized (((SpoutRegion) getOwner().getRegion()).getSimulation()) {
			object.setFriction(friction);
		}
	}

	@Override
	public float getMass() {
		throw new UnsupportedOperationException("Players do not have mass");
	}

	@Override
	public void setMass(float mass) {
		throw new UnsupportedOperationException("Players do not have mass");
	}

	@Override
	public ConvexShape getCollisionShape() {
		return (ConvexShape) object.getCollisionShape();
	}

	@Override
	public void setCollisionShape(CollisionShape shape) {
		throw new UnsupportedOperationException("Setting new shapes for Player controllers are not allowed.");
	}

	@Override
	public Vector3 getAngularVelocity() {
		return MathHelper.toVector3(object.getInterpolationAngularVelocity(new Vector3f()));
	}

	@Override
	public Vector3 getLinearVelocity() {
		return MathHelper.toVector3(object.getInterpolationLinearVelocity(new Vector3f()));
	}

	@Override
	public boolean isVelocityDirty() {
		return dirty;
	}

	@Override
	public void applyImpulse(Vector3 impulse) {
		throw new UnsupportedOperationException("Cannot impulse Player character controllers");
	}

	@Override
	public void applyImpulse(Vector3 impulse, Vector3 relativePos) {
		throw new UnsupportedOperationException("Cannot impulse Player character controllers");
	}

	@Override
	public void applyForce(Vector3 impulse) {
		throw new UnsupportedOperationException("Cannot force Player character controllers");
	}

	@Override
	public void applyForce(Vector3 force, Vector3 relativePos) {
		throw new UnsupportedOperationException("Cannot force Player character controllers");
	}

	@Override
	public PairCachingGhostObject getCollisionObject() {
		return object;
	}

	@Override
	public void copySnapshot() {
		SpoutRegion r = (SpoutRegion) getOwner().getRegion();
		if (r == null) {
			throw new IllegalStateException("Player region is null!");
		}
		synchronized (r.getSimulation()) {
			Vector3 angularVelocityLive = getAngularVelocity();
			Vector3 linearVelocityLive = getLinearVelocity();
			dirty = !linearVelocityLive.equals(linearVelocity) || angularVelocityLive.equals(angularVelocity);
			angularVelocity = angularVelocityLive;
			linearVelocity = linearVelocityLive;
		}
	}
}
