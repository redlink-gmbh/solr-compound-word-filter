# Redlink Compound Word Filters

## HyphenationCompoundWordTokenFilter

Redlink version of the `solr.HyphenationCompoundWordTokenFilterFactory` with the fix
for [LUCENE-8183](https://issues.apache.org/jira/browse/LUCENE-8183)

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
    <filter class="PrimaryWordTokenFilterFactory" hyphenator="hyphenator.xml" encoding="UTF-8"
        dictionary="dictionary.txt" minWordSize="5" minSubwordSize="2" maxSubwordSize="15"
        onlyLongestMatch="true"/>
  </analyzer>
</fieldType>
 ``` 
 
 ## License
 
 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
 