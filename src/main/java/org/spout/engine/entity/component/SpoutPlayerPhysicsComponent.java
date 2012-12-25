package org.spout.engine.entity.component;

import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.dynamics.character.KinematicCharacterController;

import org.spout.api.component.impl.PlayerPhysicsComponent;
import org.spout.api.entity.Player;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.math.MathHelper;

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
	public void teleport(Point loc) {
		controller.warp(MathHelper.toVector3f(loc));
		player.getNetworkSynchronizer().setPositionDirty();
	}

	@Override
	public void teleport(Transform transform) {
		controller.warp(MathHelper.toVector3f(transform.getPosition()));
		player.getNetworkSynchronizer().setPositionDirty();
	}
}
