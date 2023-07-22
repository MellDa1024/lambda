package com.lambda.client.module.modules.chat

import com.lambda.mixin.gui.MixinGuiChat
import com.lambda.client.gui.mc.KoreanGuiChat
import com.lambda.client.gui.mc.KoreanGuiTextField
import com.lambda.client.module.Category
import com.lambda.client.module.Module

object KoreanChat : Module(
    name = "KoreanChat",
    description = "KoreanChat settings",
    category = Category.CHAT,
    alwaysEnabled = true,
    showOnArray = false
) {
    var language by setting("Language", Language.KOREAN)
    var ctrlComboBypass by setting("CtrlComboBypass", true, description =  "Force to stop switching korean/english when player uses a ctrl combokey.")
    val debugging by setting("Debugging", false, description = "Logs something. I do not recommend to enable this without any reason.")

    enum class Language {
        KOREAN, ENGLISH;

        fun switch() {
            if (this == KOREAN) language = ENGLISH
            if (this == ENGLISH) language = KOREAN
        }
    }
    /**
     * @see KoreanGuiChat
     * @see KoreanGuiTextField
     * @see MixinGuiChat
     */
}
