/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaFuncBodyOwner
import com.tang.intellij.lua.psi.LuaParamInfo
import com.tang.intellij.lua.search.LuaPredefinedScope
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassMethodIndex
import com.tang.intellij.lua.ty.ITyClass

/**
 * override supper
 * Created by tangzx on 2016/12/25.
 */
class OverrideCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
        val id = completionParameters.position
        val methodDef = PsiTreeUtil.getParentOfType(id, LuaClassMethodDef::class.java)
        if (methodDef != null) {
            val context = SearchContext(methodDef.project)
            val classType = methodDef.getClassType(context)
            if (classType != null) {
                val sup = classType.getSuperClass(context)
                addOverrideMethod(completionParameters, completionResultSet, sup)
            }
        }
    }

    private fun addOverrideMethod(completionParameters: CompletionParameters, completionResultSet: CompletionResultSet, sup: ITyClass?) {
        var superCls = sup
        if (superCls != null) {
            val project = completionParameters.originalFile.project
            val context = SearchContext(project)
            val clazzName = superCls.className
            val list = LuaClassMethodIndex.instance.get(clazzName, project, LuaPredefinedScope(project))
            for (def in list) {
                val methodName = def.name
                if (methodName != null) {
                    val elementBuilder = LookupElementBuilder.create(def.getName()!!)
                            .withIcon(LuaIcons.CLASS_METHOD_OVERRIDING)
                            .withInsertHandler(OverrideInsertHandler(def))
                            .withTailText(def.getParamSignature())

                    completionResultSet.addElement(elementBuilder)
                }
            }

            superCls = superCls.getSuperClass(context)
            addOverrideMethod(completionParameters, completionResultSet, superCls)
        }
    }

    internal class OverrideInsertHandler(funcBodyOwner: LuaFuncBodyOwner) : FuncInsertHandler(funcBodyOwner) {

        override val autoInsertParameters = true

        override fun createTemplate(manager: TemplateManager, paramNameDefList: Array<LuaParamInfo>): Template {
            val template = super.createTemplate(manager, paramNameDefList)
            template.addEndVariable()
            template.addTextSegment("\nend")
            return template
        }
    }
}
