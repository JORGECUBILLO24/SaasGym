package ni.edu.uam.GimnasioAPI.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(UUID gimnasioId) {
        if (gimnasioId == null) {
            throw new IllegalArgumentException("El gimnasio activo es obligatorio.");
        }
        CURRENT.set(new Context(gimnasioId, false));
    }

    public static void allowRootAccess() {
        Context context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("No se puede habilitar acceso raíz sin un gimnasio autenticado.");
        }
        CURRENT.set(new Context(context.gimnasioId(), true));
    }

    public static UUID requireTenantId() {
        Context context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("No se resolvió el gimnasio para esta solicitud.");
        }
        return context.gimnasioId();
    }

    static UUID tenantIdForHibernate() {
        Context context = CURRENT.get();
        return context == null ? new UUID(0L, 0L) : context.gimnasioId();
    }

    public static boolean isRoot() {
        Context context = CURRENT.get();
        return context != null && context.root();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private record Context(UUID gimnasioId, boolean root) {}
}
