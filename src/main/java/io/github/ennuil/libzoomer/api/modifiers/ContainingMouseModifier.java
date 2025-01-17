package io.github.ennuil.libzoomer.api.modifiers;

import io.github.ennuil.libzoomer.api.MouseModifier;
import net.minecraft.util.Identifier;

//A sin was probably committed by using a lot of for each loops
//TODO - Consider if Ok Zoomer or LibZoomer is a better place for this
/**
 * A mouse modifier that contains multiple mouse modifiers.
 */
public class ContainingMouseModifier implements MouseModifier {
    private Identifier transitionId = new Identifier("libzoomer:modifier_container");
    private boolean active;
    private MouseModifier[] modifiers;

    public ContainingMouseModifier(MouseModifier[] modifiers) {
        this.active = false;
        this.modifiers = modifiers;
    }

    @Override
    public Identifier getIdentifier() {
        return this.transitionId;
    }

    @Override
    public boolean getActive() {
        return this.active;
    }

    @Override
    public double applyXModifier(double cursorXDelta, double o, double mouseUpdateDelta, double targetDivisor, double transitionMultiplier) {
        double returnedValue = o;
        for (MouseModifier modifier : modifiers) {
            returnedValue = modifier.applyXModifier(cursorXDelta, returnedValue, mouseUpdateDelta, targetDivisor, transitionMultiplier);
        }
        return returnedValue;
    }

    @Override
    public double applyYModifier(double cursorYDelta, double p, double mouseUpdateDelta, double targetDivisor, double transitionMultiplier) {
        double returnedValue = p;
        for (MouseModifier modifier : modifiers) {
            returnedValue = modifier.applyYModifier(cursorYDelta, returnedValue, mouseUpdateDelta, targetDivisor, transitionMultiplier);
        }
        return returnedValue;
    }

    @Override
    public void tick(boolean active, boolean cinematicCameraEnabled) {
        boolean generalActive = false;
        for (MouseModifier modifier : modifiers) {
            modifier.tick(active, cinematicCameraEnabled);
            if (!generalActive) {
                generalActive = modifier.getActive();
            }
        }
        this.active = generalActive;
    }
}
