package com.citygo.auth.service;
/** A null jti disconnects all sessions for an account; otherwise only the logged-out token's sockets. */
public record AuthChanged(Long userId,String jti){}
