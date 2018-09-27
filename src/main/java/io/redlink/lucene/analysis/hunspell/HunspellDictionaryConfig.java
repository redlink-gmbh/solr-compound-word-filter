/*
 * Copyright 2018 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.redlink.lucene.analysis.hunspell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.util.ResourceLoader;

import io.redlink.lucene.analysis.util.HashUtil;

public class HunspellDictionaryConfig {

    private final ResourceLoader resourceLoader;
    
    private final List<String> dictionaryFiles;
    private final byte[] dictMd5;
    private final String affixFile;
    private final byte[] affixMd5;
    private final boolean ignoreCase;

    
    HunspellDictionaryConfig(ResourceLoader resourceLoader, String affixFile, List<String> dictionaryFiles, boolean ignoreCase) throws IOException {
        this.resourceLoader = resourceLoader;
        this.dictionaryFiles = dictionaryFiles;
        this.affixFile = affixFile;
        this.ignoreCase = ignoreCase;
        //we want to detect changes in the files!
        try (InputStream in = resourceLoader.openResource(affixFile)){
            this.affixMd5 = HashUtil.md5(in);
        }
        ByteArrayOutputStream dicMd5Out = new ByteArrayOutputStream();
        for(String dict : dictionaryFiles){
            try (InputStream in = resourceLoader.openResource(dict)){
                dicMd5Out.write(HashUtil.md5(in));
            }
        }
        dictMd5 = dicMd5Out.toByteArray();
    
    }


    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }


    public List<String> getDictionaryFiles() {
        return dictionaryFiles;
    }


    public String getAffixFile() {
        return affixFile;
    }


    public boolean isIgnoreCase() {
        return ignoreCase;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((affixFile == null) ? 0 : affixFile.hashCode());
        result = prime * result + ((affixMd5 == null) ? 0 : Arrays.hashCode(affixMd5));
        result = prime * result + ((dictionaryFiles == null) ? 0 : dictionaryFiles.hashCode());
        result = prime * result + ((dictMd5 == null) ? 0 : Arrays.hashCode(dictMd5));
        result = prime * result + (ignoreCase ? 1231 : 1237);
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HunspellDictionaryConfig other = (HunspellDictionaryConfig) obj;
        if (affixFile == null) {
            if (other.affixFile != null)
                return false;
        } else if (!affixFile.equals(other.affixFile))
            return false;
        if (affixMd5 == null) {
            if (other.affixMd5 != null)
                return false;
        } else if (!Arrays.equals(affixMd5, other.affixMd5))
            return false;
        if (dictionaryFiles == null) {
            if (other.dictionaryFiles != null)
                return false;
        } else if (!dictionaryFiles.equals(other.dictionaryFiles))
            return false;
        if (dictMd5 == null) {
            if (other.dictMd5 != null)
                return false;
        } else if (!Arrays.equals(dictMd5, other.dictMd5))
            return false;
        if (ignoreCase != other.ignoreCase)
            return false;
        return true;
    }


    @Override
    public String toString() {
        return "DictionaryConfig [affix=" + affixFile + "(md5:" + new BigInteger(1, affixMd5).toString(16) 
                + "), dictionary=" + dictionaryFiles + "(md5:" + new BigInteger(1, dictMd5).toString(16) 
                + "), ignoreCase=" + ignoreCase + "]";
    }
    
    
    
}
