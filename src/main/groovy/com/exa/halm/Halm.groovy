package com.exa.halm

import groovy.transform.CompileDynamic

import java.util.regex.Pattern

/**
 * Halm class provides support for HAL view DSL.
 * <p/>
 * It collects all information from it's parameters and allows Closure to provide more.
 * After it completed the method asMap() converts content of the instance into the Map. Later can be converted into JSON (or other serialized form).
 * <p/>
 * Created by cbialik on 1/24/17.
 */
@CompileDynamic
class Halm {
    private static final Pattern RELATIVE_URL_PATTERN = Pattern.compile('\\{?/.*')
    private static final Pattern CURIE_LINK_PATTERN   = Pattern.compile('.+:.+')
    private static final Pattern PARAM_PATTERN        = Pattern.compile('\\{?[?&].*')

    private String   baseURI
    private String   type
    private String   defaultCurieName = null
    private Embedded parent

    private Map<FORMAT, Set<String>> fieldFilter = [:]

    private Map<String, ?> linkMap     = [:]
    private Map<String, ?> curieMap    = [:]
    private Map<String, ?> embeddedMap = [:]
    private Map<String, ?> valueMap    = [:]

    /**
     * Constructs the instance of Halm with base URL and optional other parts of the URL
     * <p/>
     * @param path -- Service part of the URL
     * @param href -- relative path part of the URL
     * @param params -- query part of the URL
     * @param fieldFilter -- a set of fields to skip for each response format
     */
    Halm(String path, String href = null, String params = '', Map<FORMAT, Set<String>> fieldFilter = [:]) {
        baseURI = path
        this.fieldFilter = fieldFilter
        linkMap.put('curie', curieMap)
        link('self', href, params)
    }

    /**
     * Convenience constructor to take a StringBuffer for Service URL
     * <p/>
     * @param path
     * @param href
     * @param params
     * @param fieldFilter
     */
    Halm(StringBuffer path, String href = null, String params = '', Map<FORMAT, Set<String>> fieldFilter = [:]) {
        this(path.toString(), href, params, fieldFilter)
    }

    protected Map<FORMAT, Set<String>> getFieldFilter() {
        return fieldFilter
    }

    /**
     * Handles unknown JSON properties
     * <p/>
     * @param name
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        switch (args.size()) {
            case 0:
                break
            case 1:
                if (args[0] != null) {
                    if (args[0] instanceof Closure) {
                        value(name, args[0]())
                    } else {
                        value(name, args[0])
                    }
                }
                break
            default:
                value(name, args)
                break
        }
    }

    /**
     * Insert curie entry into enclosing HAL document
     * <p/>
     * @param name of the curie
     * @param href template including "{/rel}" to build URL of the documentation
     * @return
     */
    Halm curie(String name, String href = null) {
        if (!href) {
            defaultCurieName = name
        }
        Curie curie = new Curie(baseURI)
        curie.rel(name)
        curieMap.put(name, curie)
    }

    /**
     * Includes single pair of value into enclosing HAL docuemtn
     * <p/>
     * @param label
     * @param value
     */
    Halm value(String label, Object value) {
        valueMap.put(label, value)
        return this
    }

    /**
     * Include the whole Map as set of values into enclosing HAL document
     * <p/>
     * @param value -- Map of values to include
     */
    Halm value(Map value) {
        valueMap.putAll(value)
        return this
    }

