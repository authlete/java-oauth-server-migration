package com.authlete.jaxrs.server;

import java.util.function.Function;

/**
 * An enum class that controls which response is returned from the {@link AuthleteApiHolder#tryWithAuthleteApis(ResponseReturnStrategy, Function)} method
 * under different scenarios.
 *
 * @author kylegonzalez
 */
public enum ResponseReturnStrategy {

    /**
     * Always return the V3 API call response.
     */
    V3_RESPONSE,

    /**
     * Always return the V2 API call response.
     * <br/>
     * <br/>
     * <b>This should not be preferred since the 2.3 environment is not required to start up this application.</b>
     */
    V2_RESPONSE,

    /**
     * Returns the first non-error response, prioritising V3 then if it is an error response, then V2's response will be returned.
     */
    FIRST_NON_ERROR_RESPONSE,

    /**
     * Returns the last non-error response, which prioritises V2 unless V2's response is an error, then V3 is returned.
     */
    LAST_NON_ERROR_RESPONSE,

    /**
     * If both APIs return an error response then V3's response will be returned to the caller.
     */
    BOTH_ERROR_THEN_V3,

    /**
     * If both APIs return an error response then V2's response will be returned to the caller.
     */
    BOTH_ERROR_THEN_V2,

    /**
     * If one API results in an error response and the other in a success response then return the error response.
     */
    ONE_ERROR_THAN_ERROR,

    /**
     * If one API results in an error response and the other in a success response then return the success response.
     */
    ONE_ERROR_THAN_SUCCESS
}
