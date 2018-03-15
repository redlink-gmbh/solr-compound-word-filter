package io.redlink.lucene.analysis.miscellaneous;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.util.ResourceLoader;

import io.redlink.lucene.analysis.util.HashUtil;

public class StemmerOverrideDictionaryConfig {
    
    
    private final ResourceLoader resourceLoader;
    
    private final List<String> dictionaryFiles;
    private final byte[] dictMd5;

    private final boolean ignoreCase;

    public StemmerOverrideDictionaryConfig(ResourceLoader resourceLoader, List<String> dictionaryFiles,
            boolean ignoreCase) throws IOException {
        super();
        this.resourceLoader = resourceLoader;
        this.dictionaryFiles = dictionaryFiles;
        ByteArrayOutputStream dicMd5Out = new ByteArrayOutputStream();
        for(String dict : dictionaryFiles){
            try (InputStream in = resourceLoader.openResource(dict)){
                dicMd5Out.write(HashUtil.md5(in));
            }
        }
        dictMd5 = dicMd5Out.toByteArray();
        this.ignoreCase = ignoreCase;
    }

    public List<String> getDictionaryFiles() {
        return dictionaryFiles;
    }
    
    public boolean isIgnoreCase() {
        return ignoreCase;
    }
    
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(dictMd5);
        result = prime * result + ((dictionaryFiles == null) ? 0 : dictionaryFiles.hashCode());
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
        StemmerOverrideDictionaryConfig other = (StemmerOverrideDictionaryConfig) obj;
        if (!Arrays.equals(dictMd5, other.dictMd5))
            return false;
        if (dictionaryFiles == null) {
            if (other.dictionaryFiles != null)
                return false;
        } else if (!dictionaryFiles.equals(other.dictionaryFiles))
            return false;
        if (ignoreCase != other.ignoreCase)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "StemmerOverrideDictionaryConfig [dictionar=" + dictionaryFiles + "(md5="
                + new BigInteger(1, dictMd5).toString(16) + ", ignoreCase=" + ignoreCase + "]";
    }
    
    
    
}
