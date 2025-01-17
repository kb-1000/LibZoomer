package io.github.ennuil.libzoomer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.ennuil.libzoomer.api.ZoomInstance;
import io.github.ennuil.libzoomer.api.ZoomOverlay;
import io.github.ennuil.libzoomer.api.ZoomRegistry;
import io.github.ennuil.libzoomer.impl.OverlayCancellingHelper;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Final
    @Shadow
    private MinecraftClient client;
    
    @Unique
    private double formerFov;

    @Inject(
        at = @At("HEAD"),
        method = "tick()V"
    )
    private void tickInstances(CallbackInfo info) {
        for (ZoomInstance instance : ZoomRegistry.getZoomInstances()) {
            boolean zoom = instance.getZoom();
            if (zoom || (instance.isTransitionActive() || instance.isOverlayActive())) {
                double divisor = 1.0F;
                if (zoom) {
                    divisor = instance.getZoomDivisor();
                }
    
                instance.getZoomOverlay().tick(instance.getZoom(), instance.getZoomDivisor(), instance.getTransitionMode().getInternalMultiplier());
                instance.getTransitionMode().tick(zoom, divisor);
            }
        }
    }

    @Inject(
        at = @At("RETURN"),
        method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
        cancellable = true
    )
    private double getZoomedFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double fov = cir.getReturnValue();
        boolean zoomedIn = false;
        double zoomedFov = fov;
        
        for (ZoomInstance instance : ZoomRegistry.getZoomInstances()) {
            if (instance.isTransitionActive()) {
                double divisor = 1.0F;
                if (instance.getZoom()) {
                    divisor = instance.getZoomDivisor();
                }
                zoomedIn = true;
                zoomedFov = instance.getTransitionMode().applyZoom(zoomedFov, divisor, tickDelta);   
            }
        }

        if (zoomedIn) {
            cir.setReturnValue(zoomedFov);
        }

        if (zoomedFov > formerFov) {
            if (changingFov) {
                this.client.worldRenderer.scheduleTerrainUpdate();
            }
        }

        this.formerFov = zoomedFov;

        return fov;
    }

    @Inject(
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z"),
        method = "render(FJZ)V"
    )
    public void injectZoomOverlay(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        if (this.client.options.hudHidden) return;
        
        RenderSystem.enableBlend();
        OverlayCancellingHelper.setCancelOverlayRender(false);
        for (ZoomInstance instance : ZoomRegistry.getZoomInstances()) {
            ZoomOverlay overlay = instance.getZoomOverlay();
            overlay.tickBeforeRender();
            if (overlay.getActive()) {
                if (overlay.cancelOverlayRendering()) {
                    OverlayCancellingHelper.setCancelOverlayRender(true);
                }
                
                overlay.renderOverlay();	
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
    }
}
