/*
 * Copyright 2018 redlink GmbH
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.compound.CompoundWordTokenFilterBase;
import org.apache.lucene.analysis.compound.hyphenation.Hyphenation;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.xml.sax.InputSource;

/**
 * A {@link org.apache.lucene.analysis.TokenFilter} that decomposes compound
 * words found in many Germanic languages to find the primary word.
 *
 * "Donaudampfschiff" is decomposed to Donau, dampf, schiff. The primary word is
 * expected to be the last part - 'schiff' in this example.
 * 
 * In case the configured dictionary contains 'dampfschiff' the primary word
 * would be 'dampfschiff' as it is the longest match. But in case of
 * <code>onlyLongestMatch=false</code> both 'dampfschiff' and 'schiff would be
 * added as tokens
 * 
 * While this filter works without a dictionary it is <b>highly</b> recommended
 * to provide one as otherwise results would be the the last syllable of the
 * token what is not very helpful in most situations
 * 
 * @author Rupert Westenthaler
 */
public class PrimaryWordTokenFilter extends CompoundWordTokenFilterBase {

    public static final boolean DEFAULT_ONLY_LONGEST_MATCH = true;

    private HyphenationTree hyphenator;

    /**
     * Creates a new {@link PrimaryWordTokenFilter} instance.
     *
     * @param input
     *            the {@link org.apache.lucene.analysis.TokenStream} to process
     * @param hyphenator
     *            the hyphenation pattern tree to use for hyphenation
     * @param dictionary
     *            the word dictionary to match against.
     */
    public PrimaryWordTokenFilter(TokenStream input, HyphenationTree hyphenator, CharArraySet dictionary) {
        this(input, hyphenator, dictionary, DEFAULT_MIN_WORD_SIZE, DEFAULT_MIN_SUBWORD_SIZE, 
                DEFAULT_MAX_SUBWORD_SIZE, DEFAULT_ONLY_LONGEST_MATCH);
    }

    /**
     * Creates a new {@link PrimaryWordTokenFilter} instance.
     *
     * @param input
     *            the {@link org.apache.lucene.analysis.TokenStream} to process
     * @param hyphenator
     *            the hyphenation pattern tree to use for hyphenation
     * @param dictionary
     *            the word dictionary to match against.
     * @param minWordSize
     *            only words longer than this get processed
     * @param minSubwordSize
     *            only subwords longer than this get to the output stream
     * @param maxSubwordSize
     *            only subwords shorter than this get to the output stream
     */
    public PrimaryWordTokenFilter(TokenStream input, HyphenationTree hyphenator, CharArraySet dictionary,
            int minWordSize, int minSubwordSize, int maxSubwordSize, boolean onlyLongestMatch) {
        super(input, dictionary, minWordSize, minSubwordSize, maxSubwordSize, onlyLongestMatch);

        this.hyphenator = hyphenator;
    }

    /**
     * Create a hyphenator tree
     *
     * @param hyphenationFilename
     *            the filename of the XML grammar to load
     * @return An object representing the hyphenation patterns
     * @throws java.io.IOException
     *             If there is a low-level I/O error.
     */
    public static HyphenationTree getHyphenationTree(String hyphenationFilename) throws IOException {
        return getHyphenationTree(new InputSource(hyphenationFilename));
    }

    /**
     * Create a hyphenator tree
     *
     * @param hyphenationSource
     *            the InputSource pointing to the XML grammar
     * @return An object representing the hyphenation patterns
     * @throws java.io.IOException
     *             If there is a low-level I/O error.
     */
    public static HyphenationTree getHyphenationTree(InputSource hyphenationSource) throws IOException {
        HyphenationTree tree = new HyphenationTree();
        tree.loadPatterns(hyphenationSource);
        return tree;
    }

    @Override
    protected void decompose() {
        // if the token is in the dictionary the token is the primary word
        if (onlyLongestMatch && dictionary != null && (dictionary.contains(termAtt.buffer(), 0, termAtt.length())
                || termAtt.length() > 1 && dictionary.contains(termAtt.buffer(), 0, termAtt.length() - 1))) {
            return; // the whole token is in the dictionary - do not decompose
        }

        // get the hyphenation points
        Hyphenation hyphens = hyphenator.hyphenate(termAtt.buffer(), 0, termAtt.length(), 1, 1);
        // No hyphen points found -> exit
        if (hyphens == null) {
            return;
        }
        int maxSubwordSize = Math.min(this.maxSubwordSize, termAtt.length() - 1);

        final int[] hyp = hyphens.getHyphenationPoints();

        int end = hyp[hyp.length - 1];

        for (int i = 0; i < hyp.length; i++) {
            int start = hyp[i];
            int partLength = end - start;

            // if the part is longer than maxSubwordSize we
            // are done with this round
            if (partLength > maxSubwordSize) {
                continue;
            }

            // we only put subwords to the token stream
            // that are longer than minPartSize
            if (partLength < this.minSubwordSize) {
                break;
            }

            // check the dictionary
            if (dictionary == null || dictionary.contains(termAtt.buffer(), start, partLength)) {
                tokens.add(new CompoundToken(start, partLength));
                if (onlyLongestMatch) {
                    break;
                }
            } else if (partLength > this.minSubwordSize
                    && dictionary.contains(termAtt.buffer(), start, partLength - 1)) {
                // check the dictionary again with a word that is one character
                // shorter to avoid problems with genitive 's characters and
                // other binding characters
                tokens.add(new CompoundToken(start, partLength - 1));
                if (onlyLongestMatch) {
                    break;
                }
            } // else dictionary is present but does not contain the part
        }
    }
}
