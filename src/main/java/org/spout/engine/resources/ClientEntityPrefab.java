/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011-2012, Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.engine.resources;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.bulletphysics.collision.shapes.CollisionShape;

import org.apache.commons.lang3.ArrayUtils;

import org.spout.api.component.implementation.ModelComponent;
import org.spout.api.component.implementation.PhysicsComponent;
import org.spout.api.component.type.EntityComponent;
import org.spout.api.entity.Entity;
import org.spout.api.entity.EntityPrefab;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.math.Quaternion;
import org.spout.api.math.Vector3;
import org.spout.api.resource.Resource;

import org.spout.engine.entity.SpoutEntity;
import org.spout.engine.entity.component.SpoutPhysicsComponent;

public class ClientEntityPrefab extends Resource implements EntityPrefab {
	private String name;
	private List<Class<? extends EntityComponent>> components = new ArrayList<Class<? extends EntityComponent>>();
	private Map<String, Object> datas = new HashMap<String, Object>();
	private Map<String, Float> collisionDatas = new HashMap<String, Float>();

	public ClientEntityPrefab(String name, List<Class<? extends EntityComponent>> components, Map<String, Object> datas, Map<String, Float> collisionDatas) {
		this.name = name;
		this.components.addAll(components);
		this.datas = datas;
		this.collisionDatas = collisionDatas;
	}

	public String getName() {
		return name;
	}

	public List<Class<? extends EntityComponent>> getComponents() {
		return components;
	}

	public Map<String, Object> getDatas() {
		return datas;
	}

	public Entity createEntity(Point point) {
		return createEntity(new Transform(point, Quaternion.IDENTITY, Vector3.ONE));
	}

	public Entity createEntity(Transform transform) {
		SpoutEntity entity = new SpoutEntity(transform);
		for (Class<? extends EntityComponent> c : components) {
			entity.add(c);
		}

		if (datas.containsKey("Model")) {
			ModelComponent mc = entity.get(ModelComponent.class);
			if (mc == null) {
				mc = entity.add(ModelComponent.class);
			}

			mc.setModel((String) datas.get("Model"));
		}


		Class collisionClazz = (Class) datas.get("Shape");
		CollisionShape shape;
		//Start reading in collision data
		if (collisionClazz != null && collisionDatas != null) {
			//Add/Get physics component
			SpoutPhysicsComponent physics = (SpoutPhysicsComponent) entity.add(PhysicsComponent.class);

			//Form a map of Bounds#, value
			HashMap<String, Float> boundsMap = new HashMap<String, Float>();
			for (Map.Entry entry : collisionDatas.entrySet()) {
				if (entry.getKey().toString().contains("Bounds")) {
					boundsMap.put(entry.getKey().toString(), (Float) entry.getValue());
				}
			}

			//Sort the values into a linked list
			LinkedList<Float> bounds = new LinkedList<Float>();
			for (int i = 0; i < boundsMap.size(); i++) {
				bounds.add(boundsMap.get("Bounds" + (i + 1))); //Bounds start at 1
			}

			//Decipher shape constructors
			Constructor[] constructors = collisionClazz.getConstructors();
			Constructor found = null;
			for (Constructor constructor : constructors) {
				Class[] types = constructor.getParameterTypes();
				boolean isValid = true;
				if (types.length != bounds.size()) {
					continue;
				}
				for (Class clazz : types) {
					if (!clazz.equals(Float.class)) {
						isValid = false;
					}
				}
				if (isValid) {
					found = constructor;
					break;
				}
			}

			if (found == null) {
				throw new IllegalStateException("Bounds: " + bounds.toString() + " for class: " + collisionClazz.getName() + " in EntityPrefab: " + getName() + " is invalid!");
			}
			try {
				shape = (CollisionShape) found.newInstance(bounds.toArray(new Float[bounds.size()]));
			} catch (Exception e) {
				throw new IllegalStateException("Could not create: " + collisionClazz.getName() + " from Bounds: " + bounds.toString() + " in EntityPrefab: " + getName());
			}

			/*
			 * ORDER MATTERS
			 */

			//Handle Mass
			if (collisionDatas.containsKey("Mass")) {
				physics.setMass(collisionDatas.get("Mass"));
			}

			//Add the shape
			physics.setCollisionShape(shape);

			//Handle Damping
			Float angDamping = collisionDatas.get("AngularDamping");
			Float linDamping = collisionDatas.get("LinearDamping");
			if (angDamping != null && linDamping != null) {
				physics.setDamping(linDamping, angDamping);
			} else if (angDamping == null && linDamping != null) {
				physics.setDamping(linDamping, 0f);
			} else if (angDamping != null && linDamping == null) {
				physics.setDamping(0f, angDamping);
			}

			//Handle Friction
			if (collisionDatas.containsKey("Friction")) {
				physics.setFriction(collisionDatas.get("Friction"));
			}

			//Handle Restitution
			if (collisionDatas.containsKey("Restitution")) {
				physics.setMass(collisionDatas.get("Restitution"));
			}
		}
		return entity;
	}
}
