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

package com.tang.intellij.lua.stubs.types

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.psi.LuaFile
import com.tang.intellij.lua.psi.LuaGlobalFuncDef
import com.tang.intellij.lua.psi.LuaParamInfo
import com.tang.intellij.lua.psi.LuaPsiImplUtil
import com.tang.intellij.lua.psi.impl.LuaGlobalFuncDefImpl
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.LuaGlobalFuncStub
import com.tang.intellij.lua.stubs.LuaGlobalFuncStubImpl
import com.tang.intellij.lua.stubs.index.LuaClassMethodIndex
import com.tang.intellij.lua.stubs.index.LuaGlobalIndex
import com.tang.intellij.lua.stubs.index.LuaShortNameIndex
import com.tang.intellij.lua.stubs.index.StubKeys
import com.tang.intellij.lua.ty.Ty
import java.io.IOException

/**

 * Created by tangzx on 2016/11/26.
 */
class LuaGlobalFuncType : IStubElementType<LuaGlobalFuncStub, LuaGlobalFuncDef>("Global Function", LuaLanguage.INSTANCE) {

    override fun createPsi(luaGlobalFuncStub: LuaGlobalFuncStub): LuaGlobalFuncDef {
        return LuaGlobalFuncDefImpl(luaGlobalFuncStub, this)
    }

    override fun createStub(globalFuncDef: LuaGlobalFuncDef, stubElement: StubElement<*>): LuaGlobalFuncStub {
        val nameRef = globalFuncDef.nameIdentifier!!
        val searchContext = SearchContext(globalFuncDef.project).setCurrentStubFile(globalFuncDef.containingFile)
        val returnTypeSet = LuaPsiImplUtil.guessReturnTypeSetOriginal(globalFuncDef, searchContext)
        val params = LuaPsiImplUtil.getParamsOriginal(globalFuncDef)
        var moduleName = Constants.WORD_G
        val file = globalFuncDef.containingFile
        if (file is LuaFile) moduleName = file.moduleName ?: Constants.WORD_G

        return LuaGlobalFuncStubImpl(nameRef.text, moduleName, params, returnTypeSet, stubElement)
    }

    override fun getExternalId() = "lua.global_func_def"

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val element = node.psi
        if (element is LuaGlobalFuncDef) {
            return element.nameIdentifier != null
        }
        return false
    }

    @Throws(IOException::class)
    override fun serialize(luaGlobalFuncStub: LuaGlobalFuncStub, stubOutputStream: StubOutputStream) {
        stubOutputStream.writeName(luaGlobalFuncStub.name)
        stubOutputStream.writeName(luaGlobalFuncStub.module)

        // params
        val params = luaGlobalFuncStub.params
        stubOutputStream.writeByte(params.size)
        for (param in params) {
            LuaParamInfo.serialize(param, stubOutputStream)
        }

        Ty.serialize(luaGlobalFuncStub.returnTypeSet, stubOutputStream)
    }

    @Throws(IOException::class)
    override fun deserialize(stubInputStream: StubInputStream, stubElement: StubElement<*>): LuaGlobalFuncStub {
        val name = stubInputStream.readName()
        val module = stubInputStream.readName()

        // params
        val len = stubInputStream.readByte().toInt()
        val params = mutableListOf<LuaParamInfo>()
        for (i in 0 until len) {
            params.add(LuaParamInfo.deserialize(stubInputStream))
        }

        val returnTypeSet = Ty.deserialize(stubInputStream)
        return LuaGlobalFuncStubImpl(StringRef.toString(name)!!,
                StringRef.toString(module)!!,
                params.toTypedArray(),
                returnTypeSet,
                stubElement)
    }

    override fun indexStub(luaGlobalFuncStub: LuaGlobalFuncStub, indexSink: IndexSink) {
        val name = luaGlobalFuncStub.name
        val moduleName = luaGlobalFuncStub.module

        indexSink.occurrence(LuaClassMethodIndex.KEY, moduleName)
        indexSink.occurrence(LuaClassMethodIndex.KEY, moduleName + "*" + name)

        indexSink.occurrence(StubKeys.CLASS_MEMBER, moduleName)
        indexSink.occurrence(StubKeys.CLASS_MEMBER, moduleName + "*" + name)

        indexSink.occurrence(LuaShortNameIndex.KEY, name)
        if (moduleName == Constants.WORD_G)
            indexSink.occurrence(LuaGlobalIndex.KEY, name)
    }
}