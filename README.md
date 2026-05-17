## How to run tutorials

### System Requirements
* jdk version 17 or later
* docker-compose
* node.js version 14 or later
* npm version 8 or later

### Build tutorial sample application 
```sh
# execute sample with default db(=postgres)
sh build_and_run_tutorial.sh

# execute sample with specific db
sh build_and_run_tutorial.sh postgres
sh build_and_run_tutorial.sh mysql
```

### See tutorial
* 참고) Tutorial Page 를 Open 할 때 Sample DB 가 초기화 됨.

http://localhost:7007

### OpenAPI
http://localhost:7007/swagger-ui.html

## Run test (jest)
* 참고) 별도 쉘 터미널에서 실행할 것
```sh
cd tutorial+test
npm run test
# or 
npx jest --verbose 
```
