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
package io.redlink.lucene.analysis.de;

import java.util.Map;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;

/**
 * Factory for {@link FugenSTokenFilter}.
 * <p>
 * This factory accepts no parameters:
 * 
 * <pre class="prettyprint">
 * &lt;fieldType name="text_de" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.FugenSTokenFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 *
 * @see FugenSTokenFilter
 * @author Rupert Westenthaler
 * @lucene.spi {@value #NAME}
 */
public class FugenSTokenFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "fugenS";

    /** Default ctor for compatibility with SPI */
    public FugenSTokenFilterFactory() {
        super();
    }

    /**
     * Creates a new HyphenationCompoundWordTokenFilterFactory
     * @param args the arguments
     */
    public FugenSTokenFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new FugenSTokenFilter(input);
    }
}
