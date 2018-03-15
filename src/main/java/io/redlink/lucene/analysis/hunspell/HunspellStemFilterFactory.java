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
package io.redlink.lucene.analysis.hunspell;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.hunspell.HunspellStemFilter;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;

import io.redlink.lucene.analysis.util.ResourceCache;
import io.redlink.lucene.analysis.util.ResourceCache.ResourceRef;
import io.redlink.lucene.analysis.util.ResourceCache.ResourceType;

/**
 * Variant of the <code>solr.HunspellStemFilterFactory</code> that uses a
 * cache to load the {@link Dictionary}. This avoids the Issue described
 * by <a href="https://issues.apache.org/jira/browse/SOLR-3443">SOLR-3443</a>.
 * <p>
 * The {@link ResourceCache} is used to cache the Hunspell {@link Dictionary 
 * Dictionaries} to ensure that the same Dictionary used by different Fields
 * and/or different Cores is only loaded once into memory
 * <p>
 * TokenFilterFactory that creates instances of {@link HunspellStemFilter}.
 * Example config for British English:
 * <pre class="prettyprint">
 * &lt;filter class=&quot;io.redlink.lucene.analysis.hunspell.HunspellStemFilterFactory&quot;
 *         dictionary=&quot;en_GB.dic,my_custom.dic&quot;
 *         affix=&quot;en_GB.aff&quot; 
 *         ignoreCase=&quot;false&quot;
 *         longestOnly=&quot;false&quot; /&gt;</pre>
 * Both parameters dictionary and affix are mandatory.
 * Dictionaries for many languages are available through the OpenOffice project.
 * 
 * See <a href="http://wiki.apache.org/solr/Hunspell">http://wiki.apache.org/solr/Hunspell</a>
 * @lucene.experimental
 * @since 3.5.0
 */
public class HunspellStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    
  private static final ResourceType<HunspellDictionaryConfig, Dictionary> RESOURCE_DICT = new ResourceType<>(
          HunspellStemFilterFactory.class.getName() + ".dictionary", HunspellDictionaryConfig.class, Dictionary.class);
    
  private static final String PARAM_DICTIONARY    = "dictionary";
  private static final String PARAM_AFFIX         = "affix";
  // NOTE: this one is currently unused?:
  private static final String PARAM_RECURSION_CAP = "recursionCap";
  private static final String PARAM_IGNORE_CASE   = "ignoreCase";
  private static final String PARAM_LONGEST_ONLY  = "longestOnly";

  private final String dictionaryFiles;
  private final String affixFile;
  private final boolean ignoreCase;
  private final boolean longestOnly;
  private Dictionary dictionary;
  
  private final ResourceCache cache;
  
  /** Creates a new HunspellStemFilterFactory */
  public HunspellStemFilterFactory(Map<String,String> args) {
    super(args);
    dictionaryFiles = require(args, PARAM_DICTIONARY);
    affixFile = get(args, PARAM_AFFIX);
    ignoreCase = getBoolean(args, PARAM_IGNORE_CASE, false);
    longestOnly = getBoolean(args, PARAM_LONGEST_ONLY, false);
    // this isnt necessary: we properly load all dictionaries.
    // but recognize and ignore for back compat
    getBoolean(args, "strictAffixParsing", true);
    // this isn't necessary: multi-stage stripping is fixed and 
    // flags like COMPLEXPREFIXES in the data itself control this.
    // but recognize and ignore for back compat
    getInt(args, "recursionCap", 0);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
    
    cache = ResourceCache.getInstance();
    cache.registerLoader(new ResourceCache.ResourceTypeLoader<HunspellDictionaryConfig, Dictionary>(RESOURCE_DICT){
      @Override
      public Dictionary load(ResourceRef<HunspellDictionaryConfig, Dictionary> ref) throws IOException {
        HunspellDictionaryConfig config = ref.getKey();
        ResourceLoader loader = config.getResourceLoader();
        InputStream affix = null;
        List<InputStream> dictionaries = new ArrayList<>();
        try {
          dictionaries = new ArrayList<>();
          for (String file : config.getDictionaryFiles()) {
            dictionaries.add(loader.openResource(file));
          }
          affix = loader.openResource(config.getAffixFile());
          
          
          Path tempPath = Files.createTempDirectory(getDefaultTempDir(), "Hunspell");
          try (Directory tempDir = FSDirectory.open(tempPath)) {
            return new Dictionary(tempDir, "hunspell", affix, dictionaries, ignoreCase);
          } finally {
            IOUtils.rm(tempPath); 
          }
        } catch (ParseException e) {
          throw new IOException("Unable to load hunspell data! [dictionary=" + dictionaries + ",affix=" + affixFile + "]", e);
        } finally {
          IOUtils.closeWhileHandlingException(affix);
          IOUtils.closeWhileHandlingException(dictionaries);
        }
      }
    });
  }

  @Override
  public void inform(ResourceLoader loader) throws IOException {
    dictionary = cache.getResource(RESOURCE_DICT.createReference(
            new HunspellDictionaryConfig(loader, affixFile, splitFileNames(dictionaryFiles), ignoreCase)));
  }

  @Override
  public TokenStream create(TokenStream tokenStream) {
    return new HunspellStemFilter(tokenStream, dictionary, true, longestOnly);
  }
  
  /*
   * NOTE: copied from Dictionary as it has package level visibility 
   */
  private static Path DEFAULT_TEMP_DIR;

  /** Used by test framework */
  public static void setDefaultTempDir(Path tempDir) {
    DEFAULT_TEMP_DIR = tempDir;
  }

  /**
   * Returns the default temporary directory. By default, java.io.tmpdir. If not accessible
   * or not available, an IOException is thrown
   */
  synchronized static Path getDefaultTempDir() throws IOException {
    if (DEFAULT_TEMP_DIR == null) {
      // Lazy init
      String tempDirPath = System.getProperty("java.io.tmpdir");
      if (tempDirPath == null)  {
        throw new IOException("Java has no temporary folder property (java.io.tmpdir)?");
      }
      Path tempDirectory = Paths.get(tempDirPath);
      if (Files.isWritable(tempDirectory) == false) {
        throw new IOException("Java's temporary folder not present or writeable?: " 
                              + tempDirectory.toAbsolutePath());
      }
      DEFAULT_TEMP_DIR = tempDirectory;
    }

    return DEFAULT_TEMP_DIR;
  }

}
