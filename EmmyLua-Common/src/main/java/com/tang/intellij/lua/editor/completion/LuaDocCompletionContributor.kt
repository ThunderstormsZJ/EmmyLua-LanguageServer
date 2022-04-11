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

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.psi.LuaClassField
import com.tang.intellij.lua.psi.LuaFuncBodyOwner
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass
import org.eclipse.lsp4j.CompletionItemKind

/**
 * doc 相关代码完成
 * Created by tangzx on 2016/12/2.
 */
class LuaDocCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, SHOW_DOC_TAG, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                val set = LuaParserDefinition.DOC_TAG_TOKENS
                for (type in set.types) {
                    completionResultSet.addElement(LookupElementBuilder.create(type).withIcon(LuaIcons.ANNOTATION))
                }
                ADDITIONAL_TAGS.forEach { tagName ->
                    completionResultSet.addElement(LookupElementBuilder.create(tagName).withIcon(LuaIcons.ANNOTATION))
                }
                completionResultSet.stopHere()
            }
        })

        extend(CompletionType.BASIC, SHOW_OPTIONAL, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                completionResultSet.addElement(LookupElementBuilder.create("optional"))
            }
        })

        extend(CompletionType.BASIC, AFTER_PARAM, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                var element = completionParameters.originalFile.findElementAt(completionParameters.offset - 1)
                if (element != null && element !is LuaDocPsiElement)
                    element = element.parent

                if (element is LuaDocPsiElement) {
                    val owner = LuaCommentUtil.findOwner(element)
                    if (owner is LuaFuncBodyOwner) {
                        val body = owner.funcBody
                        if (body != null) {
                            // 排除已经存在得参数
                            val comment = PsiTreeUtil.getParentOfType(completionParameters.position, LuaComment::class.java)
                            var paramDefList = PsiTreeUtil.findChildrenOfType(comment, LuaDocTagParam::class.java)
                            var existParamList = ArrayList<String?>()
                            for (paramDef in paramDefList){
                                if (paramDef.textOffset != element.textOffset){
                                    existParamList.add(paramDef.paramNameRef?.text)
                                }
                            }

                            val parDefList = body.paramNameDefList
                            var paramGroupStr = ""
                            for (parDef in parDefList){
                                var parDefText =parDef.text
                                if (!existParamList.contains(parDefText)){
                                    paramGroupStr += "$parDefText "
                                    completionResultSet.addElement(
                                            LookupElementBuilder.create(parDefText)
                                                    .withIcon(LuaIcons.PARAMETER)
                                    )
                                }
                            }

                            // 添加多个参数得提示
                            paramGroupStr = paramGroupStr.trim()
                            var paramMultiList = paramGroupStr.split(" ")

                            if (paramGroupStr.isNotEmpty() && paramMultiList.size > 1){
                                val luaLookElement = LuaLookupElement("[params]").withIcon(LuaIcons.PARAMETER) as LuaLookupElement
                                var inserText = ""
                                for ((i,p) in paramMultiList.withIndex()){
                                    inserText += if (i != 0) "---@param $p " else "$p "
                                    if (i!=paramMultiList.size-1) inserText += "\n"
                                }
                                luaLookElement.kind = CompletionItemKind.Property
                                luaLookElement.insertText = inserText
                                completionResultSet.addElement(luaLookElement)
                            }
                        }
                    }
                }
            }
        })

        extend(CompletionType.BASIC, SHOW_CLASS, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                val project = completionParameters.position.project
                // 为什么 text会带emmy?
                val prefix = completionParameters.position.text.substringBefore("emmy")
                val dotIndex = completionParameters.position.text.indexOf('.')
                LuaShortNamesManager.getInstance(project).processAllClassNames(project, Processor {
                    if (dotIndex != -1 && prefix.isNotEmpty()) {
                        if (it.startsWith(prefix, true)) {
                            val luaLookElement = LuaLookupElement(it.substringAfter('.'))
                            luaLookElement.kind = CompletionItemKind.Class
                            completionResultSet.addElement(luaLookElement)
                        }
                    } else {
                        val luaLookElement = LuaLookupElement(it)
                        luaLookElement.kind = CompletionItemKind.Class
                        completionResultSet.addElement(luaLookElement)
                    }
                    true
                })

                LuaShortNamesManager.getInstance(project).processAllAlias(project, Processor { key ->
                    completionResultSet.addElement(LookupElementBuilder.create(key).withIcon(LuaIcons.Alias))
                    true
                })
                completionResultSet.stopHere()
            }
        })

        extend(CompletionType.BASIC, SHOW_ACCESS_MODIFIER, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                completionResultSet.addElement(LookupElementBuilder.create("protected"))
                completionResultSet.addElement(LookupElementBuilder.create("public"))
            }
        })

        // 属性提示
        extend(CompletionType.BASIC, SHOW_FIELD, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                val position = completionParameters.position
                val comment = PsiTreeUtil.getParentOfType(position, LuaComment::class.java)
                val classDef = PsiTreeUtil.findChildOfType(comment, LuaDocTagClass::class.java)
                if (classDef != null) {
                    val classType = classDef.type
                    val ctx = SearchContext.get(classDef.project)
                    classType.processMembers(ctx) { _, member ->
                        if (member is LuaClassField)
                            completionResultSet.addElement(LookupElementBuilder.create(member.name!!).withIcon(LuaIcons.CLASS_FIELD))
                        Unit
                    }
                }
            }
        })

        // @see member completion
        extend(CompletionType.BASIC, SHOW_SEE_MEMBER, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                val position = completionParameters.position
                val seeRefTag = PsiTreeUtil.getParentOfType(position, LuaDocTagSee::class.java)
                if (seeRefTag != null) {
                    val classType = seeRefTag.classNameRef?.resolveType() as? ITyClass
                    val ctx = SearchContext.get(seeRefTag.project)
                    classType?.processMembers(ctx) { _, member ->
                        completionResultSet.addElement(LookupElementBuilder.create(member.name!!).withIcon(LuaIcons.CLASS_FIELD))
                        Unit
                    }
                }
                completionResultSet.stopHere()
            }
        })

        /*extend(CompletionType.BASIC, SHOW_LAN, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(completionParameters: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet) {
                Language.getRegisteredLanguages().forEach {
                    val fileType = it.associatedFileType
                    var lookupElement = LookupElementBuilder.create(it.id)
                    if (fileType != null)
                        lookupElement = lookupElement.withIcon(fileType.icon)
                    completionResultSet.addElement(lookupElement)
                }
                completionResultSet.stopHere()
            }
        })*/
    }

    companion object {

        // 在 @ 之后提示 param class type ...
        private val SHOW_DOC_TAG = psiElement(LuaDocTypes.TAG_NAME)

        // 在 @param 之后提示方法的参数
        private val AFTER_PARAM = psiElement().withParent(LuaDocParamNameRef::class.java)

        // 在 @param 之后提示 optional
        private val SHOW_OPTIONAL = psiElement().afterLeaf(
                psiElement(LuaDocTypes.TAG_NAME_PARAM))

        // 在 extends 之后提示类型
        private val SHOW_CLASS = psiElement().withParent(LuaDocClassNameRef::class.java)

        // 在 @field 之后提示 public / protected
        private val SHOW_ACCESS_MODIFIER = psiElement().afterLeaf(
                psiElement().withElementType(LuaDocTypes.TAG_NAME_FIELD)
        )

        private val SHOW_FIELD = psiElement(LuaDocTypes.ID).inside(LuaDocTagField::class.java)

        //@see type#MEMBER
        private val SHOW_SEE_MEMBER = psiElement(LuaDocTypes.ID).inside(LuaDocTagSee::class.java)

        private val SHOW_LAN = psiElement(LuaDocTypes.ID).inside(LuaDocTagLan::class.java)

        private val ADDITIONAL_TAGS = arrayOf("deprecated", "author", "version", "since")
    }
}
