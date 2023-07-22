package com.lambda.client.gui.mc

import com.lambda.client.command.CommandManager
import com.lambda.client.mixin.extension.historyBuffer
import com.lambda.client.mixin.extension.sentHistoryCursor
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.ITabCompleter
import org.lwjgl.input.Keyboard

class KoreanGuiChat(
    private val startStringIn: String,
    private val inputFieldOrigin: GuiTextField? = null,
    private val historyBufferIn: String? = null,
    private val sentHistoryCursorIn: Int? = null
) : GuiChat(startStringIn), ITabCompleter {

    lateinit var tabCompleter: ChatTabCompleter

    override fun initGui() {

        super.initGui()

        //inputFieldOrigin#cursorPosition will be changed after inputField.text = startStringIn
        val cursorPosition = inputFieldOrigin?.cursorPosition

        if (inputFieldOrigin != null) {
            inputField = inputFieldOrigin
        } else {
            inputField = KoreanGuiTextField(0, fontRenderer, 4, height - 12, width - 4, 12)
            inputField.maxStringLength = 256
            inputField.enableBackgroundDrawing = false
            inputField.isFocused = true
            inputField.setCanLoseFocus(false)
        }
        inputField.text = startStringIn

        if (cursorPosition != null) {
            inputField.cursorPosition = cursorPosition
        }

        this.tabCompleter = ChatTabCompleter(inputField)
        historyBufferIn?.let { historyBuffer = it }
        sentHistoryCursorIn?.let { sentHistoryCursor = it }

        if (inputField.text.startsWith(CommandManager.prefix)) {
            mc.displayGuiScreen(LambdaGuiChat(inputField, historyBuffer, sentHistoryCursor))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        tabCompleter.resetRequested()
        if (keyCode == 15) {
            this.tabCompleter.complete()
            return
        }
        else tabCompleter.resetDidComplete()

        if (guiChatKeyTyped(typedChar, keyCode)) return

        if (inputField.text.startsWith(CommandManager.prefix)) {
            mc.displayGuiScreen(LambdaGuiChat(inputField, historyBuffer, sentHistoryCursor))
            return
        }
    }

    override fun setCompletions(vararg newCompletions: String) {
        this.tabCompleter.setCompletions(*newCompletions)
    }

    private fun guiChatKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        return if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            true
        } else if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_NUMPADENTER) {
            val chatGUI = mc.ingameGUI.chatGUI
            when (keyCode) {
                Keyboard.KEY_UP -> getSentHistory(-1)
                Keyboard.KEY_DOWN -> getSentHistory(1)
                Keyboard.KEY_PRIOR -> chatGUI.scroll(chatGUI.lineCount - 1)
                Keyboard.KEY_NEXT -> chatGUI.scroll(-chatGUI.lineCount + 1)
                else ->  inputField.textboxKeyTyped(typedChar, keyCode)
            }
            false
        } else {
            val message = inputField.text.trim()
            if (message.isNotEmpty()) sendChatMessage(message)
            mc.ingameGUI.chatGUI.addToSentMessages(message)
            mc.displayGuiScreen(null)
            true
        }
    }


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawRect(2, height - 14, width - 2, height - 2, Int.MIN_VALUE)
        inputField.drawTextBox()
    }
}
