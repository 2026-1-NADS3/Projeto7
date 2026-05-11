package com.example.aplicativo_maya.api;

public class AuthResponse {
    private Long id;
    private String token;
    private String name;
    private String email;
    private String role;

    public Long getId()      { return id; }
    public String getToken() { return token; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public String getRole()  { return role; }
}