// Doc: https://kanboard.net/documentation/api-json-rpc

import static dynapiclient.utils.ShellUtils.*
import dynapiclient.jsonrpc.*

client = new JsonRpcClient(
    base: 'http://demo.kanboard.net/jsonrpc.php',
    clientHandler: { it.auth.basic 'demo', 'demo123' }
    )

projects = client.getMyProjects()
tasks = client.getAllTasks(project_id: 1, status_id: 1)

println pretty(projects)
println pretty(tasks)
