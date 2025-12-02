package mtogo.auth.server.security;

import io.javalin.security.RouteRole;

public enum AppRole implements RouteRole {
    CUSTOMER,
    SUPPLIER,
    COURIER,
    MANAGER,
    SUPPORT
}
