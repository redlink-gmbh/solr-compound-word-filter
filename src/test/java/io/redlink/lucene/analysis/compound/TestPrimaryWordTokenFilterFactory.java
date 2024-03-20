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
import org.apache.lucene.analysis.BaseTokenStreamFactoryTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.junit.Test;

/**
 * Tests the PrimaryWord token filter factory. Tests are based on the
 * <code>TestHyphenationCompoundWordTokenFilterFactory</code> unit tests
 * 
 * @author Rupert Westenthaler
 *
 */
public class TestPrimaryWordTokenFilterFactory extends BaseTokenStreamFactoryTestCase {

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

}
