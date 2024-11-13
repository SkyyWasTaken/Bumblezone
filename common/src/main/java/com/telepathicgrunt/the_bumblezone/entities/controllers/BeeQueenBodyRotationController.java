package com.telepathicgrunt.the_bumblezone.entities.controllers;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

import java.util.Optional;

public class BeeQueenBodyRotationController extends BodyRotationControl {
    private final Mob mob;

    public BeeQueenBodyRotationController(Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public void clientTick() {
        this.mob.yBodyRot = this.mob.yHeadRot;
    }
}
