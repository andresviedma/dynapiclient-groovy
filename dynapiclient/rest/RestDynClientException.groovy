package dynapiclient.rest

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
import groovyx.net.http.*

class RestDynClientException extends RuntimeException {
    final HttpResponseException httpException

    RestDynClientException(httpException) { this.httpException = httpException }

    def getCode() { httpException.response.status }
    def getData() { httpException.response.data }
    String getMessage() { "(${code}): ${data}" }
}
