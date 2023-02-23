## Running the sample code

1. Start a first node:

    ```
    sbt -Dconfig.resource=local1.conf run
    ```

2. (Optional) Start another node with different ports:

    ```
    sbt -Dconfig.resource=local2.conf run
    ```

3. Check for service readiness

    ```
    curl http://localhost:9101/ready
    ```
### TODO:
- Add a dockerfile with a building step.
   - Add it to docker-compose.yml
- Add tests to grpc service layer.
- Add tests to mappers.
- Overload repository query methods with sorting by date
- Find a better type for image(byte stream maybe).
- Find a way to isolate service/adapter layer(there are business elements inside the grpc service).
- Segregate post and user in projects.
    - Investigate using kafka to replicate event journal to other applications
  - Segregate command and query logic???(Make sure it would make any difference with the actor model).
- Investigate 'com.sksamuel.scrimage' for image compression.
- Organize the entities better(Too much content for a file);