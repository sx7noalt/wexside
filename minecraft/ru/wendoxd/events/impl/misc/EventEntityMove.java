package ru.wendoxd.events.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.wendoxd.events.Event;

public class EventEntityMove extends Event {
	private Entity ctx;
	private Vec3d from;

	public EventEntityMove(Entity ctx, Vec3d from) {
		this.ctx = ctx;
		this.from = from;
	}

	public Vec3d from() {
		return this.from;
	}

	public Entity ctx() {
		return this.ctx;
	}
}
