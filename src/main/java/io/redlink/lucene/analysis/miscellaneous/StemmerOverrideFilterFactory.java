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
package io.redlink.lucene.analysis.miscellaneous;


import io.redlink.lucene.analysis.util.ResourceCache;
import io.redlink.lucene.analysis.util.ResourceCache.ResourceRef;
import io.redlink.lucene.analysis.util.ResourceCache.ResourceType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter.StemmerOverrideMap;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;

/**
 * Factory for {@link StemmerOverrideFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_dicstem" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.StemmerOverrideFilterFactory" dictionary="dictionary.txt" ignoreCase="false"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * @since 3.1.0
 */
public class StemmerOverrideFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
  
  private static final ResourceType<StemmerOverrideDictionaryConfig, StemmerOverrideMap> RESOURCE_DICT = new ResourceType<>(
      StemmerOverrideFilterFactory.class.getName() + ".dictionary", StemmerOverrideDictionaryConfig.class, StemmerOverrideMap.class);

    
  private StemmerOverrideMap dictionary;
  private final String dictionaryFiles;
  private final boolean ignoreCase;
  
  private final ResourceCache cache;

  /**
   * Creates a new StemmerOverrideFilterFactory
   * @param args the arguments
   */
  public StemmerOverrideFilterFactory(Map<String,String> args) {
    super(args);
    dictionaryFiles = get(args, "dictionary");
    ignoreCase = getBoolean(args, "ignoreCase", false);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
    cache = ResourceCache.getInstance();
    cache.registerLoader(new ResourceCache.ResourceTypeLoader<StemmerOverrideDictionaryConfig, StemmerOverrideMap>(RESOURCE_DICT){
      @Override
      public StemmerOverrideMap load(ResourceRef<StemmerOverrideDictionaryConfig, StemmerOverrideMap> ref) throws IOException {
        StemmerOverrideDictionaryConfig config = ref.getKey();
        StemmerOverrideFilter.Builder builder = new StemmerOverrideFilter.Builder(config.isIgnoreCase());
        for (String file : config.getDictionaryFiles()) {
          List<String> list = getLines(config.getResourceLoader(), file.trim());
          for (String line : list) {
            String[] mapping = line.split("\t", 2);
            builder.add(mapping[0], mapping[1]);
          }
        }
        return builder.build();
      }});
    }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    if (dictionaryFiles != null) {
      List<String> files = splitFileNames(dictionaryFiles);
      if (files.size() > 0) {
        dictionary = cache.getResource(RESOURCE_DICT.createReference(new StemmerOverrideDictionaryConfig(loader, files, ignoreCase)));
      }
    }
  }

  public boolean isIgnoreCase() {
    return ignoreCase;
  }

  @Override
  public TokenStream create(TokenStream input) {
    return dictionary == null ? input : new StemmerOverrideFilter(input, dictionary);
  }
}
