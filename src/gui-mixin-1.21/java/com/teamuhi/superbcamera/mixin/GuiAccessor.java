package com.teamuhi.superbcamera.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's private chat layer. An invoker (not a {@code @Shadow}) so its target is remapped through the refmap
 * on every loader, including Forge 1.21 which runs on SRG names.
 */
@Mixin(Gui.class)
public interface GuiAccessor {

    @Invoker("renderChat")
    void superbcamera$renderChat(GuiGraphics guiGraphics, DeltaTracker deltaTracker);
}
