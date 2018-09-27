# Redlink Lucene Analysis Components

[![Build Status](https://travis-ci.org/redlink-gmbh/solr-compound-word-filter.svg?branch=master)](https://travis-ci.org/redlink-gmbh/solr-compound-word-filter)
[![Maven Central](https://img.shields.io/maven-central/v/io.redlink.solr/compound-word-filter.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.redlink.solr%22)
[![Sonatype (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.redlink.solr/compound-word-filter.svg)](https://oss.sonatype.org/#nexus-search;gav~io.redlink.solr~compound-word-filter~~~)

## HyphenationCompoundWordTokenFilter

Redlink version of the `solr.HyphenationCompoundWordTokenFilterFactory` with the fix for [LUCENE-8183](https://issues.apache.org/jira/browse/LUCENE-8183) and support for the [`epenthesis`](https://en.wikipedia.org/wiki/Epenthesis) parameter that allows to configure characters added between subwords in compound words.

### `ignoreCase` Parameter

The `solr.HyphenationCompoundWordTokenFilterFactory` does not support the `ignoreCase` parameter.

The typicall workaround is to 

1. add the `solr.LowerCaseFilterFactory` before and
2. convert the dictionary to lower case

However their are some cases where this workaround does not work as some other TokenFilter do change the case of tokens. One example is the `solr.StemmerOverrideFilterFactory` when used with `ignoreCase="true"` and an case sensitive dictionary. In those setting it is required to place the `solr.LowerCaseFilterFactory` afterwards as otherwise one would risk having mixed case tokens in the token stream. 

The `ignoreCase="true"` option solves this issue as it allows this factory to work work in a case insensitive manner before the `solr.LowerCaseFilterFactory`

### `epenthesis` Parameter

If the current part is not in the dictionary (and a dictionary is present) the original `solr.HyphenationCompoundWordTokenFilterFactory` always tries to remove the last character to makes an additional dictionary lookup.

This version instead checks if current part end with a configured epenthesis. Only if this is the case the epenthesis is striped of the part and an additional dictionary lookup is made.

This allows for a fine control over this functionality preventing unexpected matches in the dictionary.

To keep backward compatibility it no `epenthesis` is configured the old behavior of stripping the last char is kept.  

__German__

For German epenthesis are typically called 'Fügenlaute'. Based on [1](https://www.linguistik.hu-berlin.de/de/institut/professuren/korpuslinguistik/lehre/alte_jahrgaenge/ws-2003/hs-phaenomene-deutsch/pdf/phaeno-kp-fugen.pdf) about 27% of all compound words to use a 'Fügenlaute' and about 15% of those do use '-[e]s' and an additioanl 9% '-[e]n'. 

Based on this setting `epenthesis="es,s,en,n"` but as `en,n` typically also represents the plural and will theirfore be in the dictionary it is sufficient to set `epenthesis="es,s"`

To improve results it is important to ensure that words including the 'Fügenlaut' `s` are NOT in the dictionary as this will result that those will be added as tokens. Stemmers will NOT remove those `s` at the end and typical queries searches will NOT match those.

To give an example: Assuming the Dictionary contains 'ausbildungs', 'ausbildung' and 'leiter' the word 'Ausbildungsleiter' will be decomposed to 'ausbildungs' and 'leiter'. If the stemmer does not remove the tailing 's' queries for 'Ausbildung' will not match the decomposed word.

## PrimaryWordTikenFilter

A `TokenFilter` that decomposes compound words found in many Germanic languages to find the primary word. Inspired by this description of [Primary Word Detection](https://developer.s24.com/blog/german_stemming_like_a_pro.html#primary-word-detection-extending-the-token-filter).

"Donaudampfschiff" is decomposed to Donau, dampf, schiff. The primary word is expected to be the last part - 'schiff' in this example. 

In case the configured dictionary contains 'dampfschiff' the primary word would be 'dampfschiff' as it is the longest match. But in case of `onlyLongestMatch=false` both 'dampfschiff' and 'schiff would be added as tokens.

While this filter works without a dictionary it is <b>highly</b> recommended to provide one as otherwise results would be the the last syllable of the token what is not very helpful in most situations

The factory accepts the following parameters:
 * `hyphenator` _(mandatory)_: path to the FOP xml hyphenation pattern. See [http://offo.sourceforge.net/hyphenation/](offo.sourceforge.net/hyphenation/).
* `encoding` _(optional)_: encoding of the xml hyphenation file. defaults to UTF-8.
* `dictionary` (optional, recommended): dictionary of words. defaults to no dictionary.
* `minWordSize` _(optional)_: minimal word length that gets decomposed. defaults to 5.
* `minSubwordSize` _(optional)_: minimum length of subwords. defaults to 2.
* `maxSubwordSize` _(optional)_: maximum length of subwords. defaults to 15.
* `onlyLongestMatch` _(optional)_: if true, only the longest matching word is added as primary word to the stream. defaults to true.

```
<fieldType name="text_hyphncomp" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="io.redlink.lucene.analysis.compound.PrimaryWordTokenFilterFactory"
        hyphenator="hyphenator.xml" encoding="UTF-8" dictionary="dictionary.txt" 
        minWordSize="5" minSubwordSize="2" maxSubwordSize="15" onlyLongestMatch="true"/>
  </analyzer>
</fieldType>
 ``` 
 
 ## ResourceCache
 
The ResourceCache allows to share memory intensive reosurces between `TokenFilter`. This is especially useful for `TokenFilter` that use in-memory representations of dictionaries such as the `HunspellStemFilter`, `StemmOverrideFilter` or the CompoundWordFilter

With typical Solr configurations multiple instances with matching configuration are created. Typical examples are: 

* index and query time Analyzer often use the same TokenFilter configuration
* different `TextField` may use the same `TokenFilter` configuration
* all cores created for the same ConfigSet will instantiate the same TokenFilters (if shared schema is not enabled in the `solr.xml`)
* Even different Cores and/or ConfigSets might use the same TikenFilter configurations

So in a Setting with two German Language Fields both using the same Hunspell stemmer configuration for both index and query time analyzers and 20 cores the Hunspell dictionary would be loaded 80 times in Memory!

The `HunspellStemFilter` is the best example as it has the highest memory requirements. [SOLR-3443](https://issues.apache.org/jira/browse/SOLR-3443) is releated and describes exactly this problem.

The `ResourceCache` provides a solution to this problem as it provides a framework that allows TokenFilter factories to get resources from a cache.

TokenFilterFactory need to be adapted to make use of the `ResourceCache`. Because of that this module includes adapted versions of the `HunspellSemmFilterFactory` and the `StemmerOverwriteFilterFactory`. In addition all the other FilterFactory implementations provided by this module do also support the `ResourceCache`

### Solr Classpath and the ResourceCache

The `ResourceCache` uses a singelton pattern. For the JVM this means one instance per `Classloader`.  Solr `ResourceLoader` builds Solr Core specific Classloader for all resources from the Core/ConfigSet specific `lib` folder.

Because of this if this modules `jar` is provided via the cores `lib` every Core will have its own `ResourceCache` instance and resources will only be shared within a core. This will limit the functionality.

With the above Example the German Hunspell dictionary would be loaded 20 times instead of the 80 times with no `ResourceCache`.

To share Resources with multiple cores one needs to provide the `jar` in the `sharedLib` folder (a configuration in the `solr.xml`

### Implementation Notes

* As Lucene does not define a lifecycle for `TokenFilterFactory` components 
this cache can not use `RefCount`. Instead it uses `WeakReference` 
to hold in Resources. So it is up to the garbage collector to decide when a Resource
is no longer needed
* The cache uses the Factory Pattern to avoid extending the ResourceLoader or
adding a ResouceCacheAware callback.
* `ResourceType` definition provide type savety and `ResourceTypeLoader`
implement the actual loading od resources (if not cached).
* The `ResourceRef` has two responsibilities: First it iss used as key for 
the cache so it define equivalence for Resources. Second it needs to provide all the
information required by the `ResourceTypeLoader` to load the referenced resource
* Currently their is no way to enable/disable the `ResourceCache`. Using the Redlink versions of the TokenFilter factories will enable the usage of the `ResourceCache`

 ## License
 
 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
 