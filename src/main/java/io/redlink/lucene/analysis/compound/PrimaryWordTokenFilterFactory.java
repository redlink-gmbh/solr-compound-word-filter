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
import java.util.Map;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.compound.CompoundWordTokenFilterBase;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.xml.sax.InputSource;

/**
 * Factory for {@link PrimaryWordTokenFilter}.
 * <p>
 * This factory accepts the following parameters:
 * <ul>
 * <li><code>hyphenator</code> (mandatory): path to the FOP xml hyphenation
 * pattern. See <a href=
 * "http://offo.sourceforge.net/hyphenation/">http://offo.sourceforge.net/hyphenation/</a>.
 * <li><code>encoding</code> (optional): encoding of the xml hyphenation file.
 * defaults to UTF-8.
 * <li><code>dictionary</code> (optional, recommended): dictionary of words.
 * defaults to no dictionary.
 * <li><code>minWordSize</code> (optional): minimal word length that gets
 * decomposed. defaults to 5.
 * <li><code>minSubwordSize</code> (optional): minimum length of subwords.
 * defaults to 2.
 * <li><code>maxSubwordSize</code> (optional): maximum length of subwords.
 * defaults to 15.
 * <li><code>onlyLongestMatch</code> (optional): if true, only the longest
 * matching word is added as primary word to the stream. defaults to true.
 * </ul>
 * <br>
 * 
 * <pre class="prettyprint">
 * &lt;fieldType name="text_hyphncomp" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.PrimaryWordTokenFilterFactory" hyphenator="hyphenator.xml" encoding="UTF-8"
 *         dictionary="dictionary.txt" minWordSize="5" minSubwordSize="2" maxSubwordSize="15" onlyLongestMatch="true"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 *
 * @see PrimaryWordTokenFilter
 * @author Rupert Westenthaler
 * @lucene.spi {@value #NAME}
 */
public class PrimaryWordTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

    /** SPI name */
    public static final String NAME = "primaryWord";

    private CharArraySet dictionary;
    private HyphenationTree hyphenator;
    private final String dictFile;
    private final String hypFile;
    private final String encoding;
    private final int minWordSize;
    private final int minSubwordSize;
    private final int maxSubwordSize;
    private final boolean onlyLongestMatch;

    /** Default ctor for compatibility with SPI */
    public PrimaryWordTokenFilterFactory() {
        throw defaultCtorException();
    }

    /**
     * Creates a new HyphenationCompoundWordTokenFilterFactory
     * @param args the arguments
     */
    public PrimaryWordTokenFilterFactory(Map<String, String> args) {
        super(args);
        dictFile = get(args, "dictionary");
        encoding = get(args, "encoding");
        hypFile = require(args, "hyphenator");
        minWordSize = getInt(args, "minWordSize", CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE);
        minSubwordSize = getInt(args, "minSubwordSize", CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE);
        maxSubwordSize = getInt(args, "maxSubwordSize", CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE);
        onlyLongestMatch = getBoolean(args, "onlyLongestMatch", false);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        InputStream stream = null;
        try {
            // the dictionary can be empty.
            if (dictFile != null) {
                dictionary = getWordSet(loader, dictFile, false);
            }
            // TODO: Broken, because we cannot resolve real system id
            // ResourceLoader should also supply method like ClassLoader to get
            // resource URL
            stream = loader.openResource(hypFile);
            final InputSource is = new InputSource(stream);
            is.setEncoding(encoding); // if it's null let xml parser decide
            is.setSystemId(hypFile);
            hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
        } finally {
            IOUtils.closeWhileHandlingException(stream);
        }
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new PrimaryWordTokenFilter(input, hyphenator, dictionary, minWordSize, minSubwordSize, 
                maxSubwordSize, onlyLongestMatch);
    }
}
