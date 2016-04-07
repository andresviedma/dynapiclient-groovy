package dynapiclient.jsonrpc

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT

class JsonRpcClient {
    String base
    boolean exceptionOnError = true
    boolean handleNamedParameters = true

    Closure clientHandler = Closure.IDENTITY
    Closure paramsHandler = Closure.IDENTITY

    private httpClient = null

    JsonRpcClient() {
    }

    def methodMissing(String name, args) {
        return makeCall(name, args)
    }

    def makeCall(String name, args) {
        def json = jsonCall(name, args)
        def errorMsg = (json?.result?.error?.info ?: json?.error?.message)
        if (errorMsg == null) {
            return json?.result
        } else if (exceptionOnError) {
            throw new JsonRpcException(errorMsg, json?.result?.error?.code ?: -1)
        } else {
            return errorMsg
        }
    }

    def jsonCall(String name, params = []) {
        def http = getHttpClient()
        def callId = (new Random()).nextInt()

        // Named parameters, instead of positional
        if (handleNamedParameters && (params.size() == 1) && (params[0] instanceof Map)) {
            params = params[0]
        }

        params = paramsHandler(params)

        def json = http.request(POST, JSON) { req ->
            headers.'content-type' = "application/JSON;charset=UTF-8"
            body = [
                "jsonrpc": "2.0",
                "id": callId,
                "method": name,
                "params": params
            ]
        }
        return json
    }

    void close() {
        if (httpClient != null) {
            httpClient.shutdown()
            httpClient = null
        }
    }

    private HTTPBuilder getHttpClient() {
        if (httpClient != null && httpClient.uri.toString() != base) {
            close()
        }
        if (httpClient == null) {
            httpClient = new HTTPBuilder(base)
            def httpClient2 = clientHandler(httpClient)
            if (httpClient2 instanceof HTTPBuilder) {
                httpClient = httpClient2
            }
        }
        return httpClient
    }
}

class JsonRpcException extends RuntimeException {
    int code

    JsonRpcException(String message, int code) {
        super(message)
        this.code = code
    }
}
