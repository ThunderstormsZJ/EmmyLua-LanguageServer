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

package com.tang.vscode.documentation

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassIndex
import com.tang.intellij.lua.ty.*

/**
 * Documentation support
 * Created by tangzx on 2016/12/10.
 */
class LuaDocumentationProvider : DocumentationProvider {
    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String> {
        TODO()
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element != null) {
            when (element) {
                is LuaTypeGuessable -> {
                    val ty = element.guessType(SearchContext.get(element.project))
                    return buildString {
                        renderTy(this, ty)
                    }
                }
            }
        }
        return null
    }

    override fun getDocumentationElementForLookupItem(
        psiManager: PsiManager,
        obj: Any,
        element: PsiElement
    ): PsiElement? {
        return null
    }

    override fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement?
    ): PsiElement? {
        return LuaClassIndex.find(link, SearchContext.get(psiManager.project))
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val sb = StringBuilder()

        when (element) {
            is LuaParamNameDef -> renderParamNameDef(sb, element)
            is LuaDocTagClass -> renderClassDef(sb, element)
            is LuaClassMember -> renderClassMember(sb, element)
            is LuaNameDef -> { //local xx
                sb.wrapLanguage("lua") {
                    sb.append("local ${element.name}:")
                    val ty = element.guessType(SearchContext.get(element.project))
                    renderTy(sb, ty)
                    sb.append("\n")

                }

                val owner = PsiTreeUtil.getParentOfType(element, LuaCommentOwner::class.java)
                owner?.let { renderComment(sb, owner.comment) }
            }
            is LuaLocalFuncDef -> {
                sb.wrapLanguage("lua") {
                    sb.append("local function ${element.name}")
                    val type = element.guessType(SearchContext.get(element.project)) as ITyFunction
                    renderSignature(sb, type.mainSignature)
                }
                renderComment(sb, element.comment)
            }
            is LuaPsiFile -> {
                sb.append(element.virtualFile.path.substringAfter("file:/"))
            }

        }
        if (sb.isNotEmpty()) return sb.toString()

        return null
    }

    private fun renderClassMember(sb: StringBuilder, classMember: LuaClassMember) {
        val context = SearchContext.get(classMember.project)
        val parentType = classMember.guessClassType(context)
        val ty = classMember.guessType(context)
        //base info
        if (parentType != null) {
            sb.wrapLanguage("lua") {
                when (classMember.visibility) {
                    Visibility.PUBLIC -> {
                        sb.append("(public) ")
                    }
                    Visibility.PRIVATE -> {
                        sb.append("(private) ")
                    }
                    Visibility.PROTECTED -> {
                        sb.append("(protected) ")
                    }
                }
                when (ty) {
                    is TyFunction -> {
                        sb.append("function ")
                        if (parentType.displayName != "_G") {
                            renderTy(sb, parentType)
                            sb.append(if (ty.isColonCall) ":" else ".")
                        }
                        sb.append(classMember.name)
                        renderSignature(sb, ty.mainSignature)

                        return@wrapLanguage
                    }
                    is TyClass -> {
                        sb.append("class ")
                    }
                    is TyUnion -> {
                        sb.append("union ")
                    }
                    else -> {
                        sb.append("property ")
                    }
                }
                renderTy(sb, parentType)
                sb.append(".${classMember.name}:")
                renderTy(sb, ty)
            }
        } else {
            //NameExpr
            if (classMember is LuaNameExpr) {
                val nameExpr: LuaNameExpr = classMember
                sb.wrapLanguage("lua") {
                    sb.append("global ")
                    with(sb) {
                        append(nameExpr.name)
                        when (ty) {
                            is TyFunction -> renderSignature(sb, ty.mainSignature)
                            else -> {
                                append(":")
                                renderTy(sb, ty)
                            }
                        }
                    }
                }
                val stat = nameExpr.parent.parent // VAR_LIST ASSIGN_STAT
                if (stat is LuaAssignStat) renderComment(sb, stat.comment)
            }
        }


        //comment content
        if (classMember is LuaCommentOwner)
            renderComment(sb, classMember.comment)
        else {
            if (classMember is LuaDocTagField)
                renderCommentString("  ", null, sb, classMember.commentString)
            else if (classMember is LuaIndexExpr) {
                val p1 = classMember.parent
                val p2 = p1.parent
                if (p1 is LuaVarList && p2 is LuaAssignStat) {
                    renderComment(sb, p2.comment)
                }
            }
        }
    }

    private fun renderParamNameDef(sb: StringBuilder, paramNameDef: LuaParamNameDef) {
        val owner = PsiTreeUtil.getParentOfType(paramNameDef, LuaCommentOwner::class.java)
        val docParamDef = owner?.comment?.getParamDef(paramNameDef.name)
        if (docParamDef != null) {
            renderDocParam(sb, docParamDef)
        } else {
            val ty = infer(paramNameDef, SearchContext.get(paramNameDef.project))
            sb.appendLine("@param `${paramNameDef.name}`:")
            renderTy(sb, ty)
        }
    }
}
