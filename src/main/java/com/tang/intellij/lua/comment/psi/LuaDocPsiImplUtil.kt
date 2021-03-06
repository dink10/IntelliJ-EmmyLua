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

package com.tang.intellij.lua.comment.psi

import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.tang.intellij.lua.comment.reference.LuaClassNameReference
import com.tang.intellij.lua.comment.reference.LuaDocParamNameReference
import com.tang.intellij.lua.psi.LuaElementFactory
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import javax.swing.Icon

/**

 * Created by TangZX on 2016/11/24.
 */
fun getReference(paramNameRef: LuaDocParamNameRef): PsiReference {
    return LuaDocParamNameReference(paramNameRef)
}

fun getReference(docClassNameRef: LuaDocClassNameRef): PsiReference {
    return LuaClassNameReference(docClassNameRef)
}

fun resolveType(nameRef: LuaDocClassNameRef): ITy {
    return TyLazyClass(nameRef.text)
}

fun getName(identifierOwner: PsiNameIdentifierOwner): String? {
    val id = identifierOwner.nameIdentifier
    return id?.text
}

fun setName(identifierOwner: PsiNameIdentifierOwner, newName: String): PsiElement {
    val oldId = identifierOwner.nameIdentifier
    if (oldId != null) {
        val newId = LuaElementFactory.createIdentifier(identifierOwner.project, newName)
        oldId.replace(newId)
        return newId
    }
    return identifierOwner
}

fun getTextOffset(identifierOwner: PsiNameIdentifierOwner): Int {
    val id = identifierOwner.nameIdentifier
    return id?.textOffset ?: identifierOwner.node.startOffset
}

fun getNameIdentifier(fieldDef: LuaDocFieldDef): PsiElement? {
    return fieldDef.id
}

fun getNameIdentifier(classDef: LuaDocClassDef): PsiElement {
    return classDef.id
}

@Suppress("UNUSED_PARAMETER")
fun guessType(fieldDef: LuaDocFieldDef, context: SearchContext): ITy {
    val stub = fieldDef.stub
    if (stub != null)
        return stub.type
    return resolveDocTypeSet(fieldDef.typeSet)
}

/**
 * 猜测参数的类型
 * @param paramDec 参数定义
 * *
 * @return 类型集合
 */
fun getType(paramDec: LuaDocParamDef): ITy {
    val docTypeSet = paramDec.typeSet ?: return Ty.UNKNOWN
    return resolveDocTypeSet(docTypeSet)
}

/**
 * 获取返回类型
 * @param returnDef 返回定义
 *
 * @return 类型集合
 */
fun resolveTypeAt(returnDef: LuaDocReturnDef, index: Int): ITy {
    val typeList = returnDef.typeList
    if (typeList != null) {
        val typeSetList = typeList.typeSetList
        if (typeSetList.size > index) {
            val docTypeSet = typeSetList[index]
            return resolveDocTypeSet(docTypeSet)
        }
    }
    return Ty.UNKNOWN
}

fun resolveDocTypeSet(docTypeSet: LuaDocTypeSet?): ITy {
    if (docTypeSet != null) {
        val list = docTypeSet.tyList
        var retTy: ITy = Ty.UNKNOWN
        for (ty in list) {
            retTy = retTy.union(ty.getType())
        }
        return retTy
    }
    return Ty.UNKNOWN
}

/**
 * 优化：从stub中取名字
 * @param classDef LuaDocClassDef
 * *
 * @return string
 */
fun getName(classDef: LuaDocClassDef): String {
    val stub = classDef.stub
    if (stub != null)
        return stub.className
    return classDef.id.text
}

/**
 * for Goto Class
 * @param classDef class def
 * *
 * @return ItemPresentation
 */
fun getPresentation(classDef: LuaDocClassDef): ItemPresentation {
    return object : ItemPresentation {
        override fun getPresentableText(): String? {
            return classDef.name
        }

        override fun getLocationString(): String? {
            return classDef.containingFile.name
        }

        override fun getIcon(b: Boolean): Icon? {
            return AllIcons.Nodes.Class
        }
    }
}

fun getType(classDef: LuaDocClassDef): ITyClass {
    val stub = classDef.stub
    val luaType: ITyClass
    if (stub != null) {
        luaType = stub.classType
    } else {
        luaType = TyPsiDocClass(classDef)
    }
    return luaType
}

/**
 * 猜测类型
 * @param typeDef 类型定义
 * *
 * @return 类型集合
 */
fun getType(typeDef: LuaDocTypeDef): ITy {
    return resolveDocTypeSet(typeDef.typeSet)
}

@Suppress("UNUSED_PARAMETER")
fun toString(stubElement: StubBasedPsiElement<out StubElement<*>>): String {
    return "[STUB]"// + stubElement.getNode().getElementType().toString();
}

fun getName(fieldDef: LuaDocFieldDef): String? {
    val stub = fieldDef.stub
    if (stub != null)
        return stub.name
    return getName(fieldDef as PsiNameIdentifierOwner)
}

fun getFieldName(fieldDef: LuaDocFieldDef): String? {
    val stub = fieldDef.stub
    if (stub != null)
        return stub.name
    return fieldDef.name
}

fun getPresentation(fieldDef: LuaDocFieldDef): ItemPresentation {
    return object : ItemPresentation {
        override fun getPresentableText(): String? {
            return fieldDef.name
        }

        override fun getLocationString(): String? {
            return fieldDef.containingFile.name
        }

        override fun getIcon(b: Boolean): Icon? {
            return AllIcons.Nodes.Field
        }
    }
}

fun getType(luaDocArrTy: LuaDocArrTy): ITy {
    val baseTy = luaDocArrTy.ty.getType()
    return TyArray(baseTy)
}

fun getType(luaDocGeneralTy: LuaDocGeneralTy): ITy {
    return resolveType(luaDocGeneralTy.classNameRef)
}

fun getType(luaDocFunctionTy: LuaDocFunctionTy): ITy {
    return TyDocPsiFunction(luaDocFunctionTy)
}

fun getReturnType(luaDocFunctionTy: LuaDocFunctionTy): ITy {
    val set = luaDocFunctionTy.typeSet
    return resolveDocTypeSet(set)
}

fun getType(luaDocGenericTy: LuaDocGenericTy): ITy {
    return TyDocGeneric(luaDocGenericTy)
}