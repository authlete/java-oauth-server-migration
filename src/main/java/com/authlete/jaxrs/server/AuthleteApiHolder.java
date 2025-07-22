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

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author kylegonzalez
 */
public class AuthleteApiHolder
{
    private static final String V2_BASE_URL = "v2_base_url";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AuthleteApi authleteApiv23;
    private final AuthleteApi authleteApiv3;

    private static final AuthleteApiHolder INSTANCE = new AuthleteApiHolder();

    private AuthleteApiHolder()
    {
        AuthleteConfiguration initialConfiguration = new AuthletePropertiesConfiguration();
        authleteApiv3 = AuthleteApiFactory.create(initialConfiguration);

        // If V3 is specified and also properties for V2 are provided, we will attempt to create a configuration for
        // and connect to both configured Authlete API servers
        if (AuthleteApiVersion.V3.name().equalsIgnoreCase(initialConfiguration.getApiVersion())
                && initialConfiguration.getServiceApiSecret() != null && !initialConfiguration.getServiceApiSecret().isEmpty())
        {
            AuthleteConfiguration v2Configuration = new AuthleteSimpleConfiguration()
                    .setBaseUrl(System.getProperty(V2_BASE_URL, initialConfiguration.getBaseUrl()))
                    .setApiVersion(AuthleteApiVersion.V2.name())
                    .setServiceApiSecret(initialConfiguration.getServiceApiSecret())
                    .setServiceApiKey(initialConfiguration.getServiceApiKey())
                    .setServiceOwnerApiKey(initialConfiguration.getServiceOwnerApiKey())
                    .setServiceOwnerApiSecret(initialConfiguration.getServiceOwnerApiSecret())
                    .setDpopKey(initialConfiguration.getDpopKey())
                    .setClientCertificate(initialConfiguration.getClientCertificate());

            authleteApiv23 = AuthleteApiFactory.create(v2Configuration);
        }
        else
        {
            authleteApiv23 = null;
        }
    }

    public static AuthleteApiHolder getInstance()
    {
        return INSTANCE;
    }

    public Response tryWithAuthleteApis(Function<AuthleteApi, Response> function, BiFunction<Response, Map<String, Object>, Boolean> isErrorFunction)
    {
        return tryWithAuthleteApis(CallerStrategy.UNTIL_SUCCESS, ResponseReturnStrategy.FIRST_NON_ERROR_RESPONSE, function, isErrorFunction);
    }

    public Response tryWithAuthleteApis(Function<AuthleteApi, Response> function)
    {
        return tryWithAuthleteApis(ResponseReturnStrategy.FIRST_NON_ERROR_RESPONSE, function);
    }

    public Response tryWithAuthleteApis(ResponseReturnStrategy strategy, Function<AuthleteApi, Response> function)
    {
        return tryWithAuthleteApis(CallerStrategy.UNTIL_SUCCESS, strategy, function);
    }

    public Response tryWithAuthleteApis(CallerStrategy callerStrategy, ResponseReturnStrategy strategy, Function<AuthleteApi, Response> function)
    {
        return tryWithAuthleteApis(callerStrategy, strategy, function,
                // Default error function filters out by HTTP error response code
                (res, body) -> res.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode());
    }

    public Response tryWithAuthleteApis(CallerStrategy callerStrategy, ResponseReturnStrategy strategy, Function<AuthleteApi, Response> function, BiFunction<Response, Map<String, Object>, Boolean> isErrorFunction)
    {
        Response v30Response = null;
        try
        {
            v30Response = function.apply(authleteApiv3);
        }
        catch (WebApplicationException t)
        {
            v30Response = t.getResponse();
        }
        boolean v3IsError = v30Response == null || isErrorFunction.apply(v30Response, getResponseAsMap(v30Response));
        if (callerStrategy == CallerStrategy.ONLY_V3
            || (callerStrategy == CallerStrategy.UNTIL_SUCCESS && !v3IsError))
        {
            return v30Response;
        }

        Response v23Response;
        try
        {
            v23Response = function.apply(authleteApiv23);
        }
        catch (WebApplicationException t)
        {
            v23Response = t.getResponse();
        }
        boolean v2IsError = isErrorFunction.apply(v23Response, getResponseAsMap(v23Response));

        // We won't check for ResponseReturnStrategy.V3_RESPONSE since returning v3 response is the default fall through case

        if (strategy == ResponseReturnStrategy.V2_RESPONSE)
        {
            return v23Response;
        }
        else if (strategy == ResponseReturnStrategy.FIRST_NON_ERROR_RESPONSE)
        {
            if (!v3IsError)
            {
                return v30Response;
            }
            else if (!v2IsError)
            {
                return v23Response;
            }
        }
        else if (strategy == ResponseReturnStrategy.LAST_NON_ERROR_RESPONSE)
        {
            if (!v2IsError)
            {
                return v23Response;
            }
            else if (!v3IsError)
            {
                return v30Response;
            }
        }
        else if (strategy == ResponseReturnStrategy.BOTH_ERROR_THEN_V2)
        {
            if (v3IsError && v2IsError)
            {
                return v23Response;
            }
        }
        else if (strategy == ResponseReturnStrategy.BOTH_ERROR_THEN_V3)
        {
            if (v3IsError && v2IsError)
            {
                return v30Response;
            }
        }
        else if (strategy == ResponseReturnStrategy.ONE_ERROR_THAN_ERROR)
        {
            if (v3IsError && !v2IsError)
            {
                return v30Response;
            }
            else if (!v3IsError && v2IsError)
            {
                return v23Response;
            }
        }
        else if (strategy == ResponseReturnStrategy.ONE_ERROR_THAN_SUCCESS)
        {
            if (v3IsError && !v2IsError)
            {
                return v23Response;
            }
            else if (!v3IsError && v2IsError)
            {
                return v30Response;
            }
        }

        // Always return v3 response by default
        return v30Response;
    }

    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<Map<String, Object>>() {}.getType();

    private static Map<String, Object> getResponseAsMap(Response response)
    {
        response.bufferEntity();
        try
        {
            String json = response.getEntity().toString();
            return gson.fromJson(json, type);
        }
        catch (Throwable t)
        {
            // If we fail to parse its probably a html response not json
        }
        return new HashMap<>();
    }
}
