package halm.gson

import com.exa.halm.Halm
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
class HalmSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "simple HAL with absolute path"() {
        when:
        Halm hal = Halm.hal("base", '/path', 'params', null) {}

        then:
        hal?.asMap() == [
                _links: [
                        self: [
                                href: 'base/path?params',
                                type: 'application/hal+json'
                        ]
                ]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [:]
    }

    void "simple HAL with relative path"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', null) {}

        then:
        hal?.asMap() == [
                _links: [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [:]
    }

    void "top level HAL map"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', [
                str: 'String value',
                int: 42,
                arr: [1, 2, 3, 5, 7],
                map: [key1: 'val 1', key2: []]
        ]) {}

        then:
        hal?.asMap() == [
                _links: [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                str   : 'String value',
                int   : 42,
                arr   : [1, 2, 3, 5, 7],
                map   : [key1: 'val 1', key2: []]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                str: 'String value',
                int: 42,
                arr: [1, 2, 3, 5, 7],
                map: [key1: 'val 1', key2: []]
        ]
    }

    void "top level HAL values"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params') {
            str 'String value'
            "int" 42
            arr([1, 2, 3, 5, 7])
            map([
                    key1: 'val 1',
                    key2: []
            ])
        }

        then:
        hal?.asMap() == [
                _links: [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                str   : 'String value',
                int   : 42,
                arr   : [1, 2, 3, 5, 7],
                map   : [key1: 'val 1', key2: []]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                str: 'String value',
                int: 42,
                arr: [1, 2, 3, 5, 7],
                map: [key1: 'val 1', key2: []]
        ]
    }

    void "top level HAL links"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', null) {
            link 'simpleRelative', 'simple/relative'
            link 'simpleAbsolute', '/simple/absolute'
            link('simpleComplete', 'simple/complete', 'param=simple'
                    , new Locale('ru', 'RU', '')
                    , 'text/plane', 'simple title', 'deprecated/url', '''link's name''', 'profile/uri'
            )
            link {
                rel 'closure'
                href 'closure/uri'
            }
            link 'mixed', {
                href 'mixed/uri'
            }
            link {
                rel 'complete'
                href 'complete/uri'
                params 'param=complete'
                hreflang Locale.JAPANESE
                type 'image/*'
                title 'complete title'
                deprecation 'deprecated/url'
                name '''link's name'''
                profile 'profile/uri'
            }
        }

        then:
        def expect = [
                _links: [
                        self          : [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ],
                        simpleRelative: [
                                href: 'simple/relative',
                                type: 'application/hal+json'
                        ],
                        simpleAbsolute: [
                                href: 'base/simple/absolute',
                                type: 'application/hal+json'
                        ],
                        simpleComplete: [
                                href       : 'simple/complete?param=simple',
                                hreflang   : 'ru_RU',
                                type       : 'text/plane',
                                title      : 'simple title',
                                deprecation: 'deprecated/url',
                                name       : '''link's name''',
                                profile    : 'profile/uri'
                        ],
                        closure       : [
                                href: 'closure/uri',
                                type: 'application/hal+json'
                        ],
                        mixed         : [
                                href: 'mixed/uri',
                                type: 'application/hal+json'
                        ],
                        complete      : [
                                href       : 'complete/uri?param=complete',
                                hreflang   : 'ja',
                                type       : 'image/*',
                                title      : 'complete title',
                                deprecation: 'deprecated/url',
                                name       : '''link's name''',
                                profile    : 'profile/uri'
                        ]
                ]
        ]
        def result = hal?.asMap()

        expect['_links'].keySet().each { String rel ->
            assert result['_links'][rel]['href'] == expect['_links'][rel]['href']
            assert result['_links'][rel]['type'] == expect['_links'][rel]['type']
            assert result['_links'][rel]['templated'] == expect['_links'][rel]['templated']
            assert result['_links'][rel]['hreflang'] == expect['_links'][rel]['hreflang']
            assert result['_links'][rel]['title'] == expect['_links'][rel]['title']
            assert result['_links'][rel]['deprecation'] == expect['_links'][rel]['deprecation']
            assert result['_links'][rel]['name'] == expect['_links'][rel]['name']
            assert result['_links'][rel]['profile'] == expect['_links'][rel]['profile']
            assert result['_links'][rel] == expect['_links'][rel]
        }

        hal?.asMap() == expect

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [:]
    }

    void "top level HAL templates"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', null) {
            template 'simpleRelative', 'simple/relative'
            template 'simpleAbsolute', '/simple/absolute'
            template('simpleComplete', 'simple/complete', 'param=simple'
                    , new Locale('ru', 'RU', '')
                    , 'text/plane', 'simple title', 'deprecated/url', '''link's name''', 'profile/uri'
            )
            template {
                rel 'closure'
                href 'closure/uri'
            }
            template 'mixed', {
                href 'mixed/uri'
            }
            template {
                rel 'complete'
                href 'complete/uri'
                params 'param=complete'
                hreflang Locale.JAPANESE
                type 'image/*'
                title 'complete title'
                deprecation 'deprecated/url'
                name '''link's name'''
                profile 'profile/uri'
            }
        }

        then:
        def expect = [
                _links: [
                        self          : [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ],
                        simpleRelative: [
                                href     : 'simple/relative',
                                type     : 'application/hal+json',
                                templated: true
                        ],
                        simpleAbsolute: [
                                href     : 'base/simple/absolute',
                                type     : 'application/hal+json',
                                templated: true
                        ],
                        simpleComplete: [
                                href       : 'simple/complete?param=simple',
                                hreflang   : 'ru_RU',
                                type       : 'text/plane',
                                templated  : true,
                                title      : 'simple title',
                                deprecation: 'deprecated/url',
                                name       : '''link's name''',
                                profile    : 'profile/uri'
                        ],
                        closure       : [
                                href     : 'closure/uri',
                                type     : 'application/hal+json',
                                templated: true
                        ],
                        mixed         : [
                                href     : 'mixed/uri',
                                type     : 'application/hal+json',
                                templated: true
                        ],
                        complete      : [
                                href       : 'complete/uri?param=complete',
                                hreflang   : 'ja',
                                type       : 'image/*',
                                templated  : true,
                                title      : 'complete title',
                                deprecation: 'deprecated/url',
                                name       : '''link's name''',
                                profile    : 'profile/uri'
                        ]
                ]
        ]
        def result = hal?.asMap()

        expect['_links'].keySet().each { String rel ->
            assert result['_links'][rel]['href'] == expect['_links'][rel]['href']
            assert result['_links'][rel]['type'] == expect['_links'][rel]['type']
            assert result['_links'][rel]['templated'] == expect['_links'][rel]['templated']
            assert result['_links'][rel]['hreflang'] == expect['_links'][rel]['hreflang']
            assert result['_links'][rel]['title'] == expect['_links'][rel]['title']
            assert result['_links'][rel]['deprecation'] == expect['_links'][rel]['deprecation']
            assert result['_links'][rel]['name'] == expect['_links'][rel]['name']
            assert result['_links'][rel]['profile'] == expect['_links'][rel]['profile']
            assert result['_links'][rel] == expect['_links'][rel]
        }

        hal?.asMap() == expect

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [:]
    }

    void "top level simple embedded"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', null) {
            embedded 'inline rel', {
                href 'inline/uri'
                body {

                }
            }
            embedded {
                rel 'emb rel'
                href 'emb/uri'
                body {

                }
            }
        }

        then:
        def expect = [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        "inline rel": [
                                _links: [
                                        self: [
                                                href: 'inline/uri',
                                                type: 'application/hal+json'
                                        ]
                                ]
                        ],
                        "emb rel"   : [
                                _links: [
                                        self: [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ]
                                ]
                        ]
                ]
        ]
        def result = hal?.asMap()

        expect['_embedded'].keySet().each { String rel ->
            assert result['_embedded'][rel]['_links']['self']['href'] == expect['_embedded'][rel]['_links']['self']['href']
            assert result['_embedded'][rel]['_links']['self']['type'] == expect['_embedded'][rel]['_links']['self']['type']
            assert result['_embedded'][rel]['_links']['self']['templated'] == expect['_embedded'][rel]['_links']['self']['templated']
            assert result['_embedded'][rel]['_links']['self']['hreflang'] == expect['_embedded'][rel]['_links']['self']['hreflang']
            assert result['_embedded'][rel]['_links']['self']['title'] == expect['_embedded'][rel]['_links']['self']['title']
            assert result['_embedded'][rel] == expect['_embedded'][rel]
        }

        hal?.asMap() == expect

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                'inline rel': [:],
                'emb rel'   : [:]]
    }

    void "top level links in embedded"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', null) {
            embedded 'emb', {
                href 'emb/uri'
                body {
                    link 'simpleRelative', 'simple/relative'
                    link 'simpleAbsolute', '/simple/absolute'
                    link('simpleComplete', 'simple/complete', 'param=simple'
                            , new Locale('ru', 'RU', '')
                            , 'text/plane', 'simple title', 'deprecated/url', '''link's name''', 'profile/uri'
                    )
                    link {
                        rel 'closure'
                        href 'closure/uri'
                    }
                    link 'mixed', {
                        href 'mixed/uri'
                    }
                    link {
                        rel 'complete'
                        href 'complete/uri'
                        params 'param=complete'
                        hreflang Locale.JAPANESE
                        type 'image/*'
                        title 'complete title'
                        deprecation 'deprecated/url'
                        name '''link's name'''
                        profile 'profile/uri'
                    }
                }
            }
        }

        then:
        def expect = [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        emb: [
                                _links: [
                                        self          : [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ],
                                        simpleRelative: [
                                                href: 'simple/relative',
                                                type: 'application/hal+json'
                                        ],
                                        simpleAbsolute: [
                                                href: 'base/simple/absolute',
                                                type: 'application/hal+json'
                                        ],
                                        simpleComplete: [
                                                href       : 'simple/complete?param=simple',
                                                hreflang   : 'ru_RU',
                                                type       : 'text/plane',
                                                title      : 'simple title',
                                                deprecation: 'deprecated/url',
                                                name       : '''link's name''',
                                                profile    : 'profile/uri'
                                        ],
                                        closure       : [
                                                href: 'closure/uri',
                                                type: 'application/hal+json'
                                        ],
                                        mixed         : [
                                                href: 'mixed/uri',
                                                type: 'application/hal+json'
                                        ],
                                        complete      : [
                                                href       : 'complete/uri?param=complete',
                                                hreflang   : 'ja',
                                                type       : 'image/*',
                                                title      : 'complete title',
                                                deprecation: 'deprecated/url',
                                                name       : '''link's name''',
                                                profile    : 'profile/uri'
                                        ]
                                ]
                        ]
                ]
        ]
        def result = hal?.asMap()

        expect['_embedded']['emb']['_links'].keySet().each { String rel ->
            assert result['_embedded']['emb']['_links'][rel]['href'] == expect['_embedded']['emb']['_links'][rel]['href']
            assert result['_embedded']['emb']['_links'][rel]['type'] == expect['_embedded']['emb']['_links'][rel]['type']
            assert result['_embedded']['emb']['_links'][rel]['templated'] == expect['_embedded']['emb']['_links'][rel]['templated']
            assert result['_embedded']['emb']['_links'][rel]['hreflang'] == expect['_embedded']['emb']['_links'][rel]['hreflang']
            assert result['_embedded']['emb']['_links'][rel]['title'] == expect['_embedded']['emb']['_links'][rel]['title']
            assert result['_embedded']['emb']['_links'][rel]['deprecation'] == expect['_embedded']['emb']['_links'][rel]['deprecation']
            assert result['_embedded']['emb']['_links'][rel]['name'] == expect['_embedded']['emb']['_links'][rel]['name']
            assert result['_embedded']['emb']['_links'][rel]['profile'] == expect['_embedded']['emb']['_links'][rel]['profile']
            assert result['_embedded']['emb']['_links'][rel] == expect['_embedded']['emb']['_links'][rel]
        }

        hal?.asMap() == expect

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [emb: [:]]
    }

    void "top level templates in embedded"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', null) {
            embedded 'emb', {
                href 'emb/uri'
                body {
                    template 'simpleRelative', 'simple/relative'
                    template 'simpleAbsolute', '/simple/absolute'
                    template('simpleComplete', 'simple/complete', 'param=simple'
                            , new Locale('ru', 'RU', '')
                            , 'text/plane', 'simple title', 'deprecated/url', '''link's name''', 'profile/uri'
                    )
                    template {
                        rel 'closure'
                        href 'closure/uri'
                    }
                    template 'mixed', {
                        href 'mixed/uri'
                    }
                    template {
                        rel 'complete'
                        href 'complete/uri'
                        params 'param=complete'
                        hreflang Locale.JAPANESE
                        type 'image/*'
                        title 'complete title'
                        deprecation 'deprecated/url'
                        name '''link's name'''
                        profile 'profile/uri'
                    }
                }
            }
        }

        then:
        def expect = [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        emb: [
                                _links: [
                                        self          : [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ],
                                        simpleRelative: [
                                                href     : 'simple/relative',
                                                type     : 'application/hal+json',
                                                templated: true
                                        ],
                                        simpleAbsolute: [
                                                href     : 'base/simple/absolute',
                                                type     : 'application/hal+json',
                                                templated: true
                                        ],
                                        simpleComplete: [
                                                href       : 'simple/complete?param=simple',
                                                hreflang   : 'ru_RU',
                                                type       : 'text/plane',
                                                title      : 'simple title',
                                                templated  : true,
                                                deprecation: 'deprecated/url',
                                                name       : '''link's name''',
                                                profile    : 'profile/uri'
                                        ],
                                        closure       : [
                                                href     : 'closure/uri',
                                                type     : 'application/hal+json',
                                                templated: true
                                        ],
                                        mixed         : [
                                                href     : 'mixed/uri',
                                                type     : 'application/hal+json',
                                                templated: true
                                        ],
                                        complete      : [
                                                href       : 'complete/uri?param=complete',
                                                hreflang   : 'ja',
                                                type       : 'image/*',
                                                title      : 'complete title',
                                                templated  : true,
                                                deprecation: 'deprecated/url',
                                                name       : '''link's name''',
                                                profile    : 'profile/uri'
                                        ]
                                ]
                        ]
                ]
        ]
        def result = hal?.asMap()

        expect['_embedded']['emb']['_links'].keySet().each { String rel ->
            assert result['_embedded']['emb']['_links'][rel]['href'] == expect['_embedded']['emb']['_links'][rel]['href']
            assert result['_embedded']['emb']['_links'][rel]['type'] == expect['_embedded']['emb']['_links'][rel]['type']
            assert result['_embedded']['emb']['_links'][rel]['templated'] == expect['_embedded']['emb']['_links'][rel]['templated']
            assert result['_embedded']['emb']['_links'][rel]['hreflang'] == expect['_embedded']['emb']['_links'][rel]['hreflang']
            assert result['_embedded']['emb']['_links'][rel]['title'] == expect['_embedded']['emb']['_links'][rel]['title']
            assert result['_embedded']['emb']['_links'][rel]['deprecation'] == expect['_embedded']['emb']['_links'][rel]['deprecation']
            assert result['_embedded']['emb']['_links'][rel]['name'] == expect['_embedded']['emb']['_links'][rel]['name']
            assert result['_embedded']['emb']['_links'][rel]['profile'] == expect['_embedded']['emb']['_links'][rel]['profile']
            assert result['_embedded']['emb']['_links'][rel] == expect['_embedded']['emb']['_links'][rel]
        }

        hal?.asMap() == expect

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [emb: [:]]
    }

    void "top level embedded map"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params') {
            embedded 'emb', {
                href 'emb/uri'
                body([
                        str: 'String value',
                        int: 42,
                        arr: [1, 2, 3, 5, 7],
                        map: [key1: 'val 1', key2: []]
                ])
            }
        }

        then:
        hal?.asMap() == [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        emb: [
                                _links: [
                                        self: [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ]
                                ],
                                str   : 'String value',
                                int   : 42,
                                arr   : [1, 2, 3, 5, 7],
                                map   : [key1: 'val 1', key2: []]
                        ]
                ]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                emb: [
                        str: 'String value',
                        int: 42,
                        arr: [1, 2, 3, 5, 7],
                        map: [key1: 'val 1', key2: []]
                ]
        ]
    }

    void "top level embedded values"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params') {
            embedded 'emb', {
                href 'emb/uri'
                body {
                    str 'String value'
                    "int" 42
                    arr([1, 2, 3, 5, 7])
                    map([key1: 'val 1', key2: []])
                }
            }
        }

        then:
        hal?.asMap() == [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        emb: [
                                _links: [
                                        self: [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ]
                                ],
                                str   : 'String value',
                                int   : 42,
                                arr   : [1, 2, 3, 5, 7],
                                map   : [key1: 'val 1', key2: []]
                        ]
                ]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                emb: [
                        str: 'String value',
                        int: 42,
                        arr: [1, 2, 3, 5, 7],
                        map: [key1: 'val 1', key2: []]
                ]
        ]
    }

    void "exclude fields from top JSON"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', [
                (Halm.FORMAT.JSON): ['int', 'map', 'missing']
        ]) {
            str 'String value'
            "int" 42
            arr([1, 2, 3, 5, 7])
            map([
                    key1: 'val 1',
                    key2: []
            ])
        }

        then:
        hal?.asMap(Halm.FORMAT.HAL.suffix) == [
                _links: [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                str   : 'String value',
                arr   : [1, 2, 3, 5, 7]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                str: 'String value',
                int   : 42,
                arr: [1, 2, 3, 5, 7],
                map   : [key1: 'val 1', key2: []]
        ]
    }

    void "exclude fields from top HAL"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', [
                (Halm.FORMAT.HAL): ['int', 'map', 'missing']
        ]) {
            str 'String value'
            "int" 42
            arr([1, 2, 3, 5, 7])
            map([
                    key1: 'val 1',
                    key2: []
            ])
        }

        then:
        hal?.asMap(Halm.FORMAT.HAL.suffix) == [
                _links: [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                str   : 'String value',
                int: 42,
                arr   : [1, 2, 3, 5, 7],
                map: [key1: 'val 1', key2: []]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                str: 'String value',
                arr: [1, 2, 3, 5, 7]
        ]
    }


    void "exclude fields from embedded JSON"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', [
                (Halm.FORMAT.JSON): ['int', 'map', 'missing']
        ]) {
            embedded 'emb', {
                href 'emb/uri'
                body([
                        str: 'String value',
                        int: 42,
                        arr: [1, 2, 3, 5, 7],
                        map: [key1: 'val 1', key2: []]
                ])
            }
        }

        then:
        hal?.asMap(Halm.FORMAT.HAL.suffix) == [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        emb: [
                                _links: [
                                        self: [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ]
                                ],
                                str   : 'String value',
                                arr   : [1, 2, 3, 5, 7]
                        ]
                ]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                emb: [
                        str: 'String value',
                        int   : 42,
                        arr: [1, 2, 3, 5, 7],
                        map   : [key1: 'val 1', key2: []]
                ]
        ]
    }

    void "exclude fields from embedded HAL"() {
        when:
        Halm hal = Halm.hal("base", 'path', 'params', [
                (Halm.FORMAT.HAL): ['int', 'map', 'missing']
        ]) {
            embedded 'emb', {
                href 'emb/uri'
                body([
                        str: 'String value',
                        int: 42,
                        arr: [1, 2, 3, 5, 7],
                        map: [key1: 'val 1', key2: []]
                ])
            }
        }

        then:
        hal?.asMap(Halm.FORMAT.HAL.suffix) == [
                _links   : [
                        self: [
                                href: 'path?params',
                                type: 'application/hal+json'
                        ]
                ],
                _embedded: [
                        emb: [
                                _links: [
                                        self: [
                                                href: 'emb/uri',
                                                type: 'application/hal+json'
                                        ]
                                ],
                                str   : 'String value',
                                int: 42,
                                arr   : [1, 2, 3, 5, 7],
                                map: [key1: 'val 1', key2: []]
                        ]
                ]
        ]

        hal?.asMap(Halm.FORMAT.JSON.suffix) == [
                emb: [
                        str: 'String value',
                        arr: [1, 2, 3, 5, 7]
                ]
        ]
    }
}
