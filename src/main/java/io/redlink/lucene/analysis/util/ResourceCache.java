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

package io.redlink.lucene.analysis.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.hunspell.HunspellStemFilter;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.RefCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cache that allows to share Memory Intensive Reosurces between {@link TokenFilter}.
 * It is typical for Solr/Lucene condiguration to define multiple {@link TokenFilter}
 * with the exact same configuration. <p>
 * <p>
 * Some Examples: <ul>
 * <li> index and query time Analyzer often use the same TokenFilter configuration
 * <li> different {@link TextField}s often use the same {@link TokenFilter} configuration
 * <li> all cores created for the same ConfigSet will instantiate the same TokenFilters
 * <li>, Multiple Cores and/or ConfigSets might use the same TikenFilter configurations
 * </ul>
 * <p>
 * So it is typical that their are many instances for resources used by TokenFilters.
 * While this does not matter for smaller resoruces such as the stopword list that are
 * only some kByte in size this does get a real problem for bigger Dictionaries.
 * <p>
 * The most affected component is the {@link HunspellStemFilter} as it uses constructs
 * an in-memory representation of the stemming dictionary that typically has a size of
 * several MByte (see <a href="https://issues.apache.org/jira/browse/SOLR-3443">SOLR-3443</a>
 * for more information)
 * <p>
 * The {@link ResourceCache} aims to solve this issue by providing a possibility to
 * reuse Resources.
 * <p>
 * <b>Implementation Notes</b>
 * <ul>
 * <li> To share resources inbetween multiple Solr cores the Jar file for this component
 * MUST NOT be in the <code>lib</code> folder of the core or configSet. This is because
 * Solr uses {@link URLClassLoader} to load components from the <code>lib</code> folder
 * and therefore each core will have its own {@link ResourceCache} instance. To share
 * resources with multiple cores/configSets one needs to include this jar in the classpath
 * or configure a <code>sharedLib</code> folder in the <code>solr.xml</code> and add it
 * there.
 * <li>As Lucene does not define a lifecycle for {@link TokenFilterFactory} components
 * this cache can not use {@link RefCount}. Instead it uses {@link WeakReference}
 * to hold in Resources. So it is up to the garbage collector to decide when a Resource
 * is no longer needed
 * <li> The cache uses the Factory Pattern to avoid extending the ResourceLoader or
 * adding a ResouceCacheAware callback.
 * <li> {@link ResourceType} definition provide type savety and {@link ResourceTypeLoader}
 * implement the actual loading od resources (if not cached).
 * <li> The {@link ResourceRef} has two responsibilities: First it iss used as key for
 * the cache so it define equivalence for Resources. Second it needs to provide all the
 * information required by the {@link ResourceTypeLoader} to load the referenced resource
 * </ul>
 *
 * <b>Example Usage</b>
 * <pre>
 *
 *  public class MyClass {
 *
 *
 *      //(1) Define the Resource Type(s) used by my Component
 *      public static final ResourceType&lt;MyClass.MyConfig, Dictionary&gt; DICT_RESOURCE = new ResourceType&lt;&gt;(
 *              MyClass.class.getName() +".dict", MyClass.MyConfig.class, Dictionary.class);
 *
 *      //(2) Get the ResourceCache
 *      private ResourceCache cache = ResourceCache.getInstance();
 *
 *      public MyClass() {
 *          //(3) Tell the ResourceCache how to load my resource types
 *          cache.registerLoader(new ResourceCache.ResourceTypeLoader&lt;MyConfig, Dictionary&gt;(DICT_RESOURCE){
 *              {@literal @}Override
 *              public Dictionary load(ResourceRef&lt;MyConfig, Dictionary&gt; ref) {
 *                  MyConfig config = ref.getKey();
 *                  //implement the loading of the resource based on the data provided by the reference
 *                  return null;
 *              }
 *          });
 *      }
 *
 *      {@literal @}Override
 *      public void inform(ResourceLoader loader) throws IOException {
 *          //(4) Get the Reference to the Resoruce
 *          //Typically this will be done by parsing the configuration
 *          //and obtaining the paths to the source data from the Lucene ResourceLoader
 *          MyConfig config = null;
 *
 *          //(5) Get the Dictionary from the ResourceCache
 *          Dictionary dict = cache.getResource(DICT_RESOURCE.createReference(config));
 *
 *      }
 *
 *
 *      //(6) We want only load a single Dictionary for similar configuration. So
 *      // this class needs to implement the check for equivalence (see
 *      // {@link #hashCode()} and {@link #equals(Object)} implementation)
 *      // NOTE: In cases the configuration includes resources (e.g. dictionary files)
 *      // it is required to include the hash of those files (e.g. the MD5) in the
 *      // equivalence check. Because if someone change such a file and calls reload
 *      // core afterwards we want to detect the change and reload the dictionary.
 *      //
 *      static class MyConfig {
 *
 *          private final String dictResoruce;
 *          private final String dictMd5;
 *          private boolean caseSensitive = false;
 *
 *          public MyConfig(String dictResoruce, String dictMd5, boolean caseSensitive) {
 *              this.dictResoruce = dictResoruce;
 *              this.dictMd5 = dictMd5;
 *              this.caseSensitive = caseSensitive;
 *          }
 *
 *          public String getDictResoruce() {
 *              return dictResoruce;
 *          }
 *
 *          public boolean isCaseSensitive() {
 *              return caseSensitive;
 *          }
 *
 *          {@literal @}Override
 *          public int hashCode() {
 *              final int prime = 31;
 *              int result = 1;
 *              result = prime * result + (caseSensitive ? 1231 : 1237);
 *              result = prime * result + ((dictMd5 == null) ? 0 : dictMd5.hashCode());
 *              result = prime * result + ((dictResoruce == null) ? 0 : dictResoruce.hashCode());
 *              return result;
 *          }
 *
 *          {@literal @}Override
 *          public boolean equals(Object obj) {
 *              if (this == obj) return true;
 *              if (obj == null) return false;
 *              if (getClass() != obj.getClass()) return false;
 *              MyConfig other = (MyConfig) obj;
 *              if (caseSensitive != other.caseSensitive) return false;
 *              if (dictMd5 == null) {
 *                  if (other.dictMd5 != null) return false;
 *              } else if (!dictMd5.equals(other.dictMd5)) return false;
 *              if (dictResoruce == null) {
 *                  if (other.dictResoruce != null) return false;
 *              } else if (!dictResoruce.equals(other.dictResoruce)) return false;
 *              return true;
 *          }
 *      }
 * </pre>
 *
 * @author Rupert Westenthaler
 */
public final class ResourceCache {

    private static final Logger log = LoggerFactory.getLogger(ResourceCache.class);

    private static final ResourceCache INSTANCE = new ResourceCache();
    /*
     * NOTE: As we do not have a defined lifecycle for this cache AND the Factories
     * we can not use RefCount.
     * The best solutions seams to be using WeakReferences to the values. So the
     * Garbage Collector is in charge to decide when a cached resource is no longer
     * needed and can be removed from the cache
     */
    private final LoadingCache<ResourceRef<?, ?>, Object> resources = Caffeine.newBuilder()
            .maximumSize(Integer.MAX_VALUE)
            .weakValues()
            .build(this::loadResource);

    private final Map<String, ResourceTypeLoader<?, ?>> loaders = new ConcurrentHashMap<>();

    private ResourceCache() {
    }

    public static ResourceCache getInstance() {
        return INSTANCE;
    }

    public <K, R> void registerLoader(ResourceTypeLoader<K, R> loader) {
        if (loader == null) {
            return;
        }
        if (loader.getResource() == null) {
            throw new NullPointerException("The Resource of the ResourceLoader MUST NOT be NULL!");
        }
        if (log.isDebugEnabled()) {
            log.debug("[instance: {}] Register Loader for {}", Integer.toHexString(this.hashCode()), loader.getResource());
        }
        loaders.put(loader.getResource().getKey(), loader);
    }

    private <K, R> R loadResource(final ResourceRef<K, R> ref) throws IOException {
        @SuppressWarnings("unchecked")
        ResourceTypeLoader<K, R> loader = (ResourceTypeLoader<K, R>) loaders.get(ref.getType().getKey());
        if (loader == null) {
            throw new IllegalStateException("No ResourceLoader for " + ref.getType() + " registerd");
        }
        if (log.isDebugEnabled()) {
            log.debug("[instance: {}] load {} (Loader: {})", Integer.toHexString(this.hashCode()), ref, loader.getClass().getName());
        }
        return loader.load(ref);
    }

    @SuppressWarnings("unchecked")
    public <K, R> R getResource(ResourceRef<K, R> ref) throws IOException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("[instance: {}] lookup {}", Integer.toHexString(this.hashCode()), ref);
            }
            return (R) resources.get(ref);
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    public static final class ResourceType<K, R> {
        private final String key;
        private final Class<K> keyType;
        private final Class<R> valueType;

        public ResourceType(String key, Class<K> keyType, Class<R> valueType) {
            this.key = key;
            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        public String getKey() {
            return key;
        }

        public Class<K> getKeyType() {
            return keyType;
        }

        public Class<R> getValueType() {
            return valueType;
        }

        public final ResourceRef<K, R> createReference(K key) {
            return new ResourceRef<>(this, key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ResourceType<?, ?> other = (ResourceType<?, ?>) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "ResourceType [key=" + key + ", keyType=" + keyType + ", valueType=" + valueType + "]";
        }

    }

    public abstract static class ResourceTypeLoader<K, R> {
        final ResourceType<K, R> resource;

        protected ResourceTypeLoader(ResourceType<K, R> resource) {
            this.resource = resource;
        }

        public final ResourceType<K, R> getResource() {
            return resource;
        }

        public abstract R load(ResourceRef<K, R> ref) throws IOException;


    }

    public static class ResourceRef<K, R> {

        private final ResourceType<K, R> type;
        private final K key;

        ResourceRef(ResourceType<K, R> type, K key) {
            this.type = type;
            this.key = key;
        }

        public ResourceType<K, R> getType() {
            return type;
        }

        public K getKey() {
            return key;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ResourceRef<?, ?> other = (ResourceRef<?, ?>) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "ResourceRef [resource=" + type + ", key=" + key + "]";
        }

    }

}
