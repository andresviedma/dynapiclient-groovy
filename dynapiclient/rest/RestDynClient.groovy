package dynapiclient.rest

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
import groovyx.net.http.*

import dynapiclient.utils.*

class RestDynClient extends RestDynClientPath {
    String base
    String encoder = groovyx.net.http.ContentType.JSON

    Closure metaLoader = null
    private RestMetaDoc meta = null

    Closure paramsHandler = { a,b -> }
    Closure clientHandler = Closure.IDENTITY

    boolean exceptionOnError = true
    boolean logCalls = false

    private cachedHttpClient = null

    RestDynClient() {
    }

    RestDynClient(Map params) {
        params?.each { k, v -> this[k] = v }
        fillMetaClass()
    }

    protected def doGet(String path, params = [:]) {
        return doOperation('get', path, params)
    }
    protected def doPut(String path, params = [:]) {
        return doOperation('put', path, params)
    }
    protected def doPost(String path, params = [:]) {
        return doOperation('post', path, params)
    }
    protected def doDelete(String path, params = [:]) {
        return doOperation('delete', path, params)
    }

    private doOperation(String operation, String path, params = [:]) {
        try {
            def client = getHttpClient()
            def callParams = getCallParameters(operation, path, params)

            logInput(operation, callParams)
            def resp = client."${operation}"(callParams)
            logOutput(resp.data)

            return resp.data

        } catch (HttpResponseException e) {
            if (exceptionOnError) {
                throw new RestDynClientException(e)
            } else {
                return e.response.data
            }
        }
    }

    private Map getCallParameters(String operation, String path, params) {
        def callParams = [path: path]
        if (operation == 'post' || operation == 'put') {
            callParams.put('query', [:])
            callParams.put('body', params)
            callParams.put('requestContentType', encoder)
        } else {
            callParams.put('query', params)
        }
        paramsHandler(callParams, operation)
        return callParams
    }

    private void logInput(String operation, Map callParams) {
        if (logCalls) {
            println "==> ${operation}: ${callParams}"
        }
    }
    private void logOutput(resp) {
        if (logCalls) {
            println "<== ${resp}"
        }
    }

    void close() {
        if (cachedHttpClient != null) {
            cachedHttpClient.shutdown()
            cachedHttpClient = null
        }
    }

    def getMeta() {
        if (meta == null && metaLoader != null) {
            meta = metaLoader(base)
        }
        return meta
    }

    String toString() {
        //fillMetaClass()
        return "RestDynClient: ${base} ${path}"
    }

    private HTTPBuilder getHttpClient() {
        if (cachedHttpClient != null && cachedHttpClient.uri.toString() != base) {
            close()
        }
        if (cachedHttpClient == null) {
            cachedHttpClient = new RESTClient(base)
            def httpClient2 = clientHandler(httpClient)
            if (httpClient2 instanceof RESTClient) {
                cachedHttpClient = httpClient2
            }
        }
        return cachedHttpClient
    }
}

class RestDynClientPath {
    RestDynClient client = null
    String path = ''
    boolean hasFilledMetaClass = false

    def propertyMissing(String name) {
        return newPath(name)
    }

    def getAt(name) {
        return newPath(name.toString())
    }

    def methodMissing(String name, arguments) {
        def params = [:]
        if (arguments.length > 0) {
            params = arguments[0]
        }
        def cli = client()
        def path2 = nextpath(name)
        return cli.doGet(path2, params)
    }

    def getData() {
        return client().doGet(path)
    }

    def propertyMissing(String name, value) {
        return client().doPut(nextpath(name), value)
    }

    def leftShift(data) {
        return client().doPost(path, data)
    }

    def add(data) {
        return client().doPost(path, data)
    }

    def delete() {
        return client().doDelete(path)
    }

    def rightShift(name) {
        return client().doDelete(nextpath(name))
    }

    protected RestDynClient client() {
        return (client != null? client : this)
    }

    protected RestDynClientPath newPath(String name) {
        def next = new RestDynClientPath(client: client(), path: nextpath(name))
        next.fillMetaClass()
        return next
    }

    protected String nextpath(String name) {
        return (path == ''? name : path + '/' + name)
    }

    String toString() {
        if (this == client())  return super.toString()
        return getHelp()
    }

    String help() {
        return getHelp()
    }

    String getHelp() {
        def metaDoc = client().getMeta()
        fillMetaClass()

        def man = null
        if (metaDoc != null) {
            man = metaDoc.getResourceManual(path)
        }
        if (man == null) {
            man = "*** path: ${path}"
        }
        return man
    }

    def getPathAttributes() {
        def meta = client().getMeta()
        if (meta == null) {
            return [] as Set
        } else {
            def resources = meta.getNextResourcePieces(path)
            return resources
        }
    }

    synchronized void fillMetaClass() {
        if (!hasFilledMetaClass) {
            fillMetaClass(this)
            hasFilledMetaClass = true
        }
    }
    void fillMetaClass(o) {
        AutocompleteMetaClass.addFakeMethodsToObject(o, getPathAttributes(), [])
    }
}
