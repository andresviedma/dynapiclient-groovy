# Groovy DynApiClient

Dynamic API clients, providing dynamic service calls, autocomplete and integrated help.
Provides clients for REST (with Swagger / OpenApi) and JSON-RPC.

The whys and hows of this idea are explained in
['Operating Microservices with Groovy'](http://www.slideshare.net/andresviedma/operating-microservices-with-groovy).

## Examples of use

### JSON-RPC: Kanboard

```groovy
import dynapiclient.jsonrpc.*
import static dynapiclient.utils.ShellUtils.*

client = new JsonRpcClient(
    base: 'http://demo.kanboard.net/jsonrpc.php',
    clientHandler: { it.auth.basic 'demo', 'demo123' }
    )

println "*** Projects:"
projects = client.getMyProjects()
println pretty(projects)
println()

println "*** Project 1 Tasks:"
tasks = client.getAllTasks(project_id: 1, status_id: 1)
println pretty(tasks)
```

### REST (writable): JsonPlaceholder

```groovy
import dynapiclient.rest.*
import static dynapiclient.utils.ShellUtils.*

blog = new RestDynClient(base: 'http://jsonplaceholder.typicode.com/')

println "*** Comments of post 37 (GET):"
println pretty(blog.posts."37".comments())

println "\n*** Data of post 37 (GET):"
println pretty(blog.posts."37"()

println "\n*** Modify post 37 (PUT):"
println blog.posts."37" =
    [title: 'Hi!', id: 37, userId: 4, body: 'Good morning.']

println "\n*** Add new post (POST):"
println blog.posts << [title: 'Hi!', id: 3747634, userId: 4, body: 'Good morning.']

println "\n*** Add new post (POST):"
println blog.posts.add([title: 'Hi2!', id: 3747635, userId: 4, body: 'Good morning again.'])

println "\n*** Delete post 37 (DELETE):"
println blog.posts."37".delete()
```

### REST + integrated help with OpenApi / Swagger: The Marvel API

```groovy
import dynapiclient.rest.*
import dynapiclient.auth.*
import static dynapiclient.utils.ShellUtils.*

publicKey = '<your-marvel-api-public-key>'
privateKey = '<your-marvel-api-private-key>'

def marvelAuthenticate(Map callParams, String method) {
    def ts = System.currentTimeMillis().toString()
    def hash = EncryptionUtils.md5(ts + privateKey + publicKey)
    callParams.query += [apikey: publicKey, hash: hash, ts: ts]
}

marvel = new RestDynClient(
        base: 'http://gateway.marvel.com/',
        path: '/v1/public',
        metaLoader: { new OpenApiDoc(it) },
        paramsHandler: this.&marvelAuthenticate,
        exceptionOnError: false)

println "** Documentation of /public/v1/characters"
println marvel.characters

println "\n** Documentation of /public/v1/characters"
println marvel.characters.help()

println "\n** Call /public/v1/characters and print the ids and names"
def characters = marvel.characters()
println pretty(characters.data.results.collect { "${it.id}: ${it.name}" })

println "\n** Find Adam Warlock id"  // 1010354
def warlock = characters.data.results.find { it.name == 'Adam Warlock' }
println "Adam Warlock is: ${warlock.id}"

println "\n** Documentation of /public/v1/characters/{characterId}/series"
println marvel.characters."${warlock.id}".series

println "\n** Series Warlock appeared in"
def series =  marvel.characters."${warlock.id}".series()
println pretty(series.data.results*.title)
```
