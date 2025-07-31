# Authorization Server Implementation that supports Authlete server version migration

Authorization Server Implementation in Java supporting OAuth 2.0 & OpenID Connect, extended to demonstrate 
zero-downtime migration from Authlete 2.3 to 3.0.

## Overview

This is an authorization server implementation is based on 
[Java OAuth Server (java-oauth-server)](https://github.com/authlete/java-oauth-server)
implementation and extended to demonstrate how zero-downtime migration can be achieved between Authlete server version 
2.3 and 3.0.

Please refer to the [main repository](https://github.com/authlete/java-oauth-server) for further information about 
the OAuth server and its implementation. This repository primarily focuses on the new changes made in order to 
support zero-downtime migration.

The application accepts arguments for both the version 3 Authlete server as well as the 2.3 Authlete server. Connecting
to two Authlete servers will allow the OAuth server application to handle requests first via the connected 3.0 Authlete 
server, if there is an authentication related or general error then application will then attempt to satisfy the request
via the Authlete 2.3 server.

The general approach is to assume that the version 3 Authlete server is able to handle all requests. During the migration
period it may be unable to successfully handle every request for various reasons such as:
- Client has not been created or migrated into the Authlete 3 server
- Client's token have not been created/updated in the Authlete 3 server
- Other configuration made in Authlete 2.3 has not been applied to the Authlete 3 server application

While specific clients and/or their tokens are being migrated to the Authlete 3 server, the OAuth application will 
refer to the 2.3 Authlete server to satisfy these specific requests. This should continue until ideally all requests
are handled only by the Authlete 3 server.

Once all configuration has been moved and you are satisfied with the configuration, the Authlete 2.3 service can be 
decommissioned. In future maintenance periods, the properties for the OAuth server can remove Authlete 2.3 configuration
or can even move back to the [main repository's](https://github.com/authlete/java-oauth-server) implementation to 
just support the new version 3 of Authlete server.

------------------------------------------------------------------------------------------------------------------------

## How to Run the Application

1. Similarly to the [main repository's](https://github.com/authlete/java-oauth-server), you can clone this repository.

2. Edit the configuration file (`authlete.properties`) to set the API credentials. To enable migration support the 
following properties need to be set:
   - `v2_base_url` - The base URL to access the Authlete 2.3 application
   - `base_url` - The base URL to access the Authlete 3 application
   - `service.api_key` - The Service ID, this ID must be the same in both Authlete 2.3 and 3.0 applications (it can be
created manually in Authlete 3 or imported via the Organization settings)
   - `service.api_secret` (Authlete 2.3) the service secret from the Authlete 2.3 console
   - `api_version` - Should be set to ***V3***
   - `service.access_token` (Authlete 3.0) the service token from the Authlete 3 console

3. Make sure that you have installed [maven][42] and set `JAVA_HOME` properly.

4. Start the authorization server on [http://localhost:8080][38].

        $ mvn jetty:run &

### Run with Docker

If you prefer to use Docker, just run the following command after the step 2.

    $ docker-compose up

------------------------------------------------------------------------------------------------------------------------

## Zero-Downtime Migration Endpoint Changes

This section will outline the new behaviour for each endpoint specifically such as which response would be returned and 
how the primary and secondary AuthleteApis are called. Along with how a "failed"/"error" response is determined from 
the AuthleteApi responses.

The following endpoints are impacted by this implementation, any notes asside from the above default behaviour will be 
noted below:
- **Default behaviour** marks endpoints that initially attempt to make the API call to the primary AuthleteApi. If the
response is an error then the application will attempt to make the call to the secondary AuthleteApi returning the 
successful API result to the caller, otherwise if both are failure response the primary's response is returned.
- **Call Primary Only (3.0)** marks endpoints that are exclusively new 3.0 feature endpoints so calls to these endpoints 
are only made to the primary AuthleteApi and its response is returned immediately without considering the secondary 
AuthleteApi configuration.
- **Calls both APIs** marks endpoints that make calls to both the primary and secondary AuthleteApi then once the API
call has been made to both then the application determines which response to return to the caller.

| Endpoint                                 | Default behaviour | Call Primary Only (3.0) | Calls both APIs |
|------------------------------------------|-------------------|-------------------------|-----------------|
| /api/revocation                          |                   |                         | X               |
| /api/introspection                       | X                 |                         |                 |
| /api/authorization                       | X                 |                         |                 |
| /api/token                               | X                 |                         |                 |
| /api/authorization/decision              | X                 |                         |                 |
| /api/userinfo                            | X                 |                         |                 |
| /api/register/{id}                       | X                 |                         |                 |
| /api/par                                 | X                 |                         |                 |
| /api/gm/{id}                             | X                 |                         |                 |
| /.well-known/openid-federation           | X                 |                         |                 |
| /api/federation/register                 | X                 |                         |                 |
| /api/backchannel/authentication          | X                 |                         |                 |
| /api/backchannel/authentication/callback | X                 |                         |                 |
| /api/device/authorization                | X                 |                         |                 |
| /api/device/complete                     | X                 |                         |                 |
| /api/device/verification                 | X                 |                         |                 |
| /api/obb/accounts                        | X                 |                         |                 |
| /api/obb/resources                       | X                 |                         |                 |
| /api/obb/fapi2base-accounts              | X                 |                         |                 |
| /api/consents                            | X                 |                         |                 |
| /api/jwks                                |                   | X                       |                 |
| /.well-known/openid-configuration        |                   | X                       |                 |
| /.well-known/jwt-issuer                  |                   | X                       |                 |
| /.well-known/openid-credential-issuer    |                   | X                       |                 |
| /api/credential                          |                   | X                       |                 |
| /api/batch_credential                    |                   | X                       |                 |
| /api/vci/jwks                            |                   | X                       |                 |
| /api/offer/{id}                          |                   | X                       |                 |
| /api/offer/issue                         |                   | X                       |                 |
| /api/deferred_credential                 |                   | X                       |                 |

------------------------------------------------------------------------------------------------------------------------

## Determining API Failures

Any endpoints that have non-default implementations to determine an endpoint failure are documented explicitly below.

By default, a response from the AuthleteApi is determined by its response code having a value greater or equal to `400`.
Other conditions that contribute to an error response can be specified per endpoint.

### Introspection Endpoint (/api/introspection)

- A successful response is determined by the response having a HTTP 200 status code and the `active` JSON response
body property must have the value `true`.

### Revocation Endpoint (/api/revocation)

- Will always call **BOTH** the primary and secondary AuthleteApis to attempt to revoke the token in both environments.
Even if the call to the primary is successful.
- The first non-error response is returned. Meaning if both are successful the primary's response is returned, if only
the primary is successful its response is returned, and if only the secondary is successful its response is returned.

------------------------------------------------------------------------------------------------------------------------

## See Also

- [Authlete][7] - Authlete Home Page
- [authlete-java-common][5] - Authlete Common Library for Java
- [authlete-java-jaxrs][3] - Authlete Library for JAX-RS (Java)
- [java-resource-server][40] - Resource Server Implementation

------------------------------------------------------------------------------------------------------------------------

## Contact

| Purpose   | Email Address        |
|:----------|:---------------------|
| General   | info@authlete.com    |
| Sales     | sales@authlete.com   |
| PR        | pr@authlete.com      |
| Technical | support@authlete.com |

------------------------------------------------------------------------------------------------------------------------

## License

Apache License, Version 2.0

JSON files under `src/main/resources/ekyc-ida` have been copied from
https://bitbucket.org/openid/ekyc-ida/src/master/examples/response/ .
Regarding their license, ask the eKYC-IDA WG of OpenID Foundation.


[1]: https://www.rfc-editor.org/rfc/rfc6749.html
[2]: https://openid.net/connect/
[3]: https://github.com/authlete/authlete-java-jaxrs
[4]: https://jcp.org/en/jsr/detail?id=339
[5]: https://github.com/authlete/authlete-java-common
[6]: https://docs.authlete.com/
[7]: https://www.authlete.com/
[8]: https://www.authlete.com/developers/overview/
[9]: https://console.authlete.com/register
[10]: https://www.authlete.com/developers/getting_started/
[11]: https://www.rfc-editor.org/rfc/rfc6749.html#section-3.1
[12]: https://www.rfc-editor.org/rfc/rfc6749.html#section-3.2
[13]: https://openid.net/specs/openid-connect-core-1_0.html
[14]: https://www.rfc-editor.org/rfc/rfc7636.html
[15]: https://www.authlete.com/developers/pkce/
[16]: https://www.rfc-editor.org/rfc/rfc6749.html#section-4.2
[17]: https://www.authlete.com/developers/cd_console/
[18]: https://jersey.java.net/
[19]: https://www.rfc-editor.org/rfc/rfc6750.html
[20]: https://www.rfc-editor.org/rfc/rfc6819.html
[21]: https://www.rfc-editor.org/rfc/rfc7009.html
[22]: https://www.rfc-editor.org/rfc/rfc7033.html
[23]: https://www.rfc-editor.org/rfc/rfc7515.html
[24]: https://www.rfc-editor.org/rfc/rfc7516.html
[25]: https://www.rfc-editor.org/rfc/rfc7517.html
[26]: https://www.rfc-editor.org/rfc/rfc7518.html
[27]: https://www.rfc-editor.org/rfc/rfc7519.html
[28]: https://www.rfc-editor.org/rfc/rfc7521.html
[29]: https://www.rfc-editor.org/rfc/rfc7522.html
[30]: https://www.rfc-editor.org/rfc/rfc7523.html
[31]: https://www.rfc-editor.org/rfc/rfc7636.html
[32]: https://www.rfc-editor.org/rfc/rfc7662.html
[33]: https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
[34]: https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html
[35]: https://openid.net/specs/openid-connect-discovery-1_0.html
[36]: https://openid.net/specs/openid-connect-registration-1_0.html
[37]: https://openid.net/specs/openid-connect-session-1_0.html
[38]: http://localhost:8080
[39]: doc/CUSTOMIZATION.md
[40]: https://github.com/authlete/java-resource-server
[41]: https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
[42]: https://maven.apache.org/
[43]: https://www.rfc-editor.org/rfc/rfc7591.html
[44]: https://www.rfc-editor.org/rfc/rfc7592.html
[45]: https://www.rfc-editor.org/rfc/rfc9126.html
[46]: https://openid.net/specs/fapi-grant-management.html
[IDA]: https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html
[OIDFED]: https://openid.net/specs/openid-federation-1_0.html
