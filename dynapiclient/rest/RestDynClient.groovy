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
        return client().doGet(nextpath(name), params)
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

    private RestDynClientPath newPath(String name) {
        return new RestDynClientPath(client: client(), path: nextpath(name))
    }

    private String nextpath(String name) {
        return (path == ''? name : path + '/' + name)
    }

    private RestDynClient client() {
        return (client != null? client : this)
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
        def resources = client().getMeta().getNextResourcePieces(path)
        return resources
    }
}
