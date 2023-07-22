package com.lambda.mixin.gui;

import com.lambda.client.gui.mc.KoreanGuiChat;
import com.lambda.client.gui.mc.LambdaGuiChat;
import com.lambda.client.util.Wrapper;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Shadow protected GuiTextField inputField;
    @Shadow private String historyBuffer;
    @Shadow private int sentHistoryCursor;

    @Inject(method = "initGui", at = @At("RETURN"))
    public void returnInitGui(CallbackInfo info) {
        GuiScreen currentScreen = Wrapper.getMinecraft().currentScreen;
        if (currentScreen instanceof GuiChat) {
            if (!(currentScreen instanceof KoreanGuiChat) && !(currentScreen instanceof LambdaGuiChat)) {
                Wrapper.getMinecraft().displayGuiScreen(
                    new KoreanGuiChat(inputField.getText(), null, historyBuffer, sentHistoryCursor)
                );
            }
        }
    }
}
