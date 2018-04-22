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

package com.tang.intellij.lua.stubs

import com.intellij.openapi.util.Key
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.psi.LuaClassMember

object StubKeys {
    val CLASS_MEMBER: IndexId<Int, LuaClassMember> = IndexId.create<Int, LuaClassMember>("lua.index.class.member")
    val SHORT_NAME = Key.create<NavigatablePsiElement>("lua.index.short_name")
    val CLASS: IndexId<String, LuaDocClassDef> = IndexId.create<String, LuaDocClassDef>("lua.index.class")
    val SUPER_CLASS: IndexId<String, LuaDocClassDef> = IndexId.create<String, LuaDocClassDef>("lua.index.super_class")
}
