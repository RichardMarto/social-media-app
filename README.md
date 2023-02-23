## Running the projec
### Requirements:
1. Docker 20.10.22
2. Open JDK 11.0.17
3. Scala 2.13.5
4. sbt 1.5.6
5. grpcurl 1.8.7
### Setup:
1. Start by building the project:
    ```
    sbt compile
    ```
2. Run the stage task to generate the target
     ```
     sbt stage
     ```
3. User the docker:stage task to generate the Dockerfile
     ```
     sbt docker:stage
     ```
4. Start postgres:

    ```
    docker-compose up -d postgres-db
    ```
5. Prepare the tables
   ```
   docker exec -i akka-social-media-app_postgres-db_1 psql -U social-media -t < ddl-scripts/create_tables.sql
   ```
### To run the project locally:
1. Run the project

    ```
    sbt -Dconfig.resource=local1.conf run
    ```
### You can also run the dockerized version of the project by:
1. Start all services with docker-compose
     ```
     docker-compose up -d
     ```
### Usage
The project has 4 GRPC endpoints
1. You should use this one to register new users. A user cannot be changed after created and it should have a unique email.
     ```
    ./grpcurl -d '{"name":"Test Testson", "email": "test_testson@testmail.com"}' -plaintext localhost:8101 socialmedia.UserService.RegisterUser
     ```
   It should return a json with the registered user:
     ```
   {
        "name": "Test Testson",
        "email": "test_testson@testmail.comm"
   }
2. This endpoint can be use to post new posts. Make sure to pass a valid user email as author.
    ```
       ./grpcurl -d '{"content":"Some text.", "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...", "author": "test_testson@testmail.com"}' -plaintext 127.0.0.1:8101 socialmedia.PostService.PostPost
    ```
      It should return a json with the registered user:
    ```
    {
       "id": "test_testson@testmail.com - 2023-02-23T03:39:47.003243Z[GMT]",
       "content": "Some text.",
       "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...",
       "date": {
         "year": 54,
         "month": 2,
         "day": 2023
       },
           "author": "test_testson@testmail.com"
       }
    }
    ```
3. This endpoint can be use to update posts. Note that can only edit the content and the image. You can also omit image or content and it wont be updated. Make sure to pass a valid post id.
    ```
    ./grpcurl -d '{"id": "test_testson@testmail.com - 2023-02-23T03:39:47.003243Z[GMT]", "content":"Another text.", "author": "test_testson@testmail.com"}' -plaintext 127.0.0.1:8101 socialmedia.PostService.UpdatePost
    ```
   It should return a json with the registered user:
    ```
    {
      "id": "test_testson@testmail.com - 2023-02-23T03:39:47.003243Z[GMT]",
      "content": "Another text.",
       "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...",
      "date": {
         "year": 54,
         "month": 2,
         "day": 2023
      },
      "author": "test_testson@testmail.com"
    }
    ```
3. The 2 las endpoints are used to get a list of posts. You can pass pass the author's email as a parameter to get a custom feed with only the posts made by that user or omit that parameter to get a general feed.
    ```
    ./grpcurl -d '{"author": "test_testson@testmail.com"}' -plaintext 127.0.0.1:8101 socialmedia.PostService.GetFeed
    ```
   It should return a json with the registered user:
    ```
    {
      "id": "test_testson@testmail.com - 2023-02-23T03:39:47.003243Z[GMT]",
      "content": "Somw text.",
       "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...",
      "date": {
         "year": 54,
         "month": 2,
         "day": 2023
      },
      "author": "test_testson@testmail.com"
    }
    {
      "id": "test_testson@testmail.com - 2023-02-23T03:40:50.004443Z[GMT]",
      "content": "Another text.",
       "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...",
      "date": {
         "year": 54,
         "month": 2,
         "day": 2023
      },
      "author": "test_testson@testmail.com"
    }
    ```
### TODO:
- Add tests to grpc service layer.
- Add tests to mappers.
- Overload repository query methods with sorting by date
- Find a better type for image(byte stream maybe).
- Find a way to isolate service/adapter layer(there are business elements inside the grpc service).
- Segregate post and user in projects.
    - Investigate using kafka to replicate event journal to other applications
  - Segregate command and query logic???(Make sure it would make any difference with the actor model).
- Investigate 'com.sksamuel.scrimage' for image compression.
- Investigate GRPC for notifications
- Organize the entities better(Too much content for a file)
- Fix bad implemented matchs
- Add some logs

### Notes:
When I started this project, I had in mind an event-sourcing based architecture, using kafka as the event store and and also as an integrator(not using the same topics for integration and event jouirnaling).
As the time passed, I realized that this might not really be beneficial due to the nature of the actor model. 
some architectural decisions were made before this realization and was not changed due to my limited availability.

1. I intended to segregate user information from post related information in 2 micro-services as the post side needed to be more available.
   - That's why there is 2 aggregate roots.
   - That's also why post and user projections has no relationship.
2. I also intended to apply CQRS, so I tried to avoid making the query endpoints dependent on there respective aggregate root state. Making sure to use the projection instead.
3. Another miss implemented decision was the ports/adapters pattern that I also intended to apply, but wasn't able due to some lack of experience with the stack I used.
   - I couldn't segregate the grpc related code from the business side, so the adapter layer was mixed with the service side.
4. The dockerized version of the project is running in native linux code, as I was to lazy to manually write a Dockerfile.