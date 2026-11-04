package it.unipd.dei.webapp.dope.resource;

import org.apache.logging.log4j.ThreadContext;


/**
 * This class provides utility methods for managing log context information
 * related to the current thread using Apache Log4j's `ThreadContext`.
 *
 * It allows the logging of user-specific details such as the user, IP address,
 * action performed, and resource being processed for more detailed log output.
 *
 * The context information is stored and retrieved on a per-thread basis, making
 * it useful for tracking activities across different parts of a web application.
 *
 * This class cannot be instantiated or subclassed.
 */
public final class LogContext {


    /**
     * The user who is performing an action.
     */
    private static final String USER = "USER";

    /**
     * The IP address of the user who is performing an action.
     */
    private static final String IP = "IP";

    /**
     * The action performed by the user.
     */
    private static final String ACTION = "ACTION";

    /**
     * The resource currently processed.
     */
    private static final String RESOURCE = "RESOURCE";


    /**
     * Gets the user currently performing actions.
     *
     * @return the current user, or {@code null} if not set.
     */
    public static String getUser() {
        return ThreadContext.get(USER);
    }

    /**
     * Gets the IP address of the user currently performing actions.
     *
     * @return the current IP address, or {@code null} if not set.
     */
    public static String getIPAddress() {
        return ThreadContext.get(IP);
    }

    /**
     * Gets the action currently performed.
     *
     * @return the current action, or {@code null} if not set.
     */
    public static String getAction() {
        return ThreadContext.get(ACTION);
    }

    /**
     * Gets the resource currently processed.
     *
     * @return the current resource, or {@code null} if not set.
     */
    public static String getResource() {
        return ThreadContext.get(RESOURCE);
    }

    /**
     * Sets the {@code user} currently performing actions.
     * <p>
     * If {@code null}, it simply returns.
     *
     * @param user the {@code user} currently performing actions.
     */
    public static void setUser(final String user) {
        if (user != null && !user.isEmpty()) {
            ThreadContext.put(USER, user);
        }
    }

    /**
     * Removes the {@code User} currently performing actions.
     */
    public static void removeUser() {
        ThreadContext.remove(USER);
    }

    /**
     * Sets the {@code IP} addressed of the user currently performing actions.
     * <p>
     * If {@code null} or empty, it simply returns.
     *
     * @param ip the {@code IP} addressed of the user currently performing actions.
     */
    public static void setIPAddress(final String ip) {
        if (ip != null && !ip.isEmpty()) {
            ThreadContext.put(IP, ip);
        }
    }

    /**
     * Removes the {@code IP} addressed of the user currently performing actions.
     */
    public static void removeIPAddress() {
        ThreadContext.remove(IP);
    }


    /**
     * Sets the {@code action} currently performed.
     * <p>
     * If {@code null}, it simply returns.
     *
     * @param action the action currently performed.
     */
    public static void setAction(final String action) {
        if (action != null) {
            ThreadContext.put(ACTION, action);
        }
    }


    /**
     * Removes the action currently performed.
     */
    public static void removeAction() {
        ThreadContext.remove(ACTION);
    }

    /**
     * Sets the {@code resource} currently processed.
     * <p>
     * If {@code null}, it simply returns.
     *
     * @param resource the resource currently processed.
     */
    public static void setResource(final String resource) {
        if (resource != null && !resource.isEmpty()) {
            ThreadContext.put(RESOURCE, resource);
        }
    }

    /**
     * Removes the resource currently processed.
     */
    public static void removeResource() {
        ThreadContext.remove(RESOURCE);
    }

    /**
     * This class can be neither instantiated nor sub-classed.
     */
    private LogContext() {
        throw new AssertionError(String.format("No instances of %s allowed.", LogContext.class.getName()));
    }
}
