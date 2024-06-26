/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.events.entity.EntityRemovedEvent;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.NoRender;
import minegame159.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.render.SkyProperties;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Unique
    private final SkyProperties endSky = new SkyProperties.End();
    @Unique
    private final SkyProperties customSky = new Ambience.Custom();

    @Inject(method = "addEntityPrivate", at = @At("TAIL"))
    private void onAddEntityPrivate(int id, Entity entity, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EntityAddedEvent.get(entity));
    }

    @Inject(method = "finishRemovingEntity", at = @At("TAIL"))
    private void onFinishRemovingEntity(Entity entity, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EntityRemovedEvent.get(entity));
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "method_23777", at = @At("HEAD"), cancellable = true)
    private void onGetSkyColor(BlockPos blockPos, float tickDelta, CallbackInfoReturnable<Vec3d> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.changeSkyColor.get()) {
            info.setReturnValue(ambience.skyColor.get().getVec3d());
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getSkyProperties", at = @At("HEAD"), cancellable = true)
    private void onGetSkyProperties(CallbackInfoReturnable<SkyProperties> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.enderMode.get()) {
            info.setReturnValue(ambience.enderCustomSkyColor.get() ? customSky : endSky);
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
    private void onGetCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3d> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.changeCloudColor.get()) {
            info.setReturnValue(ambience.cloudColor.get().getVec3d());
        }
    }

    @ModifyArgs(method = "doRandomBlockDisplayTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;randomBlockDisplayTick(IIIILjava/util/Random;ZLnet/minecraft/util/math/BlockPos$Mutable;)V"))
    private void doRandomBlockDisplayTicks(Args args) {
        if (Modules.get().get(NoRender.class).noBarrierInvis()) args.set(5 , true);
    }
}
