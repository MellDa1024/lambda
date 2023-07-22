package com.lambda.client.gui.mc

import com.github.kimcore.inko.Inko.Companion.asEnglish
import com.github.kimcore.inko.Inko.Companion.asKoreanWithDoubleConsonant
import com.lambda.client.LambdaMod
import com.lambda.client.module.modules.chat.KoreanChat.Language
import com.lambda.client.module.modules.chat.KoreanChat.language
import com.lambda.client.module.modules.chat.KoreanChat.ctrlComboBypass
import com.lambda.client.module.modules.chat.KoreanChat.debugging
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.runSafe
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

class KoreanGuiTextField(
    componentId: Int,
    private val fontRenderer: FontRenderer,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) : GuiTextField(componentId, fontRenderer, x, y, width, height) {

    private val engIndex = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"
    private val korIndex = "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔㅁㄴㅇㄹㅎㅗㅓㅏㅣㅋㅌㅊㅍㅠㅜㅡㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖㅁㄴㅇㄹㅎㅗㅓㅏㅣㅋㅌㅊㅍㅠㅜㅡ"

    private var convertStartingIndex = 0
    private var canLoseFocusKTFSide = true
    private var isEnabledKTFSide = true

    override fun drawTextBox() {
        super.drawTextBox()
        drawRect(x - 3, y - 2 - height, x + 10, y - 2, 587137024)
        drawRect(x - 3, y - 1 - height, x + 9, y -3, Int.MIN_VALUE)
        drawString(fontRenderer, if (language == Language.KOREAN) "한" else "영", x, y - height, 16777215)
    }

    override fun moveCursorBy(num: Int) {
        super.moveCursorBy(num)
        resetConvertStartingIndex()
    }

    override fun textboxKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!isFocused) return false
        if (keyCode == Keyboard.KEY_LCONTROL) {
            resetConvertStartingIndex()
            language.switch()
        }
        if (GuiScreen.isKeyComboCtrlA(keyCode) || GuiScreen.isKeyComboCtrlC(keyCode) ||
            GuiScreen.isKeyComboCtrlV(keyCode) || GuiScreen.isKeyComboCtrlX(keyCode)) {
            if (ctrlComboBypass) language.switch()
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (GuiScreen.isCtrlKeyDown()) {
                if (ctrlComboBypass) language.switch()
                if (isEnabledKTFSide) deleteWords(-1)
            } else if (isEnabledKTFSide) {
                if (cursorPosition == convertStartingIndex) deleteFromCursor(-1)
                else {
                    //KoreanDeletionPart
                    deleteFromCursorWhenKorean()
                }
            }
            return true
        }
        return super.textboxKeyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        val result = super.mouseClicked(mouseX, mouseY, mouseButton)
        if (result) resetConvertStartingIndex()
        return result
    }

    override fun setCanLoseFocus(canLoseFocusIn: Boolean) {
        super.setCanLoseFocus(canLoseFocusIn)
        this.canLoseFocusKTFSide = canLoseFocusIn
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.isEnabledKTFSide = enabled
    }

    override fun writeText(textToWrite: String) {
        if (language == Language.KOREAN && textToWrite.length == 1) {
            val index = engIndex.indexOf(textToWrite)
            if (index != -1) {
                val convertedKoreanChar = korIndex.getOrNull(index) ?: kotlin.run {
                    if (debugging) LambdaMod.LOG.info(
                        "TextToWrite : $textToWrite / index : $index / engIndex : $engIndex / korIndex : $korIndex"
                    )
                    return
                }
                try {
                    if (debugging) LambdaMod.LOG.info("--------------Typing--------------")
                    if (selectionEnd != this.cursorPosition) removeSelection()
                    if (debugging) LambdaMod.LOG.info("FullText : $text - 0 : $convertStartingIndex : $cursorPosition >> " +
                        "${text.getOrNull(0)}, ${text.getOrNull(convertStartingIndex)}, ${text.getOrNull(cursorPosition)}")

                    if (text.isNotEmpty()) {
                        val convertingTextBefore = text.substring(0 until convertStartingIndex)
                        if (debugging) LambdaMod.LOG.info(convertingTextBefore)

                        val convertingText = text.substring(convertStartingIndex until this.cursorPosition) + convertedKoreanChar
                        if (debugging) LambdaMod.LOG.info(convertingText)

                        val convertingTextAfter =
                            if (text.length > this.cursorPosition) text.substring(this.cursorPosition)
                            else ""
                        if (debugging) LambdaMod.LOG.info(convertingTextAfter)

                        val convertedText = convertingText.asEnglish.asKoreanWithDoubleConsonant
                        val cursorDifference = convertingText.length - convertedText.length
                        val finalCursorPosition = cursorPosition - cursorDifference + 1

                        text = convertingTextBefore + convertedText + convertingTextAfter
                        cursorPosition = finalCursorPosition
                    } else {
                        text = convertedKoreanChar.toString()
                    }
                } catch (e : Exception) {
                    runSafe {
                        mc.displayGuiScreen(null)
                        e.printStackTrace()
                        MessageSendHelper.sendErrorMessage("""
                            Error occurred while using KoreanGuiChat: $e
                            check log for more info.
                            유저분께서 해당 메시지를 받으셨다면 개발자에게 문의해주세요!
                        """.trimIndent())
                    }
                }
                return
            }
        }
        super.writeText(textToWrite)
    }

    private fun removeSelection() {
        if (debugging) LambdaMod.LOG.info("--------------Removing Selection--------------")
        val selStart = if (cursorPosition < selectionEnd) cursorPosition else selectionEnd
        val selEnd = (if (cursorPosition < selectionEnd) selectionEnd else cursorPosition) - 1
        if (debugging) LambdaMod.LOG.info("$text - $selStart : $selEnd >> ${text.getOrNull(selStart)}, ${text.getOrNull(selEnd)}")
        if (debugging) LambdaMod.LOG.info("${text.substring(0 until selStart)} / ${text.substring(selEnd)}")
        if (debugging) LambdaMod.LOG.info(text.removeRange(selStart..selEnd))
        text = text.removeRange(selStart..selEnd)
        this.cursorPosition = selStart
        resetConvertStartingIndex()
    }

    //기존 한글채팅 변환-합성 방식을 사용하면 국사같은 단어가 국ㅅ이 되는게 아니라 굯가 됨
    //어자피 backspace는 한 단어에만 영향을 주므로 한 단어만 뽑아서 사용
    private fun deleteFromCursorWhenKorean() {
        try {
            if (selectionEnd != this.cursorPosition) return removeSelection()
            if (debugging) LambdaMod.LOG.info("--------------Deletion--------------")
            if (debugging) LambdaMod.LOG.info("FullText : $text / Len : ${text.length} - 0 : $cursorPosition >> " +
                "${text.getOrNull(0)}, ${text.getOrNull(cursorPosition - 1)}")

            val convertingTextBefore = text.substring(0 until this.cursorPosition - 1)
            if (debugging) LambdaMod.LOG.info(convertingTextBefore)

            val convertingText = text[this.cursorPosition - 1].toString()
            if (debugging) LambdaMod.LOG.info(convertingText)

            val convertingTextAfter =
                if (text.length > this.cursorPosition) text.substring(this.cursorPosition)
                else ""
            if (debugging) LambdaMod.LOG.info(convertingTextAfter)

            //asKorean이든 asKoreanWithDoubleConsonant이든 한 단어만 빠지는 상황에서 이중 종성이 일어날 리가 없음
            val convertedText = convertingText.asEnglish.dropLast(1).asKoreanWithDoubleConsonant
            val cursorDifference = convertingText.length - convertedText.length
            val finalCursorPosition = cursorPosition - cursorDifference

            text = convertingTextBefore + convertedText + convertingTextAfter
            cursorPosition = finalCursorPosition
            //중성/종성만 빼는 것은 한 글자에만 적용, 그 후로부터는 글자째로 사라져야함
            if (convertedText.isEmpty()) resetConvertStartingIndex()
            else resetConvertStartingIndex(1)
        } catch (e : Exception) {
            runSafe {
                mc.displayGuiScreen(null)
                e.printStackTrace()
                MessageSendHelper.sendErrorMessage("""
                            Error occurred while using KoreanGuiChat: $e 
                            check log for more info. 
                            유저분께서 해당 메시지를 받으셨다면 개발자에게 문의해주세요!
                        """.trimIndent())
            }
        }
        return
    }

    private fun resetConvertStartingIndex(num: Int = 0) {
        if (debugging) LambdaMod.LOG.info("Forced-cursor move, text.length : ${text.length}, cursor : ${this.cursorPosition}")
        convertStartingIndex = this.cursorPosition - num
    }
}
