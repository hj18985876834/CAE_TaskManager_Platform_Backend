# Backend Structure Checklist

This checklist maps the implemented skeleton against the design document.

## Top-level Modules

- [x] common-lib
- [x] gateway-service
- [x] user-service
- [x] solver-service
- [x] task-service
- [x] scheduler-service
- [x] node-agent

## common-lib

- [x] config
- [x] constant
- [x] dto
- [x] enums
- [x] exception
- [x] response
- [x] utils

## gateway-service

- [x] config
- [x] filter
- [x] handler
- [x] router
- [x] support

## user-service

- [x] interfaces/controller
- [x] interfaces/request
- [x] interfaces/response
- [x] application/service
- [x] application/facade
- [x] application/assembler
- [x] domain/model
- [x] domain/repository
- [x] domain/service
- [x] infrastructure/persistence/entity
- [x] infrastructure/persistence/mapper
- [x] infrastructure/persistence/repository
- [x] infrastructure/security

## solver-service

- [x] interfaces/controller
- [x] interfaces/request
- [x] interfaces/response
- [x] application/service
- [x] application/assembler
- [x] domain/model
- [x] domain/repository
- [x] domain/service
- [x] infrastructure/support

## task-service

- [x] interfaces/controller
- [x] interfaces/internal
- [x] interfaces/request
- [x] interfaces/response
- [x] application/service
- [x] application/manager
- [x] application/assembler
- [x] domain/model
- [x] domain/repository
- [x] domain/service
- [x] domain/rule
- [x] infrastructure/persistence/entity
- [x] infrastructure/persistence/mapper
- [x] infrastructure/persistence/repository
- [x] infrastructure/client
- [x] infrastructure/storage
- [x] infrastructure/support

## scheduler-service

- [x] interfaces/controller
- [x] interfaces/internal
- [x] interfaces/request
- [x] interfaces/response
- [x] application/service
- [x] application/manager
- [x] application/scheduler
- [x] domain/model
- [x] domain/service
- [x] domain/strategy
- [x] infrastructure/client
- [x] infrastructure/support

## node-agent

- [x] interfaces/controller
- [x] interfaces/request
- [x] application/service
- [x] application/manager
- [x] application/scheduler
- [x] domain/model
- [x] domain/service
- [x] domain/executor
- [x] infrastructure/client
- [x] infrastructure/process
- [x] infrastructure/storage
- [x] infrastructure/support

## Notes

- Package root is unified to com.example.cae.
- Legacy com.cae package trees were removed.
- Current stage focuses on structural scaffolding and class skeletons.
