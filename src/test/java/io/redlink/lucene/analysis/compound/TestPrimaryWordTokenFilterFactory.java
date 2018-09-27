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

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.Version;
import org.junit.Test;

/**
 * Tests the PrimaryWord token filter factory. Tests are based on the
 * <code>TestHyphenationCompoundWordTokenFilterFactory</code> unit tests
 * 
 * @author Rupert Westenthaler
 *
 */
public class TestPrimaryWordTokenFilterFactory extends BaseTokenStreamTestCase {

    @Test
    public void testWithDictionary() throws Exception {
        Reader reader = new StringReader("min veninde som er lidt af en læsehest");
        TokenStream stream = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        ((Tokenizer) stream).setReader(reader);
        stream = tokenFilterFactory("PrimaryWord", "hyphenator", "da_UTF8.xml", "dictionary",
                "da_compoundDictionary.txt").create(stream);
        // we expect læsehest -> hest
        assertTokenStreamContents(stream,
                new String[] { "min", "veninde", "som", "er", "lidt", "af", "en", "læsehest", "hest" },
                new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 0 });
    }

    @Test
    public void testHyphenationOnly() throws Exception {
        Reader reader = new StringReader("basketballkurv");
        TokenStream stream = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        ((Tokenizer) stream).setReader(reader);
        stream = tokenFilterFactory("PrimaryWord", "hyphenator", "da_UTF8.xml", "minSubwordSize", "2",
                "maxSubwordSize", "4").create(stream);

        assertTokenStreamContents(stream, new String[] { "basketballkurv","kurv" });
    }

    /** Test that bogus arguments result in exception */
    @Test
    public void testBogusArguments() throws Exception {
        IllegalArgumentException expected = expectThrows(IllegalArgumentException.class, () -> {
            tokenFilterFactory("PrimaryWord", "hyphenator", "da_UTF8.xml", "bogusArg", "bogusValue");
        });
        assertTrue(expected.getMessage().contains("Unknown parameters"));
    }

    /*
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * - - - - - - - - Helper Methods copied from
     * org.apache.lucene.analysis.util.BaseTokenStreamFactoryTestCase (because
     * they are not not available in the lucene-test-framework) - - - - - - - -
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */

    private AbstractAnalysisFactory analysisFactory(Class<? extends AbstractAnalysisFactory> clazz,
            Version matchVersion, ResourceLoader loader, String... keysAndValues) throws Exception {
        if (keysAndValues.length % 2 == 1) {
            throw new IllegalArgumentException("invalid keysAndValues map");
        }
        Map<String, String> args = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String previous = args.put(keysAndValues[i], keysAndValues[i + 1]);
            assertNull("duplicate values for key: " + keysAndValues[i], previous);
        }
        if (matchVersion != null) {
            String previous = args.put("luceneMatchVersion", matchVersion.toString());
            assertNull("duplicate values for key: luceneMatchVersion", previous);
        }
        AbstractAnalysisFactory factory = null;
        try {
            factory = clazz.getConstructor(Map.class).newInstance(args);
        } catch (InvocationTargetException e) {
            // to simplify tests that check for illegal parameters
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            } else {
                throw e;
            }
        }
        if (factory instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) factory).inform(loader);
        }
        return factory;
    }

    /**
     * Returns a fully initialized TokenFilterFactory with the specified name
     * and key-value arguments. {@link ClasspathResourceLoader} is used for
     * loading resources, so any required ones should be on the test classpath.
     */
    private TokenFilterFactory tokenFilterFactory(String name, Version version, String... keysAndValues)
            throws Exception {
        return tokenFilterFactory(name, version, new ClasspathResourceLoader(getClass()), keysAndValues);
    }

    /**
     * Returns a fully initialized TokenFilterFactory with the specified name
     * and key-value arguments. {@link ClasspathResourceLoader} is used for
     * loading resources, so any required ones should be on the test classpath.
     */
    private TokenFilterFactory tokenFilterFactory(String name, String... keysAndValues) throws Exception {
        return tokenFilterFactory(name, Version.LATEST, keysAndValues);
    }

    /**
     * Returns a fully initialized TokenFilterFactory with the specified name,
     * version, resource loader, and key-value arguments.
     */
    private TokenFilterFactory tokenFilterFactory(String name, Version matchVersion, ResourceLoader loader,
            String... keysAndValues) throws Exception {
        return (TokenFilterFactory) analysisFactory(TokenFilterFactory.lookupClass(name), matchVersion, loader,
                keysAndValues);
    }
}
