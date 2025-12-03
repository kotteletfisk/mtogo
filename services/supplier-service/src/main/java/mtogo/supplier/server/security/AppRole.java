package mtogo.supplier.server.security;

import io.javalin.security.RouteRole;

public enum AppRole implements RouteRole {
    CUSTOMER,
    SUPPLIER,
    COURIER,
    MANAGER,
    SUPPORT
}
