/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement.elytrafly;

import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.player.FindItemResult;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ElytraFlightMode {
    protected final MinecraftClient mc;
    protected final ElytraFly settings;
    private final ElytraFlightModes type;

    protected boolean lastJumpPressed;
    protected boolean incrementJumpTimer;
    protected boolean lastForwardPressed;
    protected int jumpTimer;
    protected double velX, velY, velZ;
    protected double ticksLeft;
    protected Vec3d forward, right;

    public ElytraFlightMode(ElytraFlightModes type) {
        this.settings = Modules.get().get(ElytraFly.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick() {
        if (settings.replace.get()) {
            ItemStack chestStack = mc.player.inventory.getArmorStack(2);

            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= settings.replaceDurability.get()) {
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > settings.replaceDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.getSlot()).toArmor(2);
                }
            }
        }
    }

    public void onPacketSend(PacketEvent.Send event) {}

    public void onPlayerMove() {}

    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
        ticksLeft = 0;
    }

    public void onDeactivate() {}

    public void autoTakeoff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.keyJump.isPressed();

        if (settings.autoTakeOff.get() && jumpPressed) {
            if (!lastJumpPressed && !mc.player.isFallFlying()) {
                jumpTimer = 0;
                incrementJumpTimer = true;
            }

            if (jumpTimer >= 8) {
                jumpTimer = 0;
                incrementJumpTimer = false;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.player.jump();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        lastJumpPressed = jumpPressed;
    }

    public void handleAutopilot() {
        if (!mc.player.isFallFlying()) return;

        if (settings.moveForward.get() && mc.player.getY() > settings.autoPilotMinimumHeight.get()) {
            mc.options.keyForward.setPressed(true);
            lastForwardPressed = true;
        }

        if (settings.useFireworks.get()) {
            if (ticksLeft <= 0) {
                ticksLeft = settings.autoPilotFireworkDelay.get() * 20;

                FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
                if (!itemResult.found()) return;

                if (itemResult.isOffhand()) {
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
                    mc.player.swingHand(Hand.OFF_HAND);
                } else {
                    int prevSlot = mc.player.inventory.selectedSlot;
                    InvUtils.swap(itemResult.getSlot());

                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);

                    InvUtils.swap(prevSlot);
                }
            }
            ticksLeft--;
        }
    }

    public void handleHorizontalSpeed() {
        boolean a = false;
        boolean b = false;

        if (mc.options.keyForward.isPressed()) {
            velX += forward.x * settings.horizontalSpeed.get() * 10;
            velZ += forward.z * settings.horizontalSpeed.get() * 10;
            a = true;
        } else if (mc.options.keyBack.isPressed()) {
            velX -= forward.x * settings.horizontalSpeed.get() * 10;
            velZ -= forward.z * settings.horizontalSpeed.get() * 10;
            a = true;
        }

        if (mc.options.keyRight.isPressed()) {
            velX += right.x * settings.horizontalSpeed.get() * 10;
            velZ += right.z * settings.horizontalSpeed.get() * 10;
            b = true;
        } else if (mc.options.keyLeft.isPressed()) {
            velX -= right.x * settings.horizontalSpeed.get() * 10;
            velZ -= right.z * settings.horizontalSpeed.get() * 10;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }
    }

    public void handleVerticalSpeed() {
        if (mc.options.keyJump.isPressed()) velY += 0.5 * settings.verticalSpeed.get();
        else if (mc.options.keySneak.isPressed()) velY -= 0.5 * settings.verticalSpeed.get();
    }

    public void handleFallMultiplier() {
        if (velY < 0) velY *= settings.fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    public String getHudString() {
        return type.name();
    }
}