    /**
     * Converts the whole instance into the Map.
     * <p/>
     * Converted Map can be used to serialise into JSON, XML or any other format.<br/>
     * If resulting format is HAL this method will include specific fields ("_links" and "_embedded").
     * Otherwise it includes only value pairs from all levels.
     * <p/>
     * If there is a not-empty fieldFilter then any field listed to be included in any other than requested format will be
     * skipped, unless it is listed in requested format too. All field not listed for any format will be included
     * in all format responses.
     *
     * @param formatString contains either one of the predefined suffixes or MIME string to define the outcome composition
     * @return the Map representing information from this instance
     */
    Map asMap(String formatString = FORMAT.HAL.suffix) {
        FORMAT format = FORMAT.values().find { it.suffix == formatString || it.header == formatString }
        type = findFormat(formatString)

        Map map = new LinkedHashMap()
        if (format == FORMAT.HAL) {
            if (linkMap) {
                map.put('_links', linkMap.findAll {
                    (it.key != 'curie' || curieMap?.size() > 0)
                }.collectEntries {
                    if (it.key == 'curie') {
                        [curies: it.value.collect {
                            it.value.buildLink(baseURI)
                        }]
                    } else {
                        def linkMap = it.value.buildLink() // this may update _name for default curie
                        [(it.value.rel): linkMap]
                    }
                })
            }
            if (embeddedMap) {
                map.put('_embedded', embeddedMap.collectEntries {
                    if (it.value instanceof Collection) {
                        [(it.key): it.value
                                ? it.value.collect {
                            Halm builder -> builder?.asMap(formatString)
                        }
                                : []
                        ]
                    } else {
                        [(it.key): ((Halm) it.value)?.asMap(formatString)]
                    }
                })
            }
        } else {
            embeddedMap.collect {
                if (it.value instanceof Collection) {
                    map.put(it.key, it.value?.collect { Halm builder ->
                        builder.asMap(formatString)
                    })
                } else {
                    map.put(it.key, ((Halm) it.value)?.asMap(formatString))
                }
            }
        }

        if (valueMap) {
            Set<String> filter = fieldFilter.findAll { key, value ->
                key != format && value
            }.collect {
                it.value
            }.flatten() as Set<String>
            filter -= fieldFilter?.with { it[format] } ?: []

            map.putAll(
                    valueMap.findAll {
                        !filter.contains(it.key)
                    }.collectEntries {
                        [(it.key): it.value]
                    }
            )
        }
        return map
    }

    /**
     * Defines the mapping from the extension to the MIME type
     */
	 static enum FORMAT {
        HAL('hal', 'application/hal+json'),
        HALM('halm', 'application/hal+json'), // for backward compatibility
        JSON('json', 'application/json'),
        IMAGE(null, 'image/*')

        String suffix
        String header

        private FORMAT(String suffix, String header) {
            this.suffix = suffix
            this.header = header
        }
    }

    /**
     * Find the MIME format string for provided parameter.
     * <p/>
     * If parameter contains the '/' method returns it as a MIME type string.
     * Otherwise it will look up in the {@link FORMAT} to map from extension to MIME type string.
     *
     * @param suffixOrHeader
     * @return MIME type string for provided suffix or return provided MIME type without changes
     */
    static String findFormat(String suffixOrHeader) {
        if (suffixOrHeader == null) {
            return null
        }
        if (suffixOrHeader == 'all') { // default value if no format provided
            FORMAT.HAL.header
        }
        if (suffixOrHeader?.contains('/')) {
            suffixOrHeader
        } else {
            FORMAT.values().find {
                it.suffix == suffixOrHeader
            }.header
        }
    }

    /**
     * Implementation of the DSL top-level 'halm' element.
     * <p/>
     * The Servlet part of the URL may include some additional segments to group API endpoints for different purposes.
     * For example the same endpoints can be used by browser and thick client. Each will use different authentication mechanizes and so
     * Spring Security should handle them differently. Also all URLs included should use appropriate prefix. That prefix should be appended
     * to the Servlet URL, so the HAL-generating code will stay the same regardless.
     *
     * @param baseUrl -- Servlet part of URL (with potential additional prefix)
     * @param href -- relative to Servlet URL path
     * @param params -- query part of the URL
     * @param values -- optional Map of values to include
     * @param closure to populate the rest of the instance
     * @return the instance populated with provided parameters (including the Closure)
     */
    static Halm hal(String baseUrl, String href, String params, Map<Object, Object> values = null,
                    @DelegatesTo(Halm) Closure closure) {
        Map<Boolean, Map<Object, Object>> valuesSplit = values?.groupBy { key, _ ->
            FORMAT.values().contains(key)
        } ?: [:]

        Halm hal = new Halm(baseUrl, href, params, valuesSplit[Boolean.TRUE] as Map<FORMAT, Set<String>>)
        hal.valueMap << (valuesSplit[Boolean.FALSE] ?: [:])

        closure.delegate = hal
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()

        hal
    }

    private def delegate(Object parent, Closure closure) {
        closure.delegate = parent
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        if (parent instanceof Embedded && parent._withCollection) {
            closure(parent._element)
        } else {
            closure()
        }
    }

