package dynapiclient.utils

import groovy.time.*

class ShellUtils {
    static pretty(obj) {
        try {
            return (new groovy.json.JsonBuilder(obj)).toPrettyString()
        } catch (e) {
            return obj
        }
    }

    static timed(Closure c) {
        def t0 = System.currentTimeMillis()
        try {
            def result = c()
            def t1 = System.currentTimeMillis()
            println result
            return TimeCategory.minus(new Date(t1), new Date(t0))
        } catch (e) {
            def t1 = System.currentTimeMillis()
            println TimeCategory.minus(new Date(t1), new Date(t0))
            throw e
        }
    }
}
