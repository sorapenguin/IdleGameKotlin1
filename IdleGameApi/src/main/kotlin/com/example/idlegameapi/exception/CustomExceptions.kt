package com.example.idlegameapi.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class InvalidCredentialsException(message: String) : RuntimeException(message)
class UsernameAlreadyExistsException(message: String) : RuntimeException(message)
