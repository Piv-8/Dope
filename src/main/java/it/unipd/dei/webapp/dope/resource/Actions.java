package it.unipd.dei.webapp.dope.resource;

/**
 * Defines a set of string constants representing available actions
 * within the web application, such as inserting or updating containers,
 * handling user authentication, and managing images and repositories.
 * <p>
 * This class is not instantiable.
 */
public final class Actions {



	/**
	 * Private constructor to prevent instantiation.
	 * Throws an {@link AssertionError} if called.
	 */
	private Actions() {
		throw new AssertionError(String.format("No instances of %s allowed.", Actions.class.getName()));
	}

	/** Action for deleting a containerfile. */
	public static final String DELETE_CONTAINERFILE = "DELETE_CONTAINERFILE";

	/** Action for committing a container. */
	public static final String COMMIT_CONTAINER = "COMMIT_CONTAINER";

	/** Action for deleting a container. */
	public static final String REST_DISPATCH = "REST_DISPATCH";

	/** Action for inserting a container. */
	public static final String INSERT_CONTAINER = "INSERT_CONTAINER";

	/** Action for executing a container command. */
	public static final String EXEC_CONTAINER_COMMAND = "EXEC_CONTAINER_COMMAND";

	/** Action for getting a container log. */
	public static final String GET_CONTAINER_LOGS = "GET_CONTAINER_LOGS";

	/** Action for starting a container. */
	public static final String START_CONTAINER = "START_CONTAINER";

	/** Action for killing a container. */
	public static final String KILL_CONTAINER = "KILL_CONTAINER";

	/** Action for updating a container. */
	public static final String UPDATE_CONTAINER = "UPDATE_CONTAINER";

	/** Action for authenticating a user. */
	public static final String AUTHENTICATE_USER = "AUTHENTICATE_USER";

	/** Action for inserting an image. */
	public static final String INSERT_IMAGE = "INSERT_IMAGE";

	/** Action for inserting a layer. */
	public static final String INSERT_LAYER = "INSERT_LAYER";

	/** Action for showing image details. */
	public static final String SHOW_IMAGE_DETAIL = "SHOW_IMAGE_DETAIL";

	/** Action for inserting a containerfile. */
	public static final String INSERT_CONTAINERFILE = "INSERT_CONTAINERFILE";

	/** Action for updating a containerfile. */
	public static final String UPDATE_CONTAINERFILE = "UPDATE_CONTAINERFILE";

	/** Action for searching a user. */
	public static final String SEARCH_USER = "SEARCH_USER";

	/** Action for inserting a user. */
	public static final String INSERT_USER = "INSERT_USER";

	/** Action for updating an image. */
	public static final String UPDATE_IMAGE = "UPDATE_IMAGE";

	/** Action for using a container. */
	public static final String USE_CONTAINER = "USE_CONTAINER";

	/** Action for logging out a user. */
	public static final String LOGOUT = "LOGOUT";

	/** Action for logging in a user. */
	public static final String LOGIN = "LOGIN";

	/** Action for deleting an image. */
	public static final String DELETE_IMAGE = "DELETE_IMAGE";

	/** Action for deleting a repository. */
	public static final String DELETE_REPOSITORY = "DELETE_REPOSITORY";

	/** Action for deleting the Container instantiated by a User. */
	public static final String DELETE_CONTAINERS_FROM_USER = "DELETE_CONTAINERS_FROM_USER";

	/** Action for retrieving a layer. */
	public static final String GET_LAYER = "GET_LAYER";

	/** Action for retrieving an image. */
	public static final String GET_IMAGE = "GET_IMAGE";

	/** Action for retrieving a repository. */
	public static final String GET_REPOSITORY = "GET_REPOSITORY";

	/** Action for retrieving a container. */
	public static final String GET_CONTAINER = "GET_CONTAINER";

	/** Action for retrieving a containerfile. */
	public static final String GET_CONTAINERFILE = "GET_CONTAINERFILE";

	/** Action for retrieving a containerfile from a given Image. */
	public static final String GET_CONTAINERFILE_FROM_IMAGE = "GET_CONTAINERFILE_FORM_IMAGE";

	/** Action for retrieving a User from a given Image. */
	public static final String GET_USER_FROM_IMAGE = "GET_USER_FROM_IMAGE";

	/** Action for listing all repositories. */
	public static final String LIST_REPOSITORIES = "LIST_REPOSITORIES";

	/** Action for inserting a repository. */
	public static final String INSERT_REPOSITORY = "INSERT_REPOSITORY";

	/** Action for listing images from repositories. */
	public static final String LIST_IMAGES_FROM_REPOSITORIES = "LIST_IMAGES_FROM_REPOSITORIES";

	/** Action for listing Containers from a given Image. */
	public static final String LIST_CONTAINERS_FROM_IMAGE = "LIST_CONTAINERS_FROM_IMAGE";

	/** Action for listing all Layers from a given Image. */
	public static final String LIST_LAYERS_FROM_IMAGE = "LIST_LAYERS_FROM_IMAGE";

	/** Action for time-outing a session. */
	public static final String SESSION_TIMEOUT = "SESSION_TIMEOUT";

	/** Action for expiring a session. */
	public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

	/** Action for checking if User is Owner */
	public static final String CHECK_USER = "CHECK_USER";

	/** Action for checking if an Image with a certain Tag is already in a specific Repository */
	public static final String CHECK_IMAGE = "CHECK_IMAGE";

	/** Action for filtering protected Resource */
	public static final String FILTER = "FILTER";

}
