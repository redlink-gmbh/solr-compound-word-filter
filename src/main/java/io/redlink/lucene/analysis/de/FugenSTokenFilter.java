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
package io.redlink.lucene.analysis.de;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

/**
 * A {@link org.apache.lucene.analysis.TokenFilter} that removes the
 * Fugen-S used in German with compound words.
 * 
 * Examles: <ul>
 * <li> Gesundheit(s)vorsorge
 * <li> Sicherheit(s)- und Ordnung(s)dienst
 * </ul>
 * 
 * Typically this could be done as part of a decomposer, but usages
 * as shown by the 2nd example require to have this as an own
 * {@link TokenFilter}.
 * 
 * The rule implemented by this {@link TokenFilter} is based on
 * the article <a href="http://www.spiegel.de/kultur/zwiebelfisch/zwiebelfisch-der-gebrauch-des-fugen-s-im-ueberblick-a-293195.html">
 * Der Gebrauch des Fugen-s im Überblick</a>
 * 
 * This {@link TokenFilter} will not process Token with <code>{@link KeywordAttribute#isKeyword()} == true</code> to
 * prevent processing of words that are in a dictionary
 * 
 * @author Rupert Westenthaler
 */
public final class FugenSTokenFilter extends TokenFilter {
    
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

    private final static int MIN_LENGTH = 4; //shortest ending ('en') + 's' + at least one char of the word
    /**
     * The {@link #ENDINGS} sorted by the last character to allow for a more efficient lookup
     */
    static Map<Character, Set<CharSequence>> ENDINGS_TREE;
    static {
        //based on http://www.spiegel.de/kultur/zwiebelfisch/zwiebelfisch-der-gebrauch-des-fugen-s-im-ueberblick-a-293195.html
        Set<String> endings = new HashSet<>();
        //all the endings where a Fugen-S is present
        endings.addAll(Arrays.asList("tum", "ling", "ion", "tät", "keit", "schaft", "sicht", "ung", "en"));
        //all the endings where a Fugen-S is present even if they have 'die' as article
        endings.addAll(Arrays.asList("ion", "tät", "heit", "keit", "schaft", "sicht", "ung"));
        //store the endings in a map with the first char as key to allow lookups based on the 2nd last char for tokens ending with an s
        Map<Character, Set<CharSequence>> map = new HashMap<>();
        endings.forEach(e -> map.computeIfAbsent(e.charAt(e.length()-1), k -> new HashSet<>()).add(e));
        ENDINGS_TREE = Collections.unmodifiableMap(map);
    }
    
    /**
     * Creates a new {@link FugenSTokenFilter} instance.
     *
     * @param input
     *            the {@link org.apache.lucene.analysis.TokenStream} to process
     */
    public FugenSTokenFilter(TokenStream input) {
        super(input);
    }
    
    @Override
    public boolean incrementToken() throws IOException {
      if (input.incrementToken()) {
        if (!keywordAttr.isKeyword()) {
          final int length = termAtt.length();
            if(length >= MIN_LENGTH){
            if(Character.toLowerCase(termAtt.charAt(termAtt.length()-1)) == 's'){
              Set<CharSequence> endingsMatches = ENDINGS_TREE.get(Character.valueOf(termAtt.charAt(termAtt.length() - 2)));
              if(endingsMatches != null && endingsMatches.stream()
                  .filter(e -> e.length() < termAtt.length() - 1) //token to short for this ending
                  .anyMatch(e -> e.equals(termAtt.subSequence(length - 1 - e.length(), length - 1)))){
                termAtt.setLength(length -1); //we found match so consider the tailing s as Fugen-S
              }
            }
          }
        }
        return true;
      } else {
        return false;
      }
    }
}
