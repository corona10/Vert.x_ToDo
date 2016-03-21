# todo-backend-vert.x for GSoC 2016
  -> This implementation is using H2 SQL DB for demo, 
     but it can change any other SQL engine. because it is ORM.

[![Build Status](https://travis-ci.org/corona10/Vert.x_ToDo.svg?branch=master)](https://travis-ci.org/corona10/Vert.x_ToDo)

## todo-list
* improve [unittest codes](https://github.com/corona10/Vert.x_ToDo/blob/master/todo/src/test/java/todo/APITest.java)
* write tutorial documents for beginners in Korean and English
* improve codes with Vert.x feature and Java8

## OpenShift
* [Deploy Link](http://tododemo-corona10.rhcloud.com/)
* [API Test](http://www.todobackend.com/specs/index.html?http://tododemo-corona10.rhcloud.com/)
* [Client Test](http://www.todobackend.com/client/index.html?http://tododemo-corona10.rhcloud.com/)

## [Docker](https://hub.docker.com/r/corona10/vert.x_todo/)
```
# maven build first
docker build -t corona10/vert.x-todo .
docker run -p 3000:3000 -i -t corona10/vert.x-todo

```
## Spec
* GET / -> get all to-do list
* GET /:entryId -> get entryId's to-do
* POST / -> register to-do
* DELETE / -> delete all to-do list
* DELETE /:entryId -> delete entryId's to-do
* PATCH /:entryId -> update entryId's to-do

## API test result
![Alt text](/docs/img/api-test.png "api-test")