    /**
     * Defines the _link element with provided values.
     *
     * @param rel -- name (relation) of the link
     * @param href -- path part of the URL
     * @param params -- query part of the URL
     * @param lang -- Locale of the resource which this link points to
     * @param type -- MIME type of the resource which this link points to
     * @param title -- user-readable label for the link
     */
    Halm link(String rel, String href, String params = '', Locale lang = null, String type = null, String title = null
              , String deprecation = null, String name = null, String profile = null) {
        Link link = new Link(href, params, lang, type, false, title, deprecation, name, profile)
        link.rel(rel)
        linkMap.put(rel, link)
        return this
    }

    /**
     * Defines the _link element with provided values and marked as a template.
     *
     * @param rel -- name (relation) of the link
     * @param href -- path part of the URL
     * @param params -- query part of the URL
     * @param lang -- Locale of the resource which this link points to
     * @param type -- MIME type of the resource which this link points to
     * @param title -- user-readable label for the link
     */
    Halm template(String rel, String href, String params = '', Locale lang = null, String type = null, String title = null
                  , String deprecation = null, String name = null, String profile = null) {
        Link link = new Link(href, params, lang, type, true, title, deprecation, name, profile)
        link.rel(rel)
        linkMap.put(rel, link)
        return this
    }

    /**
     * Defines the _link element with provided values. Some of values can be omitted and defined in the closure instead.
     *
     * @param rel -- name (relation) of the link
     * @param href -- path part of the URL
     * @param params -- query part of the URL
     * @param lang -- Locale of the resource which this link points to
     * @param type -- MIME type of the resource which this link points to
     * @param title -- user-readable label for the link
     * @param closure includes calls to set any other arguments
     */
    Link link(String rel = null, String href = null, String params = '', Locale lang = null, String type = null, String title = null
              , String deprecation = null, String name = null, String profile = null,
              @DelegatesTo(Link) Closure closure) {
        Link link = new Link(href, params, lang, type, false, title, deprecation, name, profile)
        link.rel(rel)
        delegate(link, closure)
        linkMap.put(link.rel, link)
        link
    }

    /**
     * Defines the _link element with provided values and marked as a template. Some of values can be omitted and defined in the closure instead.
     *
     * @param rel -- name (relation) of the link
     * @param href -- path part of the URL
     * @param params -- query part of the URL
     * @param lang -- Locale of the resource which this link points to
     * @param type -- MIME type of the resource which this link points to
     * @param title -- user-readable label for the link
     * @param closure includes calls to set any other arguments
     */
    Link template(String rel = null, String href = null, String params = '', Locale lang = null, String type = null, String title = null
                  , String deprecation = null, String name = null, String profile = null,
                  @DelegatesTo(Link) Closure closure) {
        Link template = new Link(href, params, lang, type, true, title, deprecation, name, profile)
        template.rel(rel)
        delegate(template, closure)
        linkMap.put(template.rel, template)
        template
    }

    Curie curie(@DelegatesTo(Curie) Closure closure) {
        Curie curie = new Curie(baseURI)
        delegate(curie, closure)
        curieMap.put(curie._rel, curie)
        curie
    }

    /**
     * Defines embedded collection of resources.
     *
     * @param rel -- name (relation) of the collection
     * @param collection -- empty or not array of resources to embed
     * @param closure includes calls to set href and halm of the resource from the collection
     * @return
     */
    Embedded embedded(String rel, Object[] collection, @DelegatesTo(Embedded) Closure closure) {
        embedded(rel, collection as List<Object>, closure)
    }

    /**
     * Defines embedded a single resource.
     * <p/>
     * If parameter 'rel' is missing it must be set within the Closure
     *
     * @param rel -- name (relation) of the collection
     * @param closure includes calls to set rel (if not provided as parameter, href and halm of the resource
     * @return
     */
    Embedded embedded(String rel = null, @DelegatesTo(Embedded) Closure closure) {
        embedded(rel, null, closure)
    }

