/*
 * Copyright (C) 2018 Apenk.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kweny.carefree.mongodb;

/**
 * 辅助工具
 *
 * @author Kweny
 * @since 1.0.0
 */
class _Assistant {

    static final String MONGO_CAREFREE_PREFIX = "carefree.mongodb";
    static final String MONGO_CAREFREE_OPTIONS_PREFIX = "carefree.mongodb.options";
    static final String ENABLED_OVERRIDE_PROPERTY = "carefree.mongodb.enable";

    static final String DEFAULT_MONGO_TEMPLATE_NAME = "mongoTemplate";
    static final String DEFAULT_GRID_FS_TEMPLATE_NAME = "gridFsTemplate";

    static String deleteWhitespace(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        final int sz = str.length();
        final char[] chs = new char[sz];
        int count = 0;
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                chs[count++] = str.charAt(i);
            }
        }
        if (count == sz) {
            return str;
        }
        return new String(chs, 0, count);
    }

}