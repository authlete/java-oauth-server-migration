認可サーバー実装 (Java)
=======================

概要
----

未定。

実行方法
--------

1. この認可サーバーの実装をダウンロードします。

        $ git clone https://github.com/authlete/java-oauth-server.git
        $ cd java-oauth-server

2. 設定ファイルを編集して API クレデンシャルズをセットします。

        $ vi authlete.properties

3. [maven][42] がインストールされていること、 `JAVA_HOME` が適切に設定されていることを確認します。

4. [http://localhost:8080][38] で認可サーバーを起動します。

        $ mvn jetty:run &

#### Docker を利用する

Docker を利用する場合は, ステップ 2 の後に以下のコマンドを実行してください.

    $ docker-compose up

その他の情報
------------

- [Authlete][7] - Authlete ホームページ
- [authlete-java-common][5] - Java 用 Authlete 共通ライブラリ
- [authlete-java-jaxrs][3] - JAX-RS (Java) 用 Authlete ライブラリ
- [java-resource-server][40] - リソースサーバーの実装


コンタクト
----------

| 目的 | メールアドレス       |
|:-----|:---------------------|
| 一般 | info@authlete.com    |
| 営業 | sales@authlete.com   |
| 広報 | pr@authlete.com      |
| 技術 | support@authlete.com |

ライセンス
----------

Apache License, Version 2.0

`src/main/resources/ekyc-ida` 以下の JSON ファイル群は
https://bitbucket.org/openid/ekyc-ida/src/master/examples/response/
からコピーしたものです。それらのライセンスについては、OpenID Foundation の
eKYC-IDA ワーキンググループにお尋ねください。

[1]: https://www.rfc-editor.org/rfc/rfc6749.html
[2]: https://openid.net/connect/
[3]: https://github.com/authlete/authlete-java-jaxrs
[4]: https://jcp.org/en/jsr/detail?id=339
[5]: https://github.com/authlete/authlete-java-common
[6]: https://docs.authlete.com/
[7]: https://www.authlete.com/
[8]: https://www.authlete.com/ja/developers/overview/
[9]: https://so.authlete.com/accounts/signup
[10]: https://www.authlete.com/ja/developers/getting_started/
[11]: https://www.rfc-editor.org/rfc/rfc6749.html#section-3.1
[12]: https://www.rfc-editor.org/rfc/rfc6749.html#section-3.2
[13]: https://openid.net/specs/openid-connect-core-1_0.html
[14]: https://www.rfc-editor.org/rfc/rfc7636.html
[15]: https://www.authlete.com/ja/developers/pkce/
[16]: https://www.rfc-editor.org/rfc/rfc6749.html#section-4.2
[17]: https://www.authlete.com/ja/developers/cd_console/
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
[39]: doc/CUSTOMIZATION.ja.md
[40]: https://github.com/authlete/java-resource-server
[41]: https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
[42]: https://maven.apache.org/
[43]: https://www.rfc-editor.org/rfc/rfc7591.html
[44]: https://www.rfc-editor.org/rfc/rfc7592.html
[45]: https://www.rfc-editor.org/rfc/rfc9126.html
[46]: https://openid.net/specs/fapi-grant-management.html
[IDA]: https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html
[OIDFED]: https://openid.net/specs/openid-federation-1_0.html

