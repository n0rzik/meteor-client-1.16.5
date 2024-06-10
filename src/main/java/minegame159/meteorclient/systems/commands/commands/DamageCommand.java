/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.movement.NoFall;
import minegame159.meteorclient.systems.modules.player.AntiHunger;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DamageCommand extends Command {
    private final static SimpleCommandExceptionType INVULNERABLE = new SimpleCommandExceptionType(new LiteralText("You are invulnerable."));
    
    public DamageCommand() {
        super("damage", "Damages self", "dmg");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("damage", IntegerArgumentType.integer(1, 7)).executes(context -> {
            int amount = IntegerArgumentType.getInteger(context, "damage");

            if (mc.player.abilities.invulnerable) {
                throw INVULNERABLE.create();
            }

            damagePlayer(amount);
            return SINGLE_SUCCESS;
        }));

    }
    
    private void damagePlayer(int amount) {
        boolean noFall = Modules.get().isActive(NoFall.class);
        if (noFall) Modules.get().get(NoFall.class).toggle();

        boolean antiHunger = Modules.get().isActive(AntiHunger.class);
        if (antiHunger) Modules.get().get(AntiHunger.class).toggle();

        Vec3d pos = mc.player.getPos();

        for(int i = 0; i < 80; i++) {
            sendPosistionPacket(pos.x, pos.y + amount + 2.1, pos.z, false);
            sendPosistionPacket(pos.x, pos.y + 0.05, pos.z, false);
        }
        
        sendPosistionPacket(pos.x, pos.y, pos.z, true);

        if (noFall) Modules.get().get(NoFall.class).toggle();
        if (antiHunger) Modules.get().get(AntiHunger.class).toggle();
    }

    private void sendPosistionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, onGround));
    }
}
