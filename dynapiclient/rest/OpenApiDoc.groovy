package dynapiclient.rest

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
import groovyx.net.http.*

class OpenApiDoc implements RestMetaDoc {
    final List resources
    final Map resourcePaths
    final boolean debug = false

    OpenApiDoc(String base, debug = false) {
        this.debug = debug
        try {
            log "Reading doc..."
            this.resources = readDocs(base)
            log "OK"
        } catch (e) {
            log "ERROR reading doc: ${e.message}"
            this.resources = []
        }
        this.resourcePaths = processResourcesPieces(this.resources)
    }

    private log(s) {
        if (debug)  println s
    }

    String getResourceManual(String path) {
        def data = getResourceData(path)
        if (data == null) {
            return null
        } else {
            def buffer = new StringWriter()
            buffer.println "**** ${data.path} ${data.description}"
            data?.operations.each {
                printOperationManual(it, buffer)
            }

            def nextPieces = getNextResourcePieces(path)
            if (!nextPieces.isEmpty()) {
                buffer.println "Next: ${nextPieces.join(', ')}"
            }

            return buffer.toString()
        }
    }

    private String printOperationManual(Map data, buffer) {
        buffer.println "** ${data.httpMethod}: ${data.summary}"
        //if (data.notes) buffer.println "${data.notes}"
        if (data.parameters && data.parameters.size() > 0) {
            buffer.println 'Params: ' + data.parameters.
                collect { param -> "${param.name}${param.required? '(*)' : ''}" }.
                join(', ')
                //${param.description}
        }
    }

    Map getResourceData(String path) {
        return resources.find { resource ->
            matchesResourcePath(path, resource.path)
        }
    }

    private boolean matchesResourcePath(String path, String resource) {
        if (resource.length() < 2)  return false
        if (path.length() < 2)  return false
        if (resource[0] == '/')  resource == resource[1..-1]
        if (path[0] == '/')  path == path[1..-1]

        def regexp = resource.
            replace('/', '\\/').
            replaceAll('\\{[^\\/]+\\}', '([^\\/]+)')
        return path =~ regexp + '$'
    }

    Set getNextResourcePieces(String path) {
        def pieces = path.tokenize('/')
        Map map = resourcePaths
        for (piece in pieces) {
            def nextmap = map[piece]
            if (nextmap == null) {
                def nextentry = map.find { k, v -> k.contains('{') }
                if (nextentry != null)  nextmap = nextentry.value
            }
            if (map == null) {
                break;
            } else {
                map = nextmap
            }
        }
        def result = []
        if (map != null) {
            result.addAll(map.keySet())
        }
        return result
    }

    private readDocs(String base) {
        try {
            def httpClient = new RESTClient(base)

            def resources = []
            def apisData = httpClient.get(path: '/docs')
            apisData.data.apis.each {
                def apiResources = readApiResources(httpClient, it.path)
                resources.addAll(apiResources)
            }

            httpClient.shutdown()
            return resources

        } catch (HttpResponseException e) {
            throw new RestDynClientException(e)
        }
    }

    private List readApiResources(RESTClient httpClient, String apiDocPath) {
        def apiResources = httpClient.get(path: apiDocPath)
        return apiResources.data.apis
    }

    private Map processResourcesPieces(List resources) {
        Map piecesMap = [:]
        resources*.path.each {
            def pieces = it.tokenize('/')
            addPiecesToMap(pieces, 0, piecesMap)
        }
        return piecesMap
    }

    private Map addPiecesToMap(List pieces, int from, Map map) {
        if (from <= pieces.size()) {
            def piece = pieces[from]
            if (piece != null) {
                def nextMap = map[piece]
                if (nextMap == null) {
                    nextMap = [:]
                    map[piece] = nextMap
                }
                addPiecesToMap(pieces, from+1, nextMap)
            }
        }
    }
}
