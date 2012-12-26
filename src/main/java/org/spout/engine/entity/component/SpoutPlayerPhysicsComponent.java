package org.spout.engine.entity.component;

import javax.vecmath.Matrix4f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.dynamics.character.KinematicCharacterController;
import com.bulletphysics.linearmath.Transform;

import org.spout.api.component.impl.PlayerPhysicsComponent;
import org.spout.api.entity.Player;
import org.spout.api.geo.discrete.Point;
import org.spout.api.math.MathHelper;
import org.spout.api.math.Vector3;

import org.spout.engine.entity.SpoutPlayer;
import org.spout.engine.world.SpoutRegion;

public class SpoutPlayerPhysicsComponent extends PlayerPhysicsComponent {
	private final PairCachingGhostObject object;
	private KinematicCharacterController controller;
	private SpoutPlayer player;

	public SpoutPlayerPhysicsComponent() {
		object = new PairCachingGhostObject();
	}

	@Override
	public void onAttached() {
		if (!(getOwner() instanceof Player)) {
			throw new IllegalStateException("Cannot attach Player physics to a non player!");
		}
		player = (SpoutPlayer) getOwner();
		//Set the object's starting transform to the transform of the player
		org.spout.api.geo.discrete.Transform spoutTransform = player.getTransform().getTransformLive();
		Point point = spoutTransform.getPosition();
		Transform bulletTransform = new Transform();
		bulletTransform.set(new Matrix4f(MathHelper.toQuaternionf(spoutTransform.getRotation()), MathHelper.toVector3f(point.getX(), point.getY(), point.getZ()), 1));
		object.setWorldTransform(bulletTransform);
		//Set the player's shape
		float height = spoutTransform.getScale().getY() * 1.2f; //TODO API this
		float width = spoutTransform.getScale().getZ() * 1.2f; //TODO API this
		final ConvexShape capsule = new CapsuleShape(width, height);
		object.setCollisionShape(capsule);
		object.setCollisionFlags(CollisionFlags.CHARACTER_OBJECT);
		//Set characteristics
		float stepHeight = 0.2f * height;
		controller = new KinematicCharacterController(object, capsule, stepHeight);
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
	public void setAngularVelocity(Vector3 velocity) {
		SpoutRegion r = (SpoutRegion) getOwner().getRegion();
		if (r == null) {
			throw new IllegalStateException("Player region is null!");
		}
		synchronized (r.getSimulation()) {
			object.setInterpolationAngularVelocity(MathHelper.toVector3f(velocity));
		}
	}

	@Override
	public void setLinearVelocity(Vector3 velocity) {
		SpoutRegion r = (SpoutRegion) getOwner().getRegion();
		if (r == null) {
			throw new IllegalStateException("Player region is null!");
		}
		synchronized (r.getSimulation()) {
			object.setInterpolationLinearVelocity(MathHelper.toVector3f(velocity));
		}
	}

	@Override
	public void teleport(Point loc) {
		controller.warp(MathHelper.toVector3f(loc));
		player.getNetworkSynchronizer().setPositionDirty();
	}

	@Override
	public void teleport(org.spout.api.geo.discrete.Transform transform) {
		controller.warp(MathHelper.toVector3f(transform.getPosition()));
		player.getNetworkSynchronizer().setPositionDirty();
	}

	/**
	 * Gets the collision object which holds the collision shape and is used to calculate physics such as velocity, intertia,
	 * etc. All PhysicsComponents are guaranteed to have a valid object.
	 * @return the CollisionObject
	 */
	public CollisionObject getCollisionObject() {
		return object;
	}

	public KinematicCharacterController getController() {
		return controller;
	}
}
