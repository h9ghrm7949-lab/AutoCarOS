/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.autocar.launcher.core.util

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * 重用桌面View（比如图标、文件夹、widget等）的缓存容器
 */
open class ViewCache {
    protected val mCache: SparseArray<CacheEntry> = SparseArray<CacheEntry>()

    fun setCacheSize(layoutId: Int, size: Int) {
        mCache.put(layoutId, CacheEntry(size))
    }

    fun <T : View?> getView(layoutId: Int, context: Context?, parent: ViewGroup?): T? {
        var entry = mCache.get(layoutId)
        if (entry == null) {
            entry = CacheEntry(1)
            mCache.put(layoutId, entry)
        }

        if (entry.mCurrentSize > 0) {
            entry.mCurrentSize--
            val result = entry.mViews[entry.mCurrentSize] as T?
            entry.mViews[entry.mCurrentSize] = null
            return result
        }

        return LayoutInflater.from(context).inflate(layoutId, parent, false) as T?
    }

    fun recycleView(layoutId: Int, view: View?) {
        val entry = mCache.get(layoutId)
        if (entry != null && entry.mCurrentSize < entry.mMaxSize) {
            entry.mViews[entry.mCurrentSize] = view
            entry.mCurrentSize++
        }
    }

    class CacheEntry(val mMaxSize: Int) {
        val mViews: Array<View?> = arrayOfNulls(mMaxSize)

        var mCurrentSize: Int = 0
    }
}
