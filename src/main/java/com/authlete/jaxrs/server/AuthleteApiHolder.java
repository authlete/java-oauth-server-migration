package com.authlete.jaxrs.server;

import com.authlete.common.api.AuthleteApi;
import com.authlete.common.api.AuthleteApiFactory;
import com.authlete.common.conf.AuthleteApiVersion;
import com.authlete.common.conf.AuthleteConfiguration;
import com.authlete.common.conf.AuthletePropertiesConfiguration;
import com.authlete.common.conf.AuthleteSimpleConfiguration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author kylegonzalez
 */
public class AuthleteApiHolder
{
    private static final String V2_BASE_URL = "v2_base_url";
    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<Map<String, Object>>() {}.getType();
    private static final AuthleteApiHolder INSTANCE = new AuthleteApiHolder();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The secondary Authlete API, if both are configured, this should be connected to the Authlete 2.3 server.
     */
    private final AuthleteApi secondaryAuthleteApi;

    /**
     * The primary Authlete Api, if both are configured, this should be connected to the Authlete 3 server.
     */
    private final AuthleteApi primaryAuthleteApi;

    private AuthleteApiHolder()
    {
        AuthleteConfiguration initialConfiguration = new AuthletePropertiesConfiguration();
        primaryAuthleteApi = AuthleteApiFactory.create(initialConfiguration);
        logger.info("Initializing configuration for Authlete Api with version [{}]", initialConfiguration.getApiVersion());

        // If V3 is specified and also properties for V2 are provided, we will attempt to create a configuration for
        // and connect to both configured Authlete API servers
        if (AuthleteApiVersion.V3.name().equalsIgnoreCase(initialConfiguration.getApiVersion())
                && initialConfiguration.getServiceApiSecret() != null && !initialConfiguration.getServiceApiSecret().isEmpty())
        {
            logger.info("Api Version set to [{}] but [{}] supported properties have also been provided. Initializing migration supported configuration with configured both Authlete 2.3 and Authlete 3 applications.",
                    initialConfiguration.getApiVersion(), AuthleteApiVersion.V2);
            AuthleteConfiguration v2Configuration = new AuthleteSimpleConfiguration()
                    .setBaseUrl(System.getProperty(V2_BASE_URL, initialConfiguration.getBaseUrl()))
                    .setApiVersion(AuthleteApiVersion.V2.name())
                    .setServiceApiSecret(initialConfiguration.getServiceApiSecret())
                    .setServiceApiKey(initialConfiguration.getServiceApiKey())
                    .setServiceOwnerApiKey(initialConfiguration.getServiceOwnerApiKey())
                    .setServiceOwnerApiSecret(initialConfiguration.getServiceOwnerApiSecret())
                    .setDpopKey(initialConfiguration.getDpopKey())
                    .setClientCertificate(initialConfiguration.getClientCertificate());

            secondaryAuthleteApi = AuthleteApiFactory.create(v2Configuration);
        }
        else
        {
            secondaryAuthleteApi = null;
        }
    }

    public static AuthleteApiHolder getInstance()
    {
        return INSTANCE;
    }

    public Response withApi(Function<AuthleteApi, Response> function, BiFunction<Response, Map<String, Object>, Boolean> isErrorFunction)
    {
        return withApi(CallerStrategy.UNTIL_SUCCESS, ResponseReturnStrategy.FIRST_NON_ERROR_RESPONSE, function, isErrorFunction);
    }

    public Response withApi(Function<AuthleteApi, Response> function)
    {
        return withApi(ResponseReturnStrategy.FIRST_NON_ERROR_RESPONSE, function);
    }

    public Response withApi(ResponseReturnStrategy strategy, Function<AuthleteApi, Response> function)
    {
        return withApi(CallerStrategy.UNTIL_SUCCESS, strategy, function);
    }

    public Response withApi(CallerStrategy callerStrategy, ResponseReturnStrategy strategy, Function<AuthleteApi, Response> function)
    {
        return withApi(callerStrategy, strategy, function,
                // Default error function filters out by HTTP error response code
                (res, body) -> res.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode());
    }

    public Response withApi(CallerStrategy callerStrategy, ResponseReturnStrategy strategy, Function<AuthleteApi, Response> function, BiFunction<Response, Map<String, Object>, Boolean> isErrorFunction)
    {
        Response primaryResponse = null;
        try
        {
            primaryResponse = function.apply(primaryAuthleteApi);
        }
        catch (WebApplicationException t)
        {
            primaryResponse = t.getResponse();
        }
        catch (Throwable t)
        {
            primaryResponse = null;
        }
        boolean primaryIsError = primaryResponse == null || isErrorFunction.apply(primaryResponse, getResponseAsMap(primaryResponse));
        if (callerStrategy == CallerStrategy.ONLY_PRIMARY
            || (callerStrategy == CallerStrategy.UNTIL_SUCCESS && !primaryIsError))
        {
            return primaryResponse;
        }

        Response secondaryResponse;
        try
        {
            secondaryResponse = function.apply(secondaryAuthleteApi);
        }
        catch (WebApplicationException t)
        {
            secondaryResponse = t.getResponse();
        }
        catch (Throwable t)
        {
            secondaryResponse = null;
        }
        boolean secondaryIsError = secondaryResponse == null || isErrorFunction.apply(secondaryResponse, getResponseAsMap(secondaryResponse));

        // We won't check for ResponseReturnStrategy.PRIMARY since returning v3 response is the default fall through case

        if (strategy == ResponseReturnStrategy.SECONDARY)
        {
            return secondaryResponse;
        }
        else if (strategy == ResponseReturnStrategy.FIRST_NON_ERROR_RESPONSE)
        {
            if (!primaryIsError)
            {
                return primaryResponse;
            }
            else if (!secondaryIsError)
            {
                return secondaryResponse;
            }
        }
        else if (strategy == ResponseReturnStrategy.LAST_NON_ERROR_RESPONSE)
        {
            if (!secondaryIsError)
            {
                return secondaryResponse;
            }
            else if (!primaryIsError)
            {
                return primaryResponse;
            }
        }
        else if (strategy == ResponseReturnStrategy.BOTH_ERROR_THEN_SECONDARY)
        {
            if (primaryIsError && secondaryIsError)
            {
                return secondaryResponse;
            }
        }
        else if (strategy == ResponseReturnStrategy.BOTH_ERROR_THEN_PRIMARY)
        {
            if (primaryIsError && secondaryIsError)
            {
                return primaryResponse;
            }
        }
        else if (strategy == ResponseReturnStrategy.ONE_ERROR_THAN_ERROR)
        {
            if (primaryIsError && !secondaryIsError)
            {
                return primaryResponse;
            }
            else if (!primaryIsError && secondaryIsError)
            {
                return secondaryResponse;
            }
        }
        else if (strategy == ResponseReturnStrategy.ONE_ERROR_THAN_SUCCESS)
        {
            if (primaryIsError && !secondaryIsError)
            {
                return secondaryResponse;
            }
            else if (!primaryIsError && secondaryIsError)
            {
                return primaryResponse;
            }
        }

        // Always return v3 response by default
        return primaryResponse;
    }

    private static Map<String, Object> getResponseAsMap(Response response)
    {
        if (response == null)
        {
            return null;
        }

        response.bufferEntity();
        try
        {
            String json = response.getEntity().toString();
            return gson.fromJson(json, type);
        }
        catch (Throwable t)
        {
            // If we fail to parse its probably a html response not json to fall through and return an empty map
        }
        return new HashMap<>();
    }
}