    /**
     * Defines embedded collection of resources.
     * <p/>
     * If parameter 'rel' is missing it must be set within the Closure
     *
     * @param rel -- name of the embedded resource
     * @param collection -- empty or not collection of resources to embed
     * @param closure includes calls to set rel (if not provided as parameter, href and halm of the resource
     * @return
     */
    Embedded embedded(String rel, Collection<?> collection, @DelegatesTo(Embedded) Closure closure) {
        Embedded embedded
        List<Embedded> list

        if (collection != null) {
            list = []
            if (collection) { // not empty collection
                list.addAll(collection.collect {
                    embedded = new Embedded(it, baseURI, this, rel)
                    delegate(embedded, closure)
                    embedded._hal
                }.findAll())
            } else {
                embedded = new Embedded(null, baseURI, this, rel)
//                delegate(embedded, closure)
            }
        } else {
            embedded = new Embedded(baseURI, this, rel)
            delegate(embedded, closure)
        }

        def old = embeddedMap.get(embedded._rel)

        if (list != null) {
            if (old instanceof Collection) {
                old.addAll(list)
            } else {
                if (old != null) {
                    list.add(old)
                }
                old = list
                embeddedMap.put(embedded._rel, old)
            }
        } else {
            if (old == null) {
                embeddedMap.put(embedded._rel, embedded._hal)
            } else if (old instanceof Collection) {
                old.add(embedded._hal)
            } else {
                old = [old, embedded._hal]
                embeddedMap.put(embedded._rel, old)
            }
        }
        embedded
    }

    /**
     * Internal class representing links or templates
     */
    @CompileDynamic
    class Link {
        Link(Boolean templated) { _templated = templated }

        Link(String href, String params = ''
             , Locale lang = null, String type = null
             , Boolean templated
             , String title = null, String deprecation = null
             , String name = null, String profile = null) {
            this(templated)
            this.href(href)
            this.params(params)
            hreflang(lang)
            this.type(type)
            this.title(title)
            this.deprecation(deprecation)
            this.name(name)
            this.profile(profile)
        }

        Boolean _templated

        String _curie

        void curie(String curie) { _curie = curie }

        String _rel

        void rel(String rel) { _rel = rel }

        String getRel() {
            return _rel
        }

        String _href

        void href(String href) { _href = href }

        String _params

        void params(String params) { _params = params }

        Locale _hreflang

        void hreflang(Locale hreflang) { _hreflang = hreflang }

        String _type

        void type(String type) { _type = Halm.findFormat(type) }

        String _title

        void title(String title) { _title = title }

        String _deprecation

        void deprecation(String deprecation) { _deprecation = deprecation }

        String _name

        void name(String name) { _name = name }

        String _profile

        void profile(String profile) { _profile = profile }

        Map buildLink() {
            String paramStr = _params ? _params.matches(PARAM_PATTERN) ? _params : "?${_params}" : ''

            String path = ''

            if (!_href) {
                path = baseURI
            } else if (_href.matches(RELATIVE_URL_PATTERN)) {
                if (!_curie) {
                    if (_rel != 'self' || !defaultCurieName) {
                        path = baseURI
                    }
                }
            }
            String absUrl = "$path${_href ?: ''}${paramStr}"

            Map map = [href: absUrl]
            if (_hreflang) {
                map.put('hreflang', _hreflang.toString())
            }
            if (_type || type) {
                map.put('type', _type ?: type)
            }
            if (_templated) {
                map.put('templated', true)
            }
            if (_title) {
                map.put('title', _title)
            }
            if (_deprecation) {
                map.put('deprecation', _deprecation)
            }
            if (_name) {
                map.put('name', _name)
            }
            if (_profile) {
                map.put('profile', _profile)
            }

            return map
        }
    }

    /**
     * Internal class representing a Curie link
     */
    @CompileDynamic
    class Curie {
        Curie(String baseUrl) {
            _baseURL = baseURI
        }

        String _baseURL
        String _curie

        void curie(String curie) { _curie = curie }

        String _rel

        void rel(String rel) { _rel = rel }

        String _href

        void href(String href) { _href = href }

        String href() {
            String href

            href = _href ? _href : _baseURL
            if (href.matches(RELATIVE_URL_PATTERN)) {
                if (_curie) {
                    href = curieMap[_curie].href() + href
                }
            }
            return href
        }

        Map buildLink(String baseUrl) {
            Map map = [
                    name    : _rel,
                    href    : href() + '/{rel}',
                    template: true
            ]
        }
    }
}
