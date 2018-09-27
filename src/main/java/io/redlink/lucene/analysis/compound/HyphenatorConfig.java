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

package io.redlink.lucene.analysis.compound;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;

import org.apache.lucene.analysis.util.ResourceLoader;

import io.redlink.lucene.analysis.util.HashUtil;

public class HyphenatorConfig {
    
    private final ResourceLoader resourceLoader;
    
    private final String hyphFile;
    private final byte[] hyphMd5;
    private final String encoding;
    
    public HyphenatorConfig(ResourceLoader resourceLoader, String hyphenator, String encoding) throws IOException {
        super();
        this.resourceLoader = resourceLoader;
        this.hyphFile = hyphenator;
        try (InputStream in = resourceLoader.openResource(hyphenator)){
            this.hyphMd5 = HashUtil.md5(in);
        }
        this.encoding = encoding;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public String getHyphFile() {
        return hyphFile;
    }
    
    public String getEncoding() {
        return encoding;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
        result = prime * result + Arrays.hashCode(hyphMd5);
        result = prime * result + ((hyphFile == null) ? 0 : hyphFile.hashCode());
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
        HyphenatorConfig other = (HyphenatorConfig) obj;
        if (encoding == null) {
            if (other.encoding != null)
                return false;
        } else if (!encoding.equals(other.encoding))
            return false;
        if (!Arrays.equals(hyphMd5, other.hyphMd5))
            return false;
        if (hyphFile == null) {
            if (other.hyphFile != null)
                return false;
        } else if (!hyphFile.equals(other.hyphFile))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HyphenatorConfig [hyphenator=" + hyphFile + 
                "(md5=" + new BigInteger(1, hyphMd5).toString(16) + "), encoding=" + encoding+ "]";
    }

 
    
    
}
