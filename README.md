# Redlink Compound Word Filters

[![Build Status](https://travis-ci.org/redlink-gmbh/solr-compound-word-filter.svg?branch=master)](https://travis-ci.org/redlink-gmbh/solr-compound-word-filter)

## HyphenationCompoundWordTokenFilter

Redlink version of the `solr.HyphenationCompoundWordTokenFilterFactory` with the fix for [LUCENE-8183](https://issues.apache.org/jira/browse/LUCENE-8183) and support for the [`epenthesis`](https://en.wikipedia.org/wiki/Epenthesis) parameter that allows to configure characters added between subwords in compound words.

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
 
 ## License
 
 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
 