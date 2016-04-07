import dynapiclient.rest.*

blog = new RestDynClient(base: 'http://jsonplaceholder.typicode.com/')

println blog.posts."37".comments()

println blog.posts."37"()
println blog.posts."37" = [title: 'Hi!', id: 37, userId: 4, body: 'Good morning in the morning.']
println blog.posts << [title: 'Hi!', id: 3747634, userId: 4, body: 'Good morning in the morning.']
println blog.posts.add([title: 'Hi2!', id: 3747635, userId: 4, body: 'Good morning in the morning 2.'])
println blog.posts."37".delete()
