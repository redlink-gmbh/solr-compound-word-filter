/*
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
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.compound.CompoundWordTokenFilterBase;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.xml.sax.InputSource;

import io.redlink.lucene.analysis.util.ResourceCache;
import io.redlink.lucene.analysis.util.ResourceCache.ResourceRef;
import io.redlink.lucene.analysis.util.ResourceCache.ResourceType;

/**
 * Factory for {@link HyphenationCompoundWordTokenFilter}.
 * <p>
 * This factory accepts the following parameters:
 * <ul>
 *  <li><code>hyphenator</code> (mandatory): path to the FOP xml hyphenation pattern. 
 *  See <a href="http://offo.sourceforge.net/hyphenation/">http://offo.sourceforge.net/hyphenation/</a>.
 *  <li><code>encoding</code> (optional): encoding of the xml hyphenation file. defaults to UTF-8.
 *  <li><code>dictionary</code> (optional): dictionary of words. defaults to no dictionary.
 *  <li><code>epenthesis</code> (optional): comma separated list of epenthesis (e.g. 's' for German). The
 *  first matching value is used. So ordering is important. defaults to emtpy
 *  <li><code>minWordSize</code> (optional): minimal word length that gets decomposed. defaults to 5.
 *  <li><code>minSubwordSize</code> (optional): minimum length of subwords. defaults to 2.
 *  <li><code>maxSubwordSize</code> (optional): maximum length of subwords. defaults to 15.
 *  <li><code>onlyLongestMatch</code> (optional): if true, adds only the longest matching subword 
 *    to the stream. defaults to false.
 * </ul>
 * <br>
 * <pre class="prettyprint">
 * &lt;fieldType name="text_hyphncomp" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.HyphenationCompoundWordTokenFilterFactory" hyphenator="hyphenator.xml" encoding="UTF-8"
 *         dictionary="dictionary.txt" epenthesis="s", minWordSize="5" minSubwordSize="2" maxSubwordSize="15" onlyLongestMatch="false"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 * @see HyphenationCompoundWordTokenFilter
 * @since 3.1.0
 */
public class HyphenationCompoundWordTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

  private static final ResourceType<HyphDictionaryConfig, CharArraySet> RESOURCE_DICT = new ResourceType<>(
          HyphenationCompoundWordTokenFilterFactory.class.getName() + ".dictionary", HyphDictionaryConfig.class, CharArraySet.class);
  private static final ResourceType<HyphenatorConfig, HyphenationTree> RESOURCE_HYPH = new ResourceType<>(
          HyphenationCompoundWordTokenFilterFactory.class.getName() + ".hyphenator", HyphenatorConfig.class, HyphenationTree.class);

  private CharArraySet dictionary;
  private HyphenationTree hyphenator;
  private final String dictFile;
  private final String hypFile;
  private final String encoding;
  private final boolean ignoreCase;
  private final int minWordSize;
  private final int minSubwordSize;
  private final int maxSubwordSize;
  private final boolean onlyLongestMatch;
  private final boolean noSubMatches;
  private final boolean noOverlappingMatches;
  private final String[] epenthesis;

  private final ResourceCache cache;

  /** Creates a new HyphenationCompoundWordTokenFilterFactory */
  public HyphenationCompoundWordTokenFilterFactory(Map<String, String> args) {
    super(args);
    dictFile = get(args, "dictionary");
    encoding = get(args, "encoding");
    hypFile = require(args, "hyphenator");
    ignoreCase = getBoolean(args, "ignoreCase", false);
    epenthesis = get(args, "epenthesis","").split(",");
    minWordSize = getInt(args, "minWordSize", CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE);
    minSubwordSize = getInt(args, "minSubwordSize", CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE);
    maxSubwordSize = getInt(args, "maxSubwordSize", CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE);
    onlyLongestMatch = getBoolean(args, "onlyLongestMatch", false);
    noSubMatches = getBoolean(args, "noSubMatches", false);
    noOverlappingMatches = getBoolean(args, "noOverlappingMatches", false);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
    cache = ResourceCache.getInstance();
    cache.registerLoader(new ResourceCache.ResourceTypeLoader<HyphDictionaryConfig, CharArraySet>(RESOURCE_DICT){
      @Override
      public CharArraySet load(ResourceRef<HyphDictionaryConfig, CharArraySet> ref) throws IOException {
        HyphDictionaryConfig config = ref.getKey();
        return getWordSet(config.getResourceLoader(),
                config.getDictionaryFiles().stream().collect(Collectors.joining(",")),
                config.isIgnoreCase());
      }});
    cache.registerLoader(new ResourceCache.ResourceTypeLoader<HyphenatorConfig, HyphenationTree>(RESOURCE_HYPH){
      @Override
      public HyphenationTree load(ResourceRef<HyphenatorConfig, HyphenationTree> ref) throws IOException {
        HyphenatorConfig config = ref.getKey();
        // TODO: Broken, because we cannot resolve real system id
        // ResourceLoader should also supply method like ClassLoader to get resource URL
        try(InputStream stream = config.getResourceLoader().openResource(config.getHyphFile())){
          final InputSource is = new InputSource(stream);
          is.setEncoding(config.getEncoding()); // if it's null let xml parser decide
          is.setSystemId(config.getHyphFile());
          return HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
        }
      }});
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    dictionary = cache.getResource(RESOURCE_DICT.createReference(new HyphDictionaryConfig(
            loader, splitFileNames(dictFile), ignoreCase)));
    hyphenator = cache.getResource(RESOURCE_HYPH.createReference(new HyphenatorConfig(
            loader, hypFile, encoding)));
  }

  @Override
  public TokenFilter create(TokenStream input) {
    return new HyphenationCompoundWordTokenFilter(input, hyphenator, dictionary, epenthesis, minWordSize, minSubwordSize, maxSubwordSize, onlyLongestMatch, noSubMatches, noOverlappingMatches);
  }
}
