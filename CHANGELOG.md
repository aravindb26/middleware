# App Suite Middleware

All notable changes to this project will be documented in this file.


## [8.19.0] - 2023-10-24

### Added

- [`MW-2088`](https://jira.open-xchange.com/browse/MW-2088): Middleware components for the App Suite Advanced Routing Stack
  - Added new bundles for the request analyzer feature ([`SCR-1241`](https://jira.open-xchange.com/browse/SCR-1241))
  - New properties for Segmenter Client Service ([`SCR-1277`](https://jira.open-xchange.com/browse/SCR-1277))
  - Upgraded the gson library from 2.9.0 to 2.10.1 ([`SCR-1266`](https://jira.open-xchange.com/browse/SCR-1266))
  - New REST endpoint exposed at `/request-analysis/v1/analyze` to analyze
client requests and associate them with segment markers
  - Added first batch of request analyzer implementations covering the
most common client requests
  - Introduced `request-analyzer` service role to deploy and scale
conainers independently
  - Implemented segmenter client API to determine active site for a
certain segment [`a2705aa2`](https://gitlab.open-xchange.com/middleware/core/commit/a2705aa2236c505fcba108ccab13d8b07b65c7b4)


### Changed

- [`MW-2094`](https://jira.open-xchange.com/browse/MW-2094): Added the 'LastModified' and 'ModifiedBy' metadata to each Sieve rule. [`5197be2a`](https://gitlab.open-xchange.com/middleware/core/commit/5197be2ae9634f61da5af17bd58e522e347779c7)
- [`MWB-2296`](https://jira.open-xchange.com/browse/MWB-2296): Only allow certain URI schemes for external calendar
attachments ([`SCR-1307`](https://jira.open-xchange.com/browse/SCR-1307)) [`5277863a`](https://gitlab.open-xchange.com/middleware/core/commit/5277863a2ab06e42d6de80a71dea3730da383ca9)
- [`MWB-2345`](https://jira.open-xchange.com/browse/MWB-2345): Enhanced logging, added fallback for missing response
error code from auth server [`3da0018d`](https://gitlab.open-xchange.com/middleware/core/commit/3da0018d7f8d5319cba1f8c7ed70f20944988430) [`e2332e6a`](https://gitlab.open-xchange.com/middleware/core/commit/e2332e6aac82e0697b029f374a68410f09d383e7)
- Removed vulnerable lib sqlite-jdbc and provided needed dependencies by plain snappy-java lib [`3d9e92d3`](https://gitlab.open-xchange.com/middleware/core/commit/3d9e92d35e07790c9da26517d198e7ac7213846f)
- Updated core-mw chart dependencies and enabled read-only filesystem for gotenberg [`075e07d3`](https://gitlab.open-xchange.com/middleware/core/commit/075e07d327ac728903eee757c14d927551efa001)
- Updated vulnerable lib commons-fileupload 1.4 to latest version 1.5 [`353845aa`](https://gitlab.open-xchange.com/middleware/core/commit/353845aa92016d0da56da5db17962d2d59ebe246)
- Updated vulnerable lib jackrabbit-webdav 2.19.1 to version 2.21.19 [`408e8dd3`](https://gitlab.open-xchange.com/middleware/core/commit/408e8dd33966e5fc4d61b5e4ec39afce91b3d7f1)
- Updated vulnerable lib net.minidev:json-smart and (its dependency accessors-smart) 2.4.8 to version 2.4.11 [`3b7dae91`](https://gitlab.open-xchange.com/middleware/core/commit/3b7dae918923850bb2459512c757cd4cd92f95fd)
- Updated vulnerable lib snakeyaml 1.33 to version 2.2. Depending libraries (e. g. jackson-*) required an update too [`d48c6679`](https://gitlab.open-xchange.com/middleware/core/commit/d48c6679af751b72d6a46b77adac3e42eac9eb3e)
- Updated vulnerable okio-jvm 2.8.0 lib to latest 3.5.0 and cleaned up dependencies (added okio, updated okhttp + kotlin*, test dependencies) [`0aae47f3`](https://gitlab.open-xchange.com/middleware/core/commit/0aae47f3184bd6d5076abe6b87d09c8fba768193)
- Removed default values for chart dependencies and link to source [`9861882b`](https://gitlab.open-xchange.com/middleware/core/commit/9861882bbc711ab685ff660f50c3a99e00f4f463)


### Fixed

- [`MWB-2220`](https://jira.open-xchange.com/browse/MWB-2220): use existing functionality for secret properties [`3e12ce12`](https://gitlab.open-xchange.com/middleware/core/commit/3e12ce12277c577e8ebae1ceeae8ff1434aca5b4)
- [`MWB-2250`](https://jira.open-xchange.com/browse/MWB-2250): No success notification if there are no result files [`263f92eb`](https://gitlab.open-xchange.com/middleware/core/commit/263f92ebe6a0ac6eb1de2d6024e03ced55be53be)
- [`MWB-2283`](https://jira.open-xchange.com/browse/MWB-2283): Don't try to assign a new category when moving to
"general" category [`10e99977`](https://gitlab.open-xchange.com/middleware/core/commit/10e99977a86c2fa0272969e6e26aff22caf3ffb6)
- [`MWB-2296`](https://jira.open-xchange.com/browse/MWB-2296): Check potential UID conflicts for newly added attendees [`d075d98f`](https://gitlab.open-xchange.com/middleware/core/commit/d075d98ffa16a32d020db44456d48cbf7edeabc1)
- [`MWB-2297`](https://jira.open-xchange.com/browse/MWB-2297): Prefer display name for object permission validation
errors [`43e8d4b8`](https://gitlab.open-xchange.com/middleware/core/commit/43e8d4b87756c02fef5c8249f95b566ba2e2cec2)
- [`MWB-2300`](https://jira.open-xchange.com/browse/MWB-2300): Optimized moving folder (and its subtree) to trash [`8ef3f975`](https://gitlab.open-xchange.com/middleware/core/commit/8ef3f97588632b6914a20ca012491e63c83fa229) [`0b68cc47`](https://gitlab.open-xchange.com/middleware/core/commit/0b68cc4745b44747b93e83f5b4e49852d6ecfc96)
- [`MWB-2309`](https://jira.open-xchange.com/browse/MWB-2309): Cross-check resource attendees when evaluating 'all
others declined' flag in list responses [`154ae880`](https://gitlab.open-xchange.com/middleware/core/commit/154ae8801e03b3246beffc0ea078a0282bc12fca)
- [`MWB-2310`](https://jira.open-xchange.com/browse/MWB-2310): "infostore?action=upload" fails with "EOF" error on Appsuite 8 [`269accfb`](https://gitlab.open-xchange.com/middleware/core/commit/269accfb66efefa54ebf42a201e22fdce660f58e) [`c2840ffa`](https://gitlab.open-xchange.com/middleware/core/commit/c2840ffa02b5fa80bac88b1d608eaa9e582331df)
- [`MWB-2322`](https://jira.open-xchange.com/browse/MWB-2322): Probe for name of the function for geo conversion (3) [`12d7d73f`](https://gitlab.open-xchange.com/middleware/core/commit/12d7d73f7edcaecf592a090597e5c62e88a105fa)
- [`MWB-2333`](https://jira.open-xchange.com/browse/MWB-2333): Sanitize broken/corrupt Content-Type string when parsing multipart content [`22a9393b`](https://gitlab.open-xchange.com/middleware/core/commit/22a9393b471e17d0010330f2e7736f3cb2ba0f2d)
- [`MWB-2336`](https://jira.open-xchange.com/browse/MWB-2336): Aligned naming of settings to the ones used by UI [`86bf97bd`](https://gitlab.open-xchange.com/middleware/core/commit/86bf97bde3b6b0083bed3895e990e2a461ef4ea8)
- [`MWB-2337`](https://jira.open-xchange.com/browse/MWB-2337): Ignore possible "NO [NOPERM]" response when issuing a METADATA command to retrieve deputy information from all IMAP folders [`3ce1f58a`](https://gitlab.open-xchange.com/middleware/core/commit/3ce1f58aede5441ef98ef89f381bcc2c7480c2e1)
- [`MWB-2339`](https://jira.open-xchange.com/browse/MWB-2339): Ensure privisioning related log properties are dropped
once message has been logged [`35022c92`](https://gitlab.open-xchange.com/middleware/core/commit/35022c92ac9facc9623e94ad4d55f5808ffa6d98)
- [`MWB-2343`](https://jira.open-xchange.com/browse/MWB-2343): Preferably consider 'X-MICROSOFT-CDO-INTENDEDSTATUS'
when parsing event transparency from iTIP [`2e04a819`](https://gitlab.open-xchange.com/middleware/core/commit/2e04a8193b896e348834c28fdda9c893dee38dea)
- [`MWB-2349`](https://jira.open-xchange.com/browse/MWB-2349): Orderly display plain-text mail w/ alternative text
parts [`baefd0a8`](https://gitlab.open-xchange.com/middleware/core/commit/baefd0a83df80dded87cd49aff500dc19c933c01) [`711ea55b`](https://gitlab.open-xchange.com/middleware/core/commit/711ea55b43c2b91bf84c4287466f4581010a3601)
- [`MWB-2352`](https://jira.open-xchange.com/browse/MWB-2352): More user-readable error message in case message flags
cannot be changed due to insufficient folder permissions [`91188e1e`](https://gitlab.open-xchange.com/middleware/core/commit/91188e1e0bd1f8c5047b476917fcf9db8ba6a200)
- Enhanced detection for images with data URIs [`997ed5ff`](https://gitlab.open-xchange.com/middleware/core/commit/997ed5ff0e2549990493e1676614c734e43a05bc)
- [`MWB-2353`](https://jira.open-xchange.com/browse/MWB-2353): No global lock when initializing in-memory folder map [`f7fef269`](https://gitlab.open-xchange.com/middleware/core/commit/f7fef26956a1c5b5cf7207fb7f01124790cfa72e)


### Removed

- [`MW-2169`](https://jira.open-xchange.com/browse/MW-2169): Removed preliminary sharding extension
  - [`SCR-1303`](https://jira.open-xchange.com/browse/SCR-1303): Dropped sharding related property
  - [`SCR-1304`](https://jira.open-xchange.com/browse/SCR-1304): Dropped 'shard' query paramter from SAML request [`db10fd3a`](https://gitlab.open-xchange.com/middleware/core/commit/db10fd3a6d310a4b05e9aa945ee4f469185eb7e7)
- [`SCR-1311`](https://jira.open-xchange.com/browse/SCR-1311): Removed obsolete Rhino Scripting [`ebef2cd8`](https://gitlab.open-xchange.com/middleware/core/commit/ebef2cd8e80005238b21dff3e46da0a98290f159)
- [`SCR-1312`](https://jira.open-xchange.com/browse/SCR-1312): Removed obsolete bundle [`07bc8d6c`](https://gitlab.open-xchange.com/middleware/core/commit/07bc8d6c39d23bd0f8245743940276e71e634363)


## [8.18.0] - 2023-09-27

### Added

- [`MW-2010`](https://jira.open-xchange.com/browse/MW-2010): Support for Webhooks [`8f93c95f`](https://gitlab.open-xchange.com/middleware/core/commit/8f93c95f0b399ca9f7439731daf27a2c1b070c0c) [`79d3fc0a`](https://gitlab.open-xchange.com/middleware/core/commit/79d3fc0ad3e8b19fc9bdcde7bbe3a58169f82e6a) [`39b017d3`](https://gitlab.open-xchange.com/middleware/core/commit/39b017d346f8f64527f393f7a42fc11d03fdde64)
- [`MW-2116`](https://jira.open-xchange.com/browse/MW-2116): Added option to use session parameter as a secret source. [`0c1b50d4`](https://gitlab.open-xchange.com/middleware/core/commit/0c1b50d4eb6d3b241fe31a9e6e5b63129e1b51bf)
- Mail: Support dedicated column for user flags to be queried by action=all or action=list request [`86c22aa5`](https://gitlab.open-xchange.com/middleware/core/commit/86c22aa5dc2d59f7197e3ea5e61b527237f1e614)


### Changed

- [`MW-2120`](https://jira.open-xchange.com/browse/MW-2120): Convert Mail User Flags to/from UTF-8 [`b130a436`](https://gitlab.open-xchange.com/middleware/core/commit/b130a436a0b5047eb7ea400fb99393e1ec4ebeb7)
- [`MW-2124`](https://jira.open-xchange.com/browse/MW-2124): allow subscribe/unsubscribe actions via oauth [`1df8ddf8`](https://gitlab.open-xchange.com/middleware/core/commit/1df8ddf8b1044abf13b7e05a463a4a9810fb614e)
- [`MWB-2315`](https://jira.open-xchange.com/browse/MWB-2315): Remove user-specific templates [`9ef5570d`](https://gitlab.open-xchange.com/middleware/core/commit/9ef5570d9e4c3cd997bf6c6bf27a5fb011d7535c)
- [`SCR-1283`](https://jira.open-xchange.com/browse/SCR-1283): Enhanced redis hosts configuration [`c6f1ef04`](https://gitlab.open-xchange.com/middleware/core/commit/c6f1ef04708ab027bf62c6e4aff8b9c4688c7629)
- [`SCR-1285`](https://jira.open-xchange.com/browse/SCR-1285): Updated Netty NIO libraries from v4.1.94 to v4.1.97 [`ca90cc77`](https://gitlab.open-xchange.com/middleware/core/commit/ca90cc77a9f0a82f2c9fdc27c5e24b9932022897) [`d1f792bf`](https://gitlab.open-xchange.com/middleware/core/commit/d1f792bf1530e6da660bcf30748220f37b729b66)
- [`SCR-1286`](https://jira.open-xchange.com/browse/SCR-1286): Updated lettuce library from v6.2.5 to v6.2.6 [`46e37ee3`](https://gitlab.open-xchange.com/middleware/core/commit/46e37ee33a36667950eadf3836d4184804502fef)
- Don't require 'infostore' module permission for mail pdf export [`1afcccae`](https://gitlab.open-xchange.com/middleware/core/commit/1afcccae82f33d17330819fa9b1114fd3a9cd535)


### Fixed

- [`MWB-1730`](https://jira.open-xchange.com/browse/MWB-1730): Process CUs without calendar access [`efce0922`](https://gitlab.open-xchange.com/middleware/core/commit/efce092252200fa2449a682ae2a4f199b7131450)
- [`MWB-1781`](https://jira.open-xchange.com/browse/MWB-1781): Set MySQL client protocol to SOCKET for localhost connections [`f2cff023`](https://gitlab.open-xchange.com/middleware/core/commit/f2cff023e374478169de318bc688d3297b6d1a5d)
- [`MWB-2286`](https://jira.open-xchange.com/browse/MWB-2286): not very helpful error message in case features.definitions is not defined [`77afac3d`](https://gitlab.open-xchange.com/middleware/core/commit/77afac3db82920707712535e0d8a1064ff5ef5ec)
- [`MWB-2287`](https://jira.open-xchange.com/browse/MWB-2287): Orderly detect possible "mail not found" error while checking for referenced mail on reply/forward [`643153ce`](https://gitlab.open-xchange.com/middleware/core/commit/643153ce869f7e40c23ba9effe61e035cbd540dd)
- [`MWB-2290`](https://jira.open-xchange.com/browse/MWB-2290): Ensure "INBOX" folder is translated, too [`682f34a4`](https://gitlab.open-xchange.com/middleware/core/commit/682f34a48f89171ed6a58b140a5415b7f53da72e)
- [`MWB-2294`](https://jira.open-xchange.com/browse/MWB-2294): Socket Logging not working [`108ea815`](https://gitlab.open-xchange.com/middleware/core/commit/108ea815369627f8c58982c7c2cea2a6ed432d38)
- [`MWB-2298`](https://jira.open-xchange.com/browse/MWB-2298): Changed column 'propertyValue' of table 'subadmin_config_properties' to be of type TEXT [`aa35c6d8`](https://gitlab.open-xchange.com/middleware/core/commit/aa35c6d87d7a4e77b939290fbd834fcbde0411cc)
- [`MWB-2299`](https://jira.open-xchange.com/browse/MWB-2299): Handle unsupported image format as illegal image upload [`89635a00`](https://gitlab.open-xchange.com/middleware/core/commit/89635a002189dbec9136ad64ef8d3161df11d2a0)
- [`MWB-2306`](https://jira.open-xchange.com/browse/MWB-2306): Extend the "login" column for "user_mail_account" and "user_transport_account" tables [`2fe842ac`](https://gitlab.open-xchange.com/middleware/core/commit/2fe842ac627685c0ea9b851b3a87b5835884b2b6)
- [`MWB-2307`](https://jira.open-xchange.com/browse/MWB-2307): Don't use config-cascade cache if scope preference has been set [`b4f7aa7b`](https://gitlab.open-xchange.com/middleware/core/commit/b4f7aa7b369ed2419b1711c3681f4e0f3b1a695d)
- [`MWB-2313`](https://jira.open-xchange.com/browse/MWB-2313): Check queried in-compose draft messages against cached ones [`cb82807f`](https://gitlab.open-xchange.com/middleware/core/commit/cb82807f65ef6bd03cf98a2ab86d608da02ffa1e) [`a6ed27f4`](https://gitlab.open-xchange.com/middleware/core/commit/a6ed27f4256b015ab85b25ff5076e45c708ca155)
- [`MWB-2316`](https://jira.open-xchange.com/browse/MWB-2316): Broken link in "Export PDF" documentation [`f87b10e8`](https://gitlab.open-xchange.com/middleware/core/commit/f87b10e8fcc2f6e740664dd4701dcaee5e4fd776)
- [`MWB-2317`](https://jira.open-xchange.com/browse/MWB-2317): Capability is missing in "Export PDF" documentation [`0e96d602`](https://gitlab.open-xchange.com/middleware/core/commit/0e96d60279d7e77c9a8fc87a9d6603b3c08969c6)
- [`MWB-2319`](https://jira.open-xchange.com/browse/MWB-2319): Don't limit POP3 server response when querying UIDLs of
available messages [`55899950`](https://gitlab.open-xchange.com/middleware/core/commit/55899950faeb198837ae3be43a48265a20b7bb51)
- [`MWB-2320`](https://jira.open-xchange.com/browse/MWB-2320): Updated JUnit to 5.10.0 to support Eclipse 2023-09 [`e5d2942f`](https://gitlab.open-xchange.com/middleware/core/commit/e5d2942f0a4b776e2df9d62573a329d3d74fd048)
- [`MWB-2321`](https://jira.open-xchange.com/browse/MWB-2321): Removed persistence section in values.yaml [`41b31013`](https://gitlab.open-xchange.com/middleware/core/commit/41b31013dfee49867efb44ef810390b174efebc6)
- [`MWB-2324`](https://jira.open-xchange.com/browse/MWB-2324): Restored parsing of erroneous token refresh responses [`afaad652`](https://gitlab.open-xchange.com/middleware/core/commit/afaad652e0dd71a2e2d528b97c5f450689f1472e)


### Removed

- [`MW-2093`](https://jira.open-xchange.com/browse/MW-2093): Removed Twitter integration [`8e1d53e4`](https://gitlab.open-xchange.com/middleware/core/commit/8e1d53e4384b7430621dd43e1c7c76b688c9294f)


### Security

- [`MWB-2315`](https://jira.open-xchange.com/browse/MWB-2315): CVE-2023-29051 [`d7041266`](https://gitlab.open-xchange.com/middleware/core/commit/d7041266aab43f689bb7cde251b42535c8aa9be3)


## [8.17.0] - 2023-08-30

### Added

- [`MW-2016`](https://jira.open-xchange.com/browse/MW-2016): added deployment role 'businessmobility' for USM/EAS deployments [`01bfa6d`](https://gitlab.open-xchange.com/middleware/core/commit/01bfa6d5b9cc77ef4945d0369bcf853120a729db)

### Changed

- [`MW-2003`](https://jira.open-xchange.com/browse/MW-2003): Handle Time Transparency of Appointments per User
  * Added `transp` field to attendee
  * Handle transparencies set via CalDAV clients [`12aee31`](https://gitlab.open-xchange.com/middleware/core/commit/12aee3165b9745ee8b32a873c5488b8b2e2b427d)
- [`SCR-1270`](https://jira.open-xchange.com/browse/SCR-1270): Updated Google API Client libraries [`800dc9f`](https://gitlab.open-xchange.com/middleware/core/commit/800dc9f9e2ed7232072303070dc15ac6f3be9603)
- [`MWB-2259`](https://jira.open-xchange.com/browse/MWB-2259): Added more DEBUG and INFO logging for GDPR data export [`39f74ae`](https://gitlab.open-xchange.com/middleware/core/commit/39f74aeac251eb68f4c7f4d9eb5d0769c3db407a) [`ab77d3e`](https://gitlab.open-xchange.com/middleware/core/commit/ab77d3e20b1ae846096bee55969b6f97a68b5c23)
- [`SCR-1275`](https://jira.open-xchange.com/browse/SCR-1275): Upgraded MySQL Connector for Java from v8.0.29 to v8.0.33 [`76146ce`](https://gitlab.open-xchange.com/middleware/core/commit/76146cef4dd60d7d5f0bc1b9a9da9c78d18bd12f)

### Fixed

- [`MWB-2266`](https://jira.open-xchange.com/browse/MWB-2266): Extremely long-running requests are not terminated [`f9e86fc`](https://gitlab.open-xchange.com/middleware/core/commit/f9e86fcf2146ea73fb00adfda3cb69d95b62a987) [`4bce9cd`](https://gitlab.open-xchange.com/middleware/core/commit/4bce9cdb77513bb48d02a37655f13df64be8ad94) [`e434d32`](https://gitlab.open-xchange.com/middleware/core/commit/e434d323cead7ecccc2a363844d15652a866b202) [`4f05fa1`](https://gitlab.open-xchange.com/middleware/core/commit/4f05fa1e8fd91503d7cc909bd26fa24304c8d7f6) [`cc0833b`](https://gitlab.open-xchange.com/middleware/core/commit/cc0833b73d66157f673e0f3ce1829d9206422c5c) [`38fea46`](https://gitlab.open-xchange.com/middleware/core/commit/38fea46fe2d082f2073971b3cf24090d71b7c24f) [`2be0655`](https://gitlab.open-xchange.com/middleware/core/commit/2be0655fff1c3cabb58069dc205cb73f78ac7256) [`d3bd8f2`](https://gitlab.open-xchange.com/middleware/core/commit/d3bd8f2b8518053ab1d4e81c5b5210a7f7c0632a) [`f85638f`](https://gitlab.open-xchange.com/middleware/core/commit/f85638f83e0bb64c3a77281799e50c194fb1e021) [`d494263`](https://gitlab.open-xchange.com/middleware/core/commit/d49426374e0824d1acedb066d1cfe1168f58cea0)
  * Hard timeout of 1h for tracked requests of any kind & hard timeout of 60 seconds for mail compose related communication with primary mail backend
  * Introduced wait time for concurrent operations. If elapsed, the operation is aborted
  * Use Apache FreeMarker template engine with safe configuration 
- [`MWB-2242`](https://jira.open-xchange.com/browse/MWB-2242): Take over selected filestore id properly during user
creation
  * [`SCR-1264`](https://jira.open-xchange.com/browse/SCR-1264): Update task to insert missing references into
'filestore2user' table [`867b465`](https://gitlab.open-xchange.com/middleware/core/commit/867b46566664bad5111a4a42335d57c399bb874a)
- [`MWB-2249`](https://jira.open-xchange.com/browse/MWB-2249): properly disable context during filestore move [`92e1649`](https://gitlab.open-xchange.com/middleware/core/commit/92e16494408504ebb62511de47a28124c506b432)
- [`MW-2094`](https://jira.open-xchange.com/browse/MW-2094): Backwards compatibility for extra metadata in sieve scripts [`dfee773`](https://gitlab.open-xchange.com/middleware/core/commit/dfee77322511a36e99849dae8c64689777c39c1d)
- [`MWB-2275`](https://jira.open-xchange.com/browse/MWB-2275): Yield cloned objects from Caching LDAP Contacts Access [`f4d0b36`](https://gitlab.open-xchange.com/middleware/core/commit/f4d0b36c7e7429e5d78832bf259554b052f53968)
- [`MWB-2250`](https://jira.open-xchange.com/browse/MWB-2250): Added sanity check for Task Status. [`1858544`](https://gitlab.open-xchange.com/middleware/core/commit/18585443f2d73e14226ae52a5c680b74e5dd63e2)
- [`MWB-2272`](https://jira.open-xchange.com/browse/MWB-2272): Explicitly LIST a folder once not contained in `LIST "" "*"` queried from IMAP server [`ab95588`](https://gitlab.open-xchange.com/middleware/core/commit/ab955885ca573a037e801b06a2616321e3cb6bb2)
- [`MWB-2265`](https://jira.open-xchange.com/browse/MWB-2265): Prefer to use config-cascade-wise configured value for `com.openexchange.imap.imapSupportsACL` [`c01a70a`](https://gitlab.open-xchange.com/middleware/core/commit/c01a70aa5fe900c8bfbb6abaf262f68ff1ce5967)
- [`MWB-2274`](https://jira.open-xchange.com/browse/MWB-2274): Properly encode dynamically inserted part of LDAP
folder filters [`91fe39e`](https://gitlab.open-xchange.com/middleware/core/commit/91fe39ea2e5dda8e451afb181a1092f34b9ca740)
- [`MWB-2277`](https://jira.open-xchange.com/browse/MWB-2277): Changed displayed error messages according to customer's suggestion [`7754cad`](https://gitlab.open-xchange.com/middleware/core/commit/7754cad2275a6761b6332ee3fd0900b687299ba0)
- [`MWB-2242`](https://jira.open-xchange.com/browse/MWB-2242): Corrected invocation for 'list_unassigned' in filestore [`08481d7`](https://gitlab.open-xchange.com/middleware/core/commit/08481d79bf774fa03730b2a68f13fcafbec9894f)
- [`MWB-2280`](https://jira.open-xchange.com/browse/MWB-2280): Reset attendee transparency on rescheduling [`2f13573`](https://gitlab.open-xchange.com/middleware/core/-/commit/2f135730dd474d7c1746fefe8c2e2647511a3d51)

### Security

- [`MWB-2261`](https://jira.open-xchange.com/browse/MWB-2261): CVE-2023-29048 [`cf6b2f1`](https://gitlab.open-xchange.com/middleware/core/commit/cf6b2f12e2104c3f896aab9015331a8878997fae) [`1ec3707`](https://gitlab.open-xchange.com/middleware/core/commit/1ec37077ac77235bd15867dd2e80918b7f2482de)

## [8.16.0] - 2023-08-01

### Added

- [`ASP-131`](https://jira.open-xchange.com/browse/ASP-131): Implemented a MailExportService that converts e-mails to PDFs
  * [`SCR-1235`](https://jira.open-xchange.com/browse/SCR-1235): Introduced a new action to the 'mail' module for exporting mails as PDFs
  * [`SCR-1236`](https://jira.open-xchange.com/browse/SCR-1236): Introduced new properties for the MailExportService
  * [`SCR-1237`](https://jira.open-xchange.com/browse/SCR-1237): Introduced new properties for the CollaboraMailExportConverter
  * [`SCR-1238`](https://jira.open-xchange.com/browse/SCR-1238): Introduced new properties for the GotenbergMailExportConverter
  * [`SCR-1239`](https://jira.open-xchange.com/browse/SCR-1239): Introduced new properties for the CollaboraPDFAConverter
  * [`SCR-1240`](https://jira.open-xchange.com/browse/SCR-1240): Introduced a new capability to activate the PDF MailExportService [`4d0de04`](https://gitlab.open-xchange.com/middleware/core/commit/4d0de0467de41aa62795062d5e6e1c88e8d5eeeb)
- [`MW-2036`](https://jira.open-xchange.com/browse/MW-2036): added contact collector documentation [`5441175`](https://gitlab.open-xchange.com/middleware/core/commit/5441175fda9857746dcb0eb13ae99d18ede227f6)
- [`MW-2073`](https://jira.open-xchange.com/browse/MW-2073): Log any HTTP header [`852548b`](https://gitlab.open-xchange.com/middleware/core/commit/852548b26bca8f25b82fd339edb3bddbd77393cf)
- [`MWB-2238`](https://jira.open-xchange.com/browse/MWB-2238): allow to configure a purge folder for trash deletion
  * The property com.openexchange.imap.purgeFolder allows to configure a
parent folder for renamed trash folder. If one of those folders is
configured then the trash is not deleted by the middleware itself. [`f870dcc`](https://gitlab.open-xchange.com/middleware/core/commit/f870dcc2a9b82fbdf575130d7b98bb769f189448)
- Add missing configuration for new packages [`c8651ec`](https://gitlab.open-xchange.com/middleware/core/commit/c8651ec979a9d9f2ffffef6ed24db5ac92f8949c)

### Changed

- Improve markdown for core-mw chart [`6518c42`](https://gitlab.open-xchange.com/middleware/core/commit/6518c42efb82472f3865feab5f9dcc7ceb4ec678)
- [`MW-1862`](https://jira.open-xchange.com/browse/MW-1862): Upgrade encrypted data dynamically during usage
  * Throw exception if legacy encryption is detected in CryptoService
  * Services/storages detect legacy encryption by this exception and recrypt secrets themselves (by using async task)
  * If shared item protected by secret with legacy encryption is accessed, use LegacyCrypto and log this event (not possible to recrypt here)
  * When users logs in, all items shared by him are collected and checked if secrets needs to be recrypted
- [`SCR-1233`](https://jira.open-xchange.com/browse/SCR-1233): Update encryption for passwords of anonymous guest users [`d5843c4`](https://gitlab.open-xchange.com/middleware/core/commit/d5843c4734abfdffd8a8fb95379db72afe78e9bb)
- [`MW-1840`](https://jira.open-xchange.com/browse/MW-1840): Reworked the CryptoService
  * changed the encrypting algorithm to AES/GCM/NoPadding
  * deprecated the encrypt and decrypt methods with the old mechanisms
  * removed default salting - Now callers are responsible for their salts
  * introduced fallbacks for the old mechanics
  * [`MW-1894`](https://jira.open-xchange.com/browse/MW-1894): moved CryptoUtil to c.o.java, replaced all instances of SecureRandom with the centralised version [`7a3e3e5`](https://gitlab.open-xchange.com/middleware/core/commit/7a3e3e542588489b1b39847c6b4ec21474d8be62)
- [`MW-1861`](https://jira.open-xchange.com/browse/MW-1861): Use Implicit Salt in CryptoService
  * Utilise argon2i for password hashing
  * Use the legacy crypto for the Key-based methods
  * Let the callers dictate the byte size for salt and iv
  * Use a 96bit key for IV
  * Re-create secure random after a specified amount of time
  * Use implicit salt and IV in CryptoService [`2faf2ca`](https://gitlab.open-xchange.com/middleware/core/commit/2faf2cadf657d152eb0124e000deb4ebaa6d784c)
- [`SCR-1252`](https://jira.open-xchange.com/browse/SCR-1252): Updated Netty NIO libraries from v4.1.89 to v4.1.94 [`8c7eb32`](https://gitlab.open-xchange.com/middleware/core/commit/8c7eb3272baff42bc2f4f2fe0498b792703cfa6e)
- [`SCR-1247`](https://jira.open-xchange.com/browse/SCR-1247): Updated pushy library from v0.15.1 to v0.15.2 [`8365cff`](https://gitlab.open-xchange.com/middleware/core/commit/8365cffa89d215a934577b4154c46d08d3dc25a2)
- [`SCR-1245`](https://jira.open-xchange.com/browse/SCR-1245): Updated metadata-extractor from v2.17.0 to v2.18.0 [`bd4e29c`](https://gitlab.open-xchange.com/middleware/core/commit/bd4e29c5b5c1f0582a3b5ef34306f89bff73ed18)
- [`SCR-1253`](https://jira.open-xchange.com/browse/SCR-1253): Updated lettuce library from v6.2.3 to v6.2.5 [`731ca0b`](https://gitlab.open-xchange.com/middleware/core/commit/731ca0b9afb8bd1f9f03f9c06ef81aa573e4a931)
- [`SCR-1246`](https://jira.open-xchange.com/browse/SCR-1246): Updated Google Guava from v31.1 to v32.1.1 [`1cbe1a4`](https://gitlab.open-xchange.com/middleware/core/commit/1cbe1a45f6c6078b5eada6a1d464d1c4cc4935d5)
- [`SCR-1231`](https://jira.open-xchange.com/browse/SCR-1231): Updated OSGi target platform bundles [`2a0ea4e`](https://gitlab.open-xchange.com/middleware/core/commit/2a0ea4e6b361eaf87602846870dccd1bacaa0da6)
- [`MWB-2208`](https://jira.open-xchange.com/browse/MWB-2208): Do log possible IMAP protocol errors while trying
to acquire a part's content [`8867c1b`](https://gitlab.open-xchange.com/middleware/core/commit/8867c1bbe795931260e5701849d5856726ccf5f4)
- [`SCR-1255`](https://jira.open-xchange.com/browse/SCR-1255): Updated Apache Tika library from v2.6.0 to v2.8.0 [`d19b0fc`](https://gitlab.open-xchange.com/middleware/core/commit/d19b0fcafa6ec70314b6805821486591014795d8)
- [`SCR-1256`](https://jira.open-xchange.com/browse/SCR-1256): Upgraded Javassist to 3.29.2-GA [`6a6ac84`](https://gitlab.open-xchange.com/middleware/core/commit/6a6ac84f4e92adf56c78eb50ba1c89755b901466)
- [`SCR-1244`](https://jira.open-xchange.com/browse/SCR-1244): Updated htmlcleaner from v2.22 to v2.29 [`ee140df`](https://gitlab.open-xchange.com/middleware/core/commit/ee140df60262cbf580728f85489366d473165b04)
- [`SCR-1243`](https://jira.open-xchange.com/browse/SCR-1243): Updated dnsjava from v3.5.1 to v3.5.2 [`558227a`](https://gitlab.open-xchange.com/middleware/core/commit/558227a19aec69c24fc85142f5566e7b0ede3f8a)

### Removed

- [`SCR-1254`](https://jira.open-xchange.com/browse/SCR-1254): removed support for content_disposition=inline and delivery=view parameter [`5d8bfd4`](https://gitlab.open-xchange.com/middleware/core/commit/5d8bfd43e8a1fd2bede82cbceceddf05488a2837)

### Fixed
- [`MWB-2258`](https://jira.open-xchange.com/browse/MWB-2258): Adjust 'credentials' table for enhanced crypto service
- [`SCR-1267`](https://jira.open-xchange.com/browse/SCR-1267): Extend password columns in db to store encrypted passwords [`e6cdc21`](https://gitlab.open-xchange.com/middleware/core/-/commit/e6cdc218437c4f7aed902ffc52b240d6af6aa126)
- [`MWB-2253`](https://jira.open-xchange.com/browse/MWB-2253): removed unused import  [`804a806`](https://gitlab.open-xchange.com/middleware/core/commit/804a806dbda5ea0b0783252d8f138b66cbb0ef76)
  * to fix not working imageconverter and documentconverter
- [`MWB-2252`](https://jira.open-xchange.com/browse/MWB-2252): Keep possible HTML comment markers when examining CSS [`62add69`](https://gitlab.open-xchange.com/middleware/core/commit/62add691dc3bb96282666a726df411ab86902d4c)
- [`MWB-2251`](https://jira.open-xchange.com/browse/MWB-2251): Prefer configured call-back URL regardless of [`df0ed24`](https://gitlab.open-xchange.com/middleware/core/commit/df0ed24f52cb12b129bd72fca6ea1ec1b2209fe6)
  * applicable dispatcher prefix
- [`MWB-2186`](https://jira.open-xchange.com/browse/MWB-2186): The upload of big files gets slower and slower (against MW 8.x) [`d55359b`](https://gitlab.open-xchange.com/middleware/core/commit/d55359b3eead71013b050c3ed3cc5bc5dd487940) [`0efa733`](https://gitlab.open-xchange.com/middleware/core/commit/0efa733b00bcd143a7e703e06fb85ee34502f1df)
- properly load reseller service on demand [`3166ed3`](https://gitlab.open-xchange.com/middleware/core/commit/3166ed3d51b66010a88fe96be70d286b2184f804)
- [`MWB-2228`](https://jira.open-xchange.com/browse/MWB-2228): Move EventsContactHalo into com.openexchange.halo.chronos bundle [`8171cd7`](https://gitlab.open-xchange.com/middleware/core/commit/8171cd7f2b096db29de4500ead040e520402fa25)
- [`MWB-2221`](https://jira.open-xchange.com/browse/MWB-2221): Append additionally available plain text content to existent one [`738b68c`](https://gitlab.open-xchange.com/middleware/core/commit/738b68c57164d30d7a739b22fb2d825102aca71c)
- [`MWB-2184`](https://jira.open-xchange.com/browse/MWB-2184): Add support for extraStatefulSetProperties and make use of ox-common.pods.podSpec [`92f6ce9`](https://gitlab.open-xchange.com/middleware/core/commit/92f6ce9e9c5c52b668e4727ee4e9cb28f7782c73)
- [`MWB-2240`](https://jira.open-xchange.com/browse/MWB-2240): Don't output inline images as attachment [`8de296c`](https://gitlab.open-xchange.com/middleware/core/commit/8de296ce750992941ca08c3726e0b89726c33c9f)
- [`MW-2203`](https://jira.open-xchange.com/browse/MW-2203): Omit OS version for web clients [`fca25d2`](https://gitlab.open-xchange.com/middleware/core/commit/fca25d2ec442de057ccfb23c3037eae84598ab3c)
- [`MWB-2228`](https://jira.open-xchange.com/browse/MWB-2228): Move contact halo into com.openxchange.server bundle [`691da48`](https://gitlab.open-xchange.com/middleware/core/commit/691da48ec7ad8fcf15267505445bed9dafdcbbfd)
- [`MWB-2231`](https://jira.open-xchange.com/browse/MWB-2231): Confirmation buttons not working when inviting a person
to a series exception [`19f8753`](https://gitlab.open-xchange.com/middleware/core/commit/19f87531c8c7ead6125d3fd58233eab34b99d2c7)
- [`MWB-2248`](https://jira.open-xchange.com/browse/MWB-2248): Pass proper range when querying messages from contained sub-accounts if Unified Mail [`f9c8829`](https://gitlab.open-xchange.com/middleware/core/commit/f9c8829daf658afe229d3c626f2b24b6de46263d)
- [`MWB-2227`](https://jira.open-xchange.com/browse/MWB-2227): Attendee cannot be re-invited to occurrence of event
series [`553bb34`](https://gitlab.open-xchange.com/middleware/core/commit/553bb34c8391dcd5a26c7b57eea6b4c1ca3c53a2)
- [`MWB-2233`](https://jira.open-xchange.com/browse/MWB-2233): Removed ulimit configuration from start script [`128ba0b`](https://gitlab.open-xchange.com/middleware/core/commit/128ba0badb30393f0ccce020e01cac1747e24d77)
- [`MWB-2241`](https://jira.open-xchange.com/browse/MWB-2241): Lowered log level to DEBUG when moving active/idle
sessions to first short-term session container [`82938e9`](https://gitlab.open-xchange.com/middleware/core/commit/82938e9301c3bc0ad3b3deb1e0acf18f6c7ab3dc)
- [`MWB-2223`](https://jira.open-xchange.com/browse/MWB-2223): convert all images with CID for the html body [`c7776e1`](https://gitlab.open-xchange.com/middleware/core/commit/c7776e18c6805b82a5356b0c6758cca9e38c8808)
- [`MWB-2210`](https://jira.open-xchange.com/browse/MWB-2210): Consider virtual folders when getting attachments
through chronos module [`d992d75`](https://gitlab.open-xchange.com/middleware/core/commit/d992d754f25d08290112a9883cf7ae77d9f04a81)

## [8.15.0] - 2023-07-05

### Added

- [`MW-2045`](https://jira.open-xchange.com/browse/MW-2045): Introduced separate bundle for parsing a schedule expression and for initiating periodic tasks. Refactored database clean-up framework to have a "maintenance" window, in which execution of general clean-up jobs is permitted. It also accepts custom clean-up jobs having their own schedule. [`8b9bb19`](https://gitlab.open-xchange.com/middleware/core/commit/8b9bb19e38f5de4bd43794147408ce3c9ea42976)
- [`MW-2020`](https://jira.open-xchange.com/browse/MW-2020): Dedicated simple HTTP liveness end-point for early access to liveness check & await availability of database during start-up [`a476d76`](https://gitlab.open-xchange.com/middleware/core/commit/a476d768fceb142c7d20199b831c4d5aa82dfab1)
- [`MW-1084`](https://jira.open-xchange.com/browse/MW-1084): Added support for segmented updates with OIDC [`2277d3a`](https://gitlab.open-xchange.com/middleware/core/commit/2277d3a64587fe66b6ee38ca86bd07fd5aac1ca0)
- [`MW-2073`](https://jira.open-xchange.com/browse/MW-2073): Log any HTTP header[`6bdd0d5`](https://gitlab.open-xchange.com/middleware/core/commit/6bdd0d534313135ce7b62748365def6971a8de04)

### Changed

- [`MWB-2212`](https://jira.open-xchange.com/browse/MWB-2212): Allow specifying deferrer URL with path [`cab25e7`](https://gitlab.open-xchange.com/middleware/core/commit/cab25e7b4ac6775dc5039d7f03e42bad3441a8df)
- [`MWB-2200`](https://jira.open-xchange.com/browse/MWB-2200): Output JSON session representation if it becomes too big [`118f0db`](https://gitlab.open-xchange.com/middleware/core/commit/118f0db9be9f7bb52da4e57c05403f2810562a9c)
- [`MWB-2059`](https://jira.open-xchange.com/browse/MWB-2059): Improved access to queried message range in case IMAP server does not support SORT capability [`fffe20c`](https://gitlab.open-xchange.com/middleware/core/commit/fffe20c32f915db0ae2902cb3349b86d08b66ce1)
- [`DOCS-4766`](https://jira.open-xchange.com/browse/DOCS-4766): Include pdftool from docker image [`4d9d0ad`](https://gitlab.open-xchange.com/middleware/core/commit/4d9d0adfa7335e5b440a3505bbfcc6635b16e97f)
- Enhance session representation managed in Redis storage by user database schema [`3798214`](https://gitlab.open-xchange.com/middleware/core/commit/37982143af42b3f3067c95e4f7a049c5f79083f9)
- Enhance session representation managed in Redis storage by segment marker (that is the target database schema by now) [`c008e24`](https://gitlab.open-xchange.com/middleware/core/commit/c008e24395266689ca32390435440364b20890ef)
- [`MWB-2214`](https://jira.open-xchange.com/browse/MWB-2214): Improved error handling in case a `javax.mail.FolderNotFoundException` occurs [`eb5a9f1`](https://gitlab.open-xchange.com/middleware/core/commit/eb5a9f18c6b2890320aeebb11326173c2a370891)

### Fixed

- [`MWB-2193`](https://jira.open-xchange.com/browse/MWB-2193): missed to remove deprecated servlet path to admin API.
  * removed servlet path registration for obsolete path
  * removed obvious parts related to AXIS2 [`017321e`](https://gitlab.open-xchange.com/middleware/core/commit/017321eb1015288b1cf267f284047e7724d40f7f)
- [`MW-2050`](https://jira.open-xchange.com/browse/MW-2050): Refactored message alarm delivery worker to orderly use database locks [`c99b0b5`](https://gitlab.open-xchange.com/middleware/core/commit/c99b0b5c280ad7f204c77fbed5828ed1f986b921)
- [`MWB-2130`](https://jira.open-xchange.com/browse/MWB-2130): Try to perform hard-delete by delete-through-rename [`db8afce`](https://gitlab.open-xchange.com/middleware/core/commit/db8afce5e128665a4fa5e0a164975d06d7b5a9b2)
- [`MWB-2182`](https://jira.open-xchange.com/browse/MWB-2182): Fixed typo "(E|e)xcpetion" in code [`b054b35`](https://gitlab.open-xchange.com/middleware/core/commit/b054b35e14fa7aab86137485b8b406a773d6b1c8)
- [`MWB-2130`](https://jira.open-xchange.com/browse/MWB-2130): Try to perform hard-delete by delete-through-rename [`54ac301`](https://gitlab.open-xchange.com/middleware/core/commit/54ac301a665082498078c88397f943512b91bb24)
- [`MWB-2201`](https://jira.open-xchange.com/browse/MWB-2201): Do translate standard folders of secondary accounts as well [`b549cf4`](https://gitlab.open-xchange.com/middleware/core/commit/b549cf41bbbd8dc60800686a99aaba6b83a75772)
- [`MWB-2196`](https://jira.open-xchange.com/browse/MWB-2196): Pay respect to order parameter when sorting contacts by special sorting [`1db09a3`](https://gitlab.open-xchange.com/middleware/core/commit/1db09a31d2714f2016579d79adc97e5735fc8cf9)
- [`MWB-2168`](https://jira.open-xchange.com/browse/MWB-2168): Support AWS S3 IMAP role using `AWS_WEB_IDENTITY_TOKEN_FILE` environment variable [`2b35ea8`](https://gitlab.open-xchange.com/middleware/core/commit/2b35ea856e1c0a81f69fac93bfa46f462736a805)[`2d9ad76`](https://gitlab.open-xchange.com/middleware/core/commit/2d9ad769cd24c179edb123b7182fd1f2109adb9f)
- [`MWB-2187`](https://jira.open-xchange.com/browse/MWB-2187): Add necessary imports [`61dd61e`](https://gitlab.open-xchange.com/middleware/core/commit/61dd61e4f850a3d4defee96218b44c275110868e) [`51eb12f`](https://gitlab.open-xchange.com/middleware/core/commit/51eb12f0200bcfd516bcd4427df960e0e8d241a8)
- [`MWB-2181`](https://jira.open-xchange.com/browse/MWB-2181): Fixed possible null dereference [`15519ca`](https://gitlab.open-xchange.com/middleware/core/commit/15519cab778d61c5f800d9e3b4f7dbc0a10122b3) [`f059c8d`](https://gitlab.open-xchange.com/middleware/core/commit/f059c8d04c74af422d150a6cd1961efbe01e2161)
- [`MWB-2187`](https://jira.open-xchange.com/browse/MWB-2187): Assume configured IMAP host for IMAP authentication does not need to be checked against blocked hosts (see `com.openexchange.mail.account.blacklist`) [`0971c88`](https://gitlab.open-xchange.com/middleware/core/commit/0971c8845fffd6469ca9e8f6bc75e00cb0937917)
- [`MWB-2189`](https://jira.open-xchange.com/browse/MWB-2189): Orderly close database statements [`083f2c3`](https://gitlab.open-xchange.com/middleware/core/commit/083f2c331fc2b5c36ccebf80b7a21e060a8dd6f3)
- [`MWB-2199`](https://jira.open-xchange.com/browse/MWB-2199): Mention the affected YAML file if an invalid format is detected [`1b4a086`](https://gitlab.open-xchange.com/middleware/core/commit/1b4a08654cf5467304faf52b63136c438d3b1c3a)
- [`MWB-2178`](https://jira.open-xchange.com/browse/MWB-2178): Handle possible null session on account retrieval [`357cc79`](https://gitlab.open-xchange.com/middleware/core/commit/357cc79d366ebaa1803663ae7da528d1c3415fb0)
- [`MWB-2045`](https://jira.open-xchange.com/browse/MWB-2045): Omit specific OS version for macOS clients (2) [`78a60c1`](https://gitlab.open-xchange.com/middleware/core/commit/78a60c1c0452d7388255e26603496839d57736bd)
- [`MWB-2194`](https://jira.open-xchange.com/browse/MWB-2194): Fixed typo in property description [`b71221f`](https://gitlab.open-xchange.com/middleware/core/commit/b71221f91b98d1b548013b110c209f03060e1674)
- [`MWB-2179`](https://jira.open-xchange.com/browse/MWB-2179): Orderly handle iTip request without method [`58fbf02`](https://gitlab.open-xchange.com/middleware/core/commit/58fbf02aed853638ef42b982983444cde0e8603a)
- [`MWB-2180`](https://jira.open-xchange.com/browse/MWB-2180): Check for possible null return value when looking-up a user with invalid user identifier [`44c3ede`](https://gitlab.open-xchange.com/middleware/core/commit/44c3ede50e4b445cb90522b9762a7130f958acec)
- [`MWB-2185`](https://jira.open-xchange.com/browse/MWB-2185): Use SMTP default settings when changing a user's assigned SMTP server [`d1c73cb`](https://gitlab.open-xchange.com/middleware/core/commit/d1c73cba67746b7a63041877625e6712d6042106)
- [`MWB-1764`](https://jira.open-xchange.com/browse/MWB-1764): Don't check against blocked hosts/allowed ports when obtaining status for subscribed mail accounts [`2e7f30a`](https://gitlab.open-xchange.com/middleware/core/commit/2e7f30afeb7a942129b72a2721b59d3483fcc19c)
- [`MWB-2214`](https://jira.open-xchange.com/browse/MWB-2214): Try to re-open folder in case a `javax.mail.FolderNotFoundException` occurs (IMAP folder not LISTed, but SELECTable) [`d60a70c`](https://gitlab.open-xchange.com/middleware/core/commit/d60a70c36a1b85ea921c96b45cb3260c4df1b92e)


## [8.14.0] - 2023-06-06

### Added

- [`MW-1545`](https://jira.open-xchange.com/browse/MW-1545): Option to hide own Free/Busy time
  * Users can now configure whether their free/busy data is exposed to others (values `all`, `none`, `internal-only`)
  * Appointments that are visible by other means (shared folder, common participation) continue to be visible
  * Default value of setting is `all`, configurable and protectable ([`SCR-1197`](https://jira.open-xchange.com/browse/SCR-1197)), and exposed to clients in JSlob ([`SCR-1198`](https://jira.open-xchange.com/browse/SCR-1198)) [`e5d91c8`](https://gitlab.open-xchange.com/middleware/core/commit/e5d91c83f05e2c7d9b810cf8c9eafe74b8b158a5)
- [`MW-1981`](https://jira.open-xchange.com/browse/MW-1981): Added caching to the resource storage [`ed81544`](https://gitlab.open-xchange.com/middleware/core/commit/ed815443de45c8e13114432bf7fb2b248c4a1ec7)
- [`SCR-1213`](https://jira.open-xchange.com/browse/SCR-1213): Introduced event flag 'all_others_declined' to indicate if one might be alone in a meeting [`ae51f2c`](https://gitlab.open-xchange.com/middleware/core/commit/ae51f2c1b41e370a852c373c2e4cdecbc1b6dd41)

### Changed

- [`MW-2007`](https://jira.open-xchange.com/browse/MW-2007): Remove man pages from image [`85e335d`](https://gitlab.open-xchange.com/middleware/core/commit/85e335de270a58b8da37186efb7d8572c9ab7643)
- [`SCR-1219`](https://jira.open-xchange.com/browse/SCR-1219): Upgraded JSoup library in target platform (con.openexchange.bundles) from v1.15.3 to v1.16.1 [`4d3cbc5`](https://gitlab.open-xchange.com/middleware/core/commit/4d3cbc557b2286b3fc17bb37c4722efec82d601e)
- [`INF-173`](https://jira.open-xchange.com/browse/INF-173): Disable `open-xchange-dataretention-csv` by default [`9048c7d`](https://gitlab.open-xchange.com/middleware/core/commit/9048c7d215b9c21b2d687191cd56fb500610d799)

### Fixed

- [`MWB-2160`](https://jira.open-xchange.com/browse/MWB-2160): Avoid excessive parsing of E-Mail addresses possibly containing CFWS personal names; e.g. `&lt;bob@example.com&gt; (Bob Smith)` [`2fb55a6`](https://gitlab.open-xchange.com/middleware/core/commit/2fb55a6edb6b8e2ceab0c3d70dc036460a177c96) [`2ed855c`](https://gitlab.open-xchange.com/middleware/core/commit/2ed855c76cbe58b6e949a1f2d1209c83c2960bc3)
- [`MWB-2150`](https://jira.open-xchange.com/browse/MWB-2150): Don't expunge messages from POP3 storage that could not be added to backing primary mail storage [`6cf89a7`](https://gitlab.open-xchange.com/middleware/core/commit/6cf89a7422607bc7ecea51bbc14cf33c019d7dbd)
- [`MWB-2156`](https://jira.open-xchange.com/browse/MWB-2156): Make DAV UserAgents configurable
  * Also add the new user agent part `dataaccessd` to properly recognize the Mac Calendar clients
  * Introduced new `com.openexchange.dav.useragent.*` properties, see also [`SCR-1220`](https://jira.open-xchange.com/browse/SCR-1220) for details [`e46c9a6`](https://gitlab.open-xchange.com/middleware/core/commit/e46c9a6ce428f239caa6f0d0febd75e798d72b5b)
- [`MWB-2158`](https://jira.open-xchange.com/browse/MWB-2158): Allow all folder names for iCAL feeds [`94c0f36`](https://gitlab.open-xchange.com/middleware/core/commit/94c0f36e5aa9dbe98f9a676d4bcc4b3cccd850f4)
- [`MWB-2149`](https://jira.open-xchange.com/browse/MWB-2149): Prepare refreshing of subscriptions in a blocking manner to avoid having underlying HTTP being being recycled [`1bb9343`](https://gitlab.open-xchange.com/middleware/core/commit/1bb9343bebfa1af856bb30d4ec3021f103dd13ef)
- [`MWB-2171`](https://jira.open-xchange.com/browse/MWB-2171): Split orphan instances on import [`2db7d02`](https://gitlab.open-xchange.com/middleware/core/commit/2db7d02f5296c843271157bef9b2cd4cf1276c25)
- [`MWB-2167`](https://jira.open-xchange.com/browse/MWB-2167): Offered parameter and config option for sanitizing CSV cell content on contact export [`8b1d684`](https://gitlab.open-xchange.com/middleware/core/commit/8b1d684d4b40cab8f09554e7e63019eb4b42cb50)
- [`MWB-2137`](https://jira.open-xchange.com/browse/MWB-2137): Unable to Delete Contacts Account if Implementation Missing [`883b9bd`](https://gitlab.open-xchange.com/middleware/core/commit/883b9bdce4124fd4e3cb86fe46aa69b13b7e60ad)
- **Redis Session Storage**: Use `tags` to differentiate between common and brand-specific session metrics [`6655f6f`](https://gitlab.open-xchange.com/middleware/core/commit/6655f6f205aea0cb508d181c2447e524c2778354)
- [`MWB-2144`](https://jira.open-xchange.com/browse/MWB-2144): Disabled Hazelcast-based session test since Hazelcast has been replaced by Redis [`cab9736`](https://gitlab.open-xchange.com/middleware/core/commit/cab9736ee0e2de4d6b5965baa6a019ffae142293)
- [`MWB-2161`](https://jira.open-xchange.com/browse/MWB-2161): Allow relative paths in yaml file names [`9dd17f3`](https://gitlab.open-xchange.com/middleware/core/commit/9dd17f370ee6ec2a1e5f90f4e10fac476b82c4b3)
- [`MWB-2162`](https://jira.open-xchange.com/browse/MWB-2162): Limit number of considered filestore candidates to a reasonable amount when determining the filestore to use for a new context/user [`eb9e0ca`](https://gitlab.open-xchange.com/middleware/core/commit/eb9e0ca1236dcff0dccab5b5c3c9dc5cb6c1be7a) [`c9b4b4d`](https://gitlab.open-xchange.com/middleware/core/commit/c9b4b4dbb77aeafac395f514c5a49adedfd37daf)
- [`MWB-2139`](https://jira.open-xchange.com/browse/MWB-2139): Check a session's origin for both - guest and application-specific authentication - prior to validating mail access' authentication data [`43229c2`](https://gitlab.open-xchange.com/middleware/core/commit/43229c21deb10a443702c2cfc59267baa7af1b5e)
- [`MWB-2153`](https://jira.open-xchange.com/browse/MWB-2153): Test for `application/x-pkcs7-signature` as well as `application/pkcs7-signature` [`e99052d`](https://gitlab.open-xchange.com/middleware/core/commit/e99052d966e7d002fe5ca9a2fe812716d87f2816)
- [`MWB-2165`](https://jira.open-xchange.com/browse/MWB-2165): Keep quotes in local part of an E-Mail address when extracted from ENVELOPE fetch item [`afdece9`](https://gitlab.open-xchange.com/middleware/core/commit/afdece9846d1a9a72e718d8c084edd4b1d083ee9) [`57df52f`](https://gitlab.open-xchange.com/middleware/core/commit/57df52f0b30420b6c37a56651d0a24ec76b11202)
- Prevent invalid Resource Names for new CalDAV Collections [`c7fae63`](https://gitlab.open-xchange.com/middleware/core/commit/c7fae6381845cfa2e280aa23e9553d65443c8d7b)
- [`MWB-2143`](https://jira.open-xchange.com/browse/MWB-2143): Accept `harddelete` parameter to let client instantly delete a previously opened composition space [`ec80711`](https://gitlab.open-xchange.com/middleware/core/commit/ec8071161abcd6332fdc74bc00b965c3306dc5cb) [`8ad2a99`](https://gitlab.open-xchange.com/middleware/core/commit/8ad2a99855839bbe4cd00736de40eb6b9cb314df)
- [`MWB-2159`](https://jira.open-xchange.com/browse/MWB-2159): Avoid unnecessary error in case of attempting to remove an already dropped session [`a9e1914`](https://gitlab.open-xchange.com/middleware/core/commit/a9e1914bd58537c1711f2aabff8af9f48f1daea0) [`c4ef016`](https://gitlab.open-xchange.com/middleware/core/commit/c4ef016fa095b94c79dcb42e99009407c923e89b)
- [`MWB-2138`](https://jira.open-xchange.com/browse/MWB-2138): DAV file upload fails with redis session storage [`364df81`](https://gitlab.open-xchange.com/middleware/core/commit/364df81e8856f5817a6e32c26534537a0f310b18)
- [`MWB-2149`](https://jira.open-xchange.com/browse/MWB-2149): Prepare refreshing of subscriptions in a blocking manner to avoid having underlying HTTP being being recycled [`e5da60b`](https://gitlab.open-xchange.com/middleware/core/commit/e5da60b9641ed10afe0a3da162853371c7a96b58)
- [`MWB-2164`](https://jira.open-xchange.com/browse/MWB-2164): Use header for authorization instead of query string [`4634856`](https://gitlab.open-xchange.com/middleware/core/commit/463485636c576323b5ac13795ed1343c0418c584)
- [`MWB-2150`](https://jira.open-xchange.com/browse/MWB-2150): Follow up, reset parameter index before re-using [`6370ec6`](https://gitlab.open-xchange.com/middleware/core/commit/6370ec62332aac68deb48b0d6335a2bb8cad72dc)
- [`MWB-2145`](https://jira.open-xchange.com/browse/MWB-2145): NumberFormatException on partial file upload [`1feeed1`](https://gitlab.open-xchange.com/middleware/core/commit/1feeed1472eb4d8298416c766df1ba3c6d0a766d)


## [8.13.0] - 2023-05-03

### Added

- [`MW-1909`](https://jira.open-xchange.com/browse/MW-1909): iTIP Analysis and Apply actions for Resource Notification Mails
  * Scheduling mails to/from booking delegates of managed resources are sent as iMIP messages
  * Introduced additional header `X-OX-ITIP` for quick identification of such mails, obeying unique server id ([`MW-1405`](https://jira.open-xchange.com/browse/MW-1405))
  * Existing iTIP analysis and apply workflows were extended accordingly
  * Consolidated notifications and scheduling messages and their transport providers
  * Introduced property `com.openexchange.calendar.useIMipForInternalUsers` to switch to full iMIP messages for internal receivers generally ([`SCR-1191`](https://jira.open-xchange.com/browse/SCR-1191)) [`91c0491`](https://gitlab.open-xchange.com/middleware/core/commit/91c0491eaa4da26d9c3722254ccc6dd80c77fc4d)
- [`MW-1908`](https://jira.open-xchange.com/browse/MW-1908): Send Calendar Notifications to Resource Owners
  * Booking delegates now receive mails upon new, modified, deleted events with the resource
  * Organizers now receive mails upon replies for their booking requests
  * `SENT-BY` property of originator/recipient as well as mail's  `From` / `Sender` header are set appropriately [`c9b28c4`](https://gitlab.open-xchange.com/middleware/core/commit/c9b28c4dc613e43c26f934e3a29be739399bf072)
- [`MW-1405`](https://jira.open-xchange.com/browse/MW-1405): Introduced a unique server identifier [`d891c9d`](https://gitlab.open-xchange.com/middleware/core/commit/d891c9d6885ca6c8349df1aae6d94ce54c71f2ae)

### Changed

- [`MW-1913`](https://jira.open-xchange.com/browse/MW-1913): Changed mail push config to prevent multiple notifications
  * [`SCR-1158`](https://jira.open-xchange.com/browse/SCR-1158): Added toggle switches for mail push implementations, made existing properties reloadable [`6156818`](https://gitlab.open-xchange.com/middleware/core/commit/6156818b07bf668f13f75fa467bcabc0f0898203)

### Deprecated

- [`SCR-1208`](https://jira.open-xchange.com/browse/SCR-1208): Deprecation of Internal OAuth Authorization Server [`cc7d99a`](https://gitlab.open-xchange.com/middleware/core/commit/cc7d99a9634e1f402f04f418f6ef2175da5e22a6)

### Fixed

- [`MWB-2124`](https://jira.open-xchange.com/browse/MWB-2124): Change PRIMARY KEY through creation of a temporary table if the attempt to drop PRIMARY KEY is prohibited by MySQL server [`78d6f9a`](https://gitlab.open-xchange.com/middleware/core/commit/78d6f9a34a5fecf253a0339e201dd2a83ff5b063)
- **IMAP**: Allow fast `EXPUNGE` of trash folder in "fire & forget" fashion [`29c12f9`](https://gitlab.open-xchange.com/middleware/core/commit/29c12f93f1c5e68accdfacfc8a4536d01d1767fa) [`3fc0079`](https://gitlab.open-xchange.com/middleware/core/commit/3fc0079d305bc84760aa7bd9cc92207a2e2f8968)
- [`MWB-2118`](https://jira.open-xchange.com/browse/MWB-2118): No Option to prevent creation of Guest Users with Specific Email Addresses[`595c926`](https://gitlab.open-xchange.com/middleware/core/commit/595c926d19bd7c43850aebd041388ae8355d712b)
  * [SCR-1206](https://jira.open-xchange.com/browse/SCR-1206)
- [`MWB-2110`](https://jira.open-xchange.com/browse/MWB-2110): Proper imports of Netty IO packages [`e1a850d`](https://gitlab.open-xchange.com/middleware/core/commit/e1a850d38474d47664570b4f243711ca73aeb01a)
- [`MWB-2125`](https://jira.open-xchange.com/browse/MWB-2125): Do not batch-delete more than 1,000 objects from S3 storage using DeleteObjects request [`204ef8e`](https://gitlab.open-xchange.com/middleware/core/commit/204ef8e3efbab60cbe873936c6102a0d47f25db7)
  * See https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html
- [`MWB-2045`](https://jira.open-xchange.com/browse/MWB-2045): Omit specific OS version for macOS clients [`b0c9b40`](https://gitlab.open-xchange.com/middleware/core/commit/b0c9b403b77281d241d0188101c0720631de3b81)
- [`MWB-2129`](https://jira.open-xchange.com/browse/MWB-2129): Orderly surround column name with backpack characters '\`' [`bfc75b7`](https://gitlab.open-xchange.com/middleware/core/commit/bfc75b72cee35d13446c467ad14a839a5bd50fb7)
- [`MWB-2121`](https://jira.open-xchange.com/browse/MWB-2121): Properly check master authentication first for getData call [`dcca450`](https://gitlab.open-xchange.com/middleware/core/commit/dcca450cab27b112961f05fd94a94bc16cee4291)
- [`MWB-1893`](https://jira.open-xchange.com/browse/MWB-1893): Error when deleting appointment series with multiple different organizers [`a9dbced`](https://gitlab.open-xchange.com/middleware/core/commit/a9dbced73c4416501e078e58f0edc8315f8e8b76)
- [`MWB-2122`](https://jira.open-xchange.com/browse/MWB-2122): Update lastmodified timestamp when decrementing use count [`917e8a0`](https://gitlab.open-xchange.com/middleware/core/commit/917e8a002ed81cfd432d232f1f78858e2f0d7bce)
- [`MWB-2119`](https://jira.open-xchange.com/browse/MWB-2119): Optimized cleanup job & settings [`52068af`](https://gitlab.open-xchange.com/middleware/core/commit/52068af937fc091656d8ccebfb11542b1bcd7939)
- [`MWB-2128`](https://jira.open-xchange.com/browse/MWB-2128): CalDAV: Unexpected runtime exception on REPORT [`f3bda8b`](https://gitlab.open-xchange.com/middleware/core/commit/f3bda8bcfd077140dfeb29aa84f347e6a2def65d)
- [`MWB-2116`](https://jira.open-xchange.com/browse/MWB-2116): Correctly use commands for POP3 [`6b8749c`](https://gitlab.open-xchange.com/middleware/core/commit/6b8749c1693b6f9edcc8b6c2d8d40059afb86d51)
- **IMAP**: Set proper status for IMAP `AUTHENTICATE` command [`89c0766`](https://gitlab.open-xchange.com/middleware/core/commit/89c076676192592f84be89b18477ca9bcdc60492)
- [`MWB-2095`](https://jira.open-xchange.com/browse/MWB-2095): Conflicting folder "Userstore" exposed to Drive Clients [`29c3373`](https://gitlab.open-xchange.com/middleware/core/commit/29c3373fa27824efd42541062a8765ff85b5f9eb)
- [`MWB-2127`](https://jira.open-xchange.com/browse/MWB-2127): Re-adding a resource leads to a permission error [`33b804d`](https://gitlab.open-xchange.com/middleware/core/commit/33b804d56b4e5bc463f34fd2e085cd6048f05ec3)
- [`MWB-2103`](https://jira.open-xchange.com/browse/MWB-2103): Missing the verb in calendar invitation email template for it_IT [`822aafc`](https://gitlab.open-xchange.com/middleware/core/commit/822aafcc22798eb61f7a87596c0452aa5a8637d4)
- [`MWB-2120`](https://jira.open-xchange.com/browse/MWB-2120): Fixed the documented default value for `com.openexchange.oidc.hosts` [`5427c82`](https://gitlab.open-xchange.com/middleware/core/commit/5427c8270c58cacb49dfef44717703c6566e320f)
- [`MWB-2134`](https://jira.open-xchange.com/browse/MWB-2134): Don't return an unmodifiable instance of java.util.Map [`186e9b1`](https://gitlab.open-xchange.com/middleware/core/commit/186e9b1520b57c29a7e4e3b059f5419724866f2a)
- [`MWB-2030`](https://jira.open-xchange.com/browse/MWB-2030): Orderly do set session- and share-cookie when resolving share link [`a417c17`](https://gitlab.open-xchange.com/middleware/core/commit/a417c17e098b76ad8cad9a98ff9e5fb9d0cb0e7e)
- [`MWB-2090`](https://jira.open-xchange.com/browse/MWB-2090): Enhanced the documentation to warn about potentially vulnerable password change scripts [`42334a4`](https://gitlab.open-xchange.com/middleware/core/commit/42334a40aa75a3cb7076cfaee1c57a92d62db8eb)
- Removed duplicate dot in internal password change notification [`c9d1baa`](https://gitlab.open-xchange.com/middleware/core/commit/c9d1baab76eb73f17c2353f21ccc5b9a2e7dc00e)

### Security

- [`MWB-1996`](https://jira.open-xchange.com/browse/MWB-1996): CVE-2023-26455 [`0c1acb4`](https://gitlab.open-xchange.com/middleware/core/commit/0c1acb4c2ca9e98127b70cce326a5633395d6ebb) [`09781ae`](https://gitlab.open-xchange.com/middleware/core/commit/09781ae9eeb5aaf214aeb86871ad16648cf77937)

## [8.12.0] - 2023-04-03

### Added

- [`MW-1747`](https://jira.open-xchange.com/browse/MW-1747): Introduce Redis-backed sessions service [`988cb4e`](https://gitlab.open-xchange.com/middleware/core/commit/988cb4e71aa8abb88a26b76b6747c62b2bb3057f) [`c3c7177`](https://gitlab.open-xchange.com/middleware/core/commit/c3c7177f67107c347a6ed982bbca0203dfe94e7c) [`b257128`](https://gitlab.open-xchange.com/middleware/core/commit/b257128c0109c1c7c690ea10c83fe3d0d92967bc) [`434eecd`](https://gitlab.open-xchange.com/middleware/core/commit/434eecd727773219f9284eb843e79003c2a45184)
- [`MW-2029`](https://jira.open-xchange.com/browse/MW-2029): Introduced metrics for Redis session storage [`12f8ebc`](https://gitlab.open-xchange.com/middleware/core/commit/12f8ebc2c3f43e7ec89ef1ddf828b772972e3638)
- [`MW-1841`](https://jira.open-xchange.com/browse/MW-1841): Allow enforcing 'STARTTLS' for IMAP, POP3, SMTP & sieve
  * [`SCR-1120`](https://jira.open-xchange.com/browse/SCR-1120) [`c4f90a2`](https://gitlab.open-xchange.com/middleware/core/commit/c4f90a20b622d7b964d358b2a35f3903e01d8f00)
- [`MW-2029`](https://jira.open-xchange.com/browse/MW-2029): Introduced metrics for Redis session storage [`bbc8f11`](https://gitlab.open-xchange.com/middleware/core/commit/bbc8f116148c045f4ec720534b313d586d634775)
- [`MW-2023`](https://jira.open-xchange.com/browse/MW-2023): introduced possibility to block commands from apply [`59402e4`](https://gitlab.open-xchange.com/middleware/core/commit/59402e4747868ef080176a840ce283d628320511)
- [`MW-1986`](https://jira.open-xchange.com/browse/MW-1986): added login_hint and target_link_uri as parameter for oidc login [`adc2f10`](https://gitlab.open-xchange.com/middleware/core/commit/adc2f1064236d1878ce6fd6875c620ce12508e64)
- made multiple servlet oauth capable [`91b3699`](https://gitlab.open-xchange.com/middleware/core/commit/91b3699c60781d38648cc7976215c86e7627577a)
- [`MWB-2073`](https://jira.open-xchange.com/browse/MWB-2073): Introduced new property to disable adding a Sproxyd end-point to blacklist [`8617d91`](https://gitlab.open-xchange.com/middleware/core/commit/8617d91e8f946e0b17a40e9ed83f7e4e982010ff)
- [`SCR-1181`](https://jira.open-xchange.com/browse/SCR-1181): New Properties to Control 'used-for-sync" Behavior of
Calendar Folders [`94c4251`](https://gitlab.open-xchange.com/middleware/core/commit/94c4251e15bed3876e62beb5785c059e3e05e24c)
- [`MW-2002`](https://jira.open-xchange.com/browse/MW-2002): Publish Changelog at documentation.open-xchange.com [`3f0b316`](https://gitlab.open-xchange.com/middleware/core/commit/3f0b316536e7161fef52d58ddd49466614c3a866)

### Changed

- [`MW-1864`](https://jira.open-xchange.com/browse/MW-1864): lost and found tests
  * fixed, refactored or deleted several tests
  * refactored SoapUserService and linked classes
  * deleted outdated indexedSearch [`7f57ae9`](https://gitlab.open-xchange.com/middleware/core/commit/7f57ae9564307a947eb5691f07b87c40ded9f783)
- [`MW-1516`](https://jira.open-xchange.com/browse/MW-1516): Use IDBasedContactsAccess for CardDAV
  * [`SCR-1145`](https://jira.open-xchange.com/browse/SCR-1145): Refactored CardDAV to use IDBasedContactsAccess
  * [`SCR-1146`](https://jira.open-xchange.com/browse/SCR-1146): External contacts providers are now synced via CardDAV [`50a0416`](https://gitlab.open-xchange.com/middleware/core/commit/50a041618d9a88a07aa61c5c7efabba55fadf3ab)
- Refactored to have gnu.trove as a bundle in target platform [`0ebe8ff`](https://gitlab.open-xchange.com/middleware/core/commit/0ebe8ffec584dfb960673b90ee36e6e17e777d93)
- [`MW-1947`](https://jira.open-xchange.com/browse/MW-1947): Updated vulnerable libraries
  * [`SCR-1176`](https://jira.open-xchange.com/browse/SCR-1176): Updated vulnerable 3rd party libraries [`4525709`](https://gitlab.open-xchange.com/middleware/core/commit/452570967da8ed59fc8d2a977bb7df5b0ed676ef)
- [`MW-1955`](https://jira.open-xchange.com/browse/MW-1955): Hand-through possible Redis connectivity/communication errors to client during runtime & probe Redis end-point until available during start-up [`aae4f1c`](https://gitlab.open-xchange.com/middleware/core/commit/aae4f1c237e8478cdebccc0ff90181918c8f6a49)
- [`MW-1955`](https://jira.open-xchange.com/browse/MW-1955): Disable max. number of sessions by default for Redis session storage [`1b65ceb`](https://gitlab.open-xchange.com/middleware/core/commit/1b65ceb220bb0392b21e0fbad417b383cf0795f6)
- [`MW-1947`](https://jira.open-xchange.com/browse/MW-1947): Updated vulnerable libraries [`cb95cbe`](https://gitlab.open-xchange.com/middleware/core/commit/cb95cbe7ac9078609bfa2a27cc8517d8f94d3627)
- [`MWB-2059`](https://jira.open-xchange.com/browse/MWB-2059): Removed corrupt sort by UID [`d316136`](https://gitlab.open-xchange.com/middleware/core/commit/d3161366c1d66e0e97659097db2533db00b222c3)
- [`MWB-2059`](https://jira.open-xchange.com/browse/MWB-2059): Fast sorting by IMAP UID in case sort by received date (INTERNALDATE) is requested [`776449b`](https://gitlab.open-xchange.com/middleware/core/commit/776449bb123cdb5cf6e6bc6b0e0e709e665cc97f)
  * Moved JCTools as bundle to traget platform & updated it from
v3.3.0 to v4.0.1 [`06f7328`](https://gitlab.open-xchange.com/middleware/core/commit/06f7328003c15ab2d293c95686c107204fff5ee6)
  * Refactored to have gnu.trove as a bundle in target platform [`e6bf595`](https://gitlab.open-xchange.com/middleware/core/commit/e6bf59500de3e8e6d770ac69ad7d7b86c9916eb2)

### Fixed

- [`MWB-1982`](https://jira.open-xchange.com/browse/MWB-1982): Timeouts for external content do not cancel the connection
  * The fix allows to interrupt client connects and InputStream reads by having hardConnectTimeout and hardReadTimeout parameters that are used for external connections
  * Defaults to 0 (disabled)
  * The following services have a defined default of 120000 for 'hardReadTimeout' and 30000 for 'hardConnectTimeout': autoconfig-server, davsub, icalfeed, rssfeed, snippetimg, vcardphoto [`63b60eb`](https://gitlab.open-xchange.com/middleware/core/commit/63b60ebdfce79e87393d5b3f45225b4bae575d15)
- [`MWB-2040`](https://jira.open-xchange.com/browse/MWB-2040): Concurrency issue when moving a touched session to first session container. The moved session might not be "visible" for a short time. [`52069a4`](https://gitlab.open-xchange.com/middleware/core/commit/52069a4b2eec77846cce2f5034c5235dfbe497ae)
- [`MWB-2061`](https://jira.open-xchange.com/browse/MWB-2061): Organizer URI not preserved when creating Appointment [`7b3e574`](https://gitlab.open-xchange.com/middleware/core/commit/7b3e574f4d7b73df2fc59025bca9f5a38ba53706)
- [`MWB-2094`](https://jira.open-xchange.com/browse/MWB-2094): Yield a modifiable list instance from messages to copy [`3aacd7a`](https://gitlab.open-xchange.com/middleware/core/commit/3aacd7a8677f4f89c9c150f08f0ab483e34afe5e)
- [`MWB-2056`](https://jira.open-xchange.com/browse/MWB-2056): Include all overridden instances in scheduling object resource [`3bd7550`](https://gitlab.open-xchange.com/middleware/core/commit/3bd7550c456633716bd86e01f88d5653202612e8)
- [`MWB-1975`](https://jira.open-xchange.com/browse/MWB-1975): start report generation in parallel to task generation [`72047d7`](https://gitlab.open-xchange.com/middleware/core/commit/72047d781bf5cb00960cdef89633b9f1833ac7a4)
- [`MWB-2101`](https://jira.open-xchange.com/browse/MWB-2101): Unnecessary Data Retrieved from Filestore when Serving [`d262bd1`](https://gitlab.open-xchange.com/middleware/core/commit/d262bd1409c25b18f551ffbd18a0a531a25a3d3e)
- [`MWB-2081`](https://jira.open-xchange.com/browse/MWB-2081): Check table existence prior to deletion attempt (and recognize if developer accidentally passed the cause as last argument) [`2372064`](https://gitlab.open-xchange.com/middleware/core/commit/2372064cd2e1e4673fa32e1f6a155170d4291a12)
- [`MWB-2054`](https://jira.open-xchange.com/browse/MWB-2054): Auto-delete guests when owner of per-user filestore is
deleted ([`SCR-1193`](https://jira.open-xchange.com/browse/SCR-1193)) [`a296656`](https://gitlab.open-xchange.com/middleware/core/commit/a296656a6eefd64d75f8664366b04b8ca2b5878c)
- [`MWB-1985`](https://jira.open-xchange.com/browse/MWB-1985): delete all tasks in folders owned by deleted user [`5f26d66`](https://gitlab.open-xchange.com/middleware/core/commit/5f26d66d376b6a418e260345fae5c025ec86bc25)
- [`MWB-2055`](https://jira.open-xchange.com/browse/MWB-2055): Skip unrelated events when iterating events needing [`98b8140`](https://gitlab.open-xchange.com/middleware/core/commit/98b814060d4d4902b79b851db32350ae1b68bf9b)
- [`MWB-2086`](https://jira.open-xchange.com/browse/MWB-2086): Potentially malicious SQL injection when using full-text autocomplete [`408fcda`](https://gitlab.open-xchange.com/middleware/core/commit/408fcdab35207ec94cda3dfb487f4263342ed550)
- [`MWB-2022`](https://jira.open-xchange.com/browse/MWB-2022): Generate a generic error response providing SMTP server
response information in case an SMTP error code occurs while attempting
to send a message [`0d43966`](https://gitlab.open-xchange.com/middleware/core/commit/0d43966b16c703fce5e46f5a07bfdcad98e953ed)
- [`MWB-2091`](https://jira.open-xchange.com/browse/MWB-2091): Mark each messages of a multiple mail forward as forwarded [`2cde555`](https://gitlab.open-xchange.com/middleware/core/commit/2cde5558809fd888f83002b801aadd1398e4dfd4)
- [`MWB-2089`](https://jira.open-xchange.com/browse/MWB-2089): Quite old 3rd party library uses weakly accessible
sun.nio.ch package. User newer library making use of up-to-date JRE
tools instead. [`4ff5296`](https://gitlab.open-xchange.com/middleware/core/commit/4ff52960e29cbf40eec8bf82dcfc83f38f4101ac)
- Fixed reading alias from settings [`840d937`](https://gitlab.open-xchange.com/middleware/core/commit/840d937d2049d75cae81a7ba1d28187e730b8a73)
- [`MWB-2080`](https://jira.open-xchange.com/browse/MWB-2080): Added details about 'baseDN' setting in LDAP client
configuration [`7668409`](https://gitlab.open-xchange.com/middleware/core/commit/76684090784085fb5cdc3940d014eeafd15aaca7)
- [`MWB-2058`](https://jira.open-xchange.com/browse/MWB-2058): Populate 'uuid' column when registering a new server as [`692222c`](https://gitlab.open-xchange.com/middleware/core/commit/692222cf52a430e936cf304eb58351fa92d30d92)
- [`MWB-1982`](https://jira.open-xchange.com/browse/MWB-1982): Timeouts for external content do not cancel the connection [`75086ca`](https://gitlab.open-xchange.com/middleware/core/commit/75086caa323f1d5c9c786f9644a171d0ef76dde3)

### Security

- [`MWB-2090`](https://jira.open-xchange.com/browse/MWB-2090): CVE-2023-26444 [`4a2b089`](https://gitlab.open-xchange.com/middleware/core/commit/4a2b089b0bcee7946dee79aa6fa5a47ee2389ce2)
- [`MWB-2102`](https://jira.open-xchange.com/browse/MWB-2102): CVE-2023-26451 [`f903cec`](https://gitlab.open-xchange.com/middleware/core/commit/f903cec7518145460a47886fd8e80a1d140e955b)


## [8.11.0] - 2023-03-08

### Added

- Generic watcher for input stream read processes [`85699c6`](https://gitlab.open-xchange.com/middleware/core/commit/85699c6e03f74feae952a5db51faec96da326e34) [`fd49709`](https://gitlab.open-xchange.com/middleware/core/commit/fd4970965a06adef921569e3e12aad3b410a3447) [`b8dcbad`](https://gitlab.open-xchange.com/middleware/core/commit/b8dcbadffa645332a12ca993e5029060585751da) [`129749c`](https://gitlab.open-xchange.com/middleware/core/commit/129749c5e942855601c116d15bfcb3a25dd7445e)
- Added possibility to filter mail drive files [`651999c`](https://gitlab.open-xchange.com/middleware/core/commit/651999caacae4c9cbe58bd256e0b0af8cd8f89f4)
- [`MWB-1959`](https://jira.open-xchange.com/browse/MWB-1959): added possibility to filter http api metric labels [`a75d3e0`](https://gitlab.open-xchange.com/middleware/core/commit/a75d3e0d48b3326afd8982aad1f6fcba9c255721)
- Support hard timeout for processor tasks [`8f1b1b9`](https://gitlab.open-xchange.com/middleware/core/commit/8f1b1b9f10b0c7284512e1912cf559de24aee8f2)
- [`SCR-1190`](https://jira.open-xchange.com/browse/SCR-1190): Added property accepting to define a timeout in
milliseconds when reading responses from IMAP server after a command has
been issued [`e2ef0ef`](https://gitlab.open-xchange.com/middleware/core/commit/e2ef0ef691a0808526fae274daab6337a4f62826) [`023c13c`](https://gitlab.open-xchange.com/middleware/core/commit/023c13cbff2d87828dd9e96f04129df94b0818c9) [`6e81751`](https://gitlab.open-xchange.com/middleware/core/commit/6e817513116da4bed86267603ac48dcace70805e)
- Add missing packages to cloud-plugins helm definition [`935005a`](https://gitlab.open-xchange.com/middleware/core/commit/935005a3b323739b15c3fd660229ea38a7c7ed4b)

### Changed

- Updated shipped VTIMEZONE resources [`4fd83de`](https://gitlab.open-xchange.com/middleware/core/commit/4fd83de896e5df3995ff60c8b39ecbff7e45a412)
- [`MWB-2049`](https://jira.open-xchange.com/browse/MWB-2049): Ensure no wrong push match has been determined for a certain push notification [`307d766`](https://gitlab.open-xchange.com/middleware/core/commit/307d76606bfdd22a5e08fa8e91d7fb4e7122ece3) [`f314ec7`](https://gitlab.open-xchange.com/middleware/core/commit/f314ec7aceb1e83d8a972646a7efb48284010dff) [`ad17da7`](https://gitlab.open-xchange.com/middleware/core/commit/ad17da73d6cdc3585027ea28a9bddbb59aa53851) [`cfc57a8`](https://gitlab.open-xchange.com/middleware/core/commit/cfc57a89050e95d49d0152e83b69c358614f3c52) [`9564229`](https://gitlab.open-xchange.com/middleware/core/commit/956422992a2db193ec7f9d6348c3f3309bf656fb) [`5dadcfb`](https://gitlab.open-xchange.com/middleware/core/commit/5dadcfbc1b4ebb617a66b4b43601bd19d3cee755) [`508879f`](https://gitlab.open-xchange.com/middleware/core/commit/508879f96e6b33726a79425b069c76470baf248c) [`70efa61`](https://gitlab.open-xchange.com/middleware/core/commit/70efa6158b1c6a461f88915f8d9007dce5203019)
- [`MWB-2063`](https://jira.open-xchange.com/browse/MWB-2063): Lenient parsing for DTSTAMP property [`6401516`](https://gitlab.open-xchange.com/middleware/core/commit/64015161c9341fc9888b4a2fca4e5dc9101d82a1)
- [`MWB-2039`](https://jira.open-xchange.com/browse/MWB-2039): Improved concurrency when loading time zone information [`2ac192a`](https://gitlab.open-xchange.com/middleware/core/commit/2ac192a4118e10883f6247eb4eada3fc69fc3a31)
- [`MWB-2059`](https://jira.open-xchange.com/browse/MWB-2059): Let /mail?action=all end-point support "allow_enqueue=true" parameter [`70cf31d`](https://gitlab.open-xchange.com/middleware/core/commit/70cf31d9960f0d003cd4c347055eefc69219bde3) [`273c592`](https://gitlab.open-xchange.com/middleware/core/commit/273c5920456cff60647d6d55614ea562ab1b9b81)
[`c7b656f`](https://gitlab.open-xchange.com/middleware/core/commit/c7b656f2ee97168a15bf1c1e59224b269a9c1512)
- [`MWB-2040`](https://jira.open-xchange.com/browse/MWB-2040): Added some logging and introduced a session-list mutator lock [`c625aef`](https://gitlab.open-xchange.com/middleware/core/commit/c625aef6beab186caf909214a816fbc696c00e51) [`702e171`](https://gitlab.open-xchange.com/middleware/core/commit/702e171b7d77c4417c1669239e91cb676d8acf6a) [`845d03c`](https://gitlab.open-xchange.com/middleware/core/commit/845d03c89a8477ab63ab20d9173a82d8b7b0ad83) [`e6938e0`](https://gitlab.open-xchange.com/middleware/core/commit/e6938e01443b4c21af98433484eeb9fb7aef7f79)
- [`MW-1964`](https://jira.open-xchange.com/browse/MW-1964): optimizations referring to spectral findings [`a9ba5ed`](https://gitlab.open-xchange.com/middleware/core/commit/a9ba5eddd23ec788b37fc91917096415da53bb3e)
- [`MWB-1845`](https://jira.open-xchange.com/browse/MWB-1845): Ensure a reasonable size for buffers, which will be
allocated for writing data to a connection [`b47f248`](https://gitlab.open-xchange.com/middleware/core/commit/b47f2482146b77c08e9ff48984518484fed9a386) [`679df5a`](https://gitlab.open-xchange.com/middleware/core/commit/679df5ab86745d08775ec95b0536ae210decfd77)
- Use only one AtomicLong to generate request number [`8f34cbc`](https://gitlab.open-xchange.com/middleware/core/commit/8f34cbc40ac739c7e2a84f1174e133d30eac3455)
- Uses timestamp to generate a unique name for the pre-update job so the helm chart can be applied multiple times in a row if needed. Also adds a (configurable) ttl to expire the job after 24hrs. [`cfcb71a`](https://gitlab.open-xchange.com/middleware/core/commit/cfcb71a32f88330a109e27026e5c6296b425c2ad)
- [`MWB-2061`](https://jira.open-xchange.com/browse/MWB-2061): Prepare entity processor decoding for internal
organizers [`270fe7e`](https://gitlab.open-xchange.com/middleware/core/commit/270fe7e848f3b4a3eced04f40bda132dc71201cb)
- Upgraded logback-extension to 2.1.5 [`eed8bf3`](https://gitlab.open-xchange.com/middleware/core/commit/eed8bf3b9e5fd8618b844940ae272266562c3fb1)
- [`MWB-2031`](https://jira.open-xchange.com/browse/MWB-2031): Accept new property to disable black-listing of end-point for which an I/O error or HTTP protocol error was encountered [`8efbc56`](https://gitlab.open-xchange.com/middleware/core/commit/8efbc56e661b445f83c02332ec6d27e040dbe737)
- [`MWB-2039`](https://jira.open-xchange.com/browse/MWB-2039): Set missing log message argument [`d3fd63a`](https://gitlab.open-xchange.com/middleware/core/commit/d3fd63ad9b7bda5b00b3a613847de251a83e6d99)
- Assume property "logback.threadlocal.put.duplicate" is "false" by default to use concurrent MDC property map [`6d84989`](https://gitlab.open-xchange.com/middleware/core/commit/6d84989472a113ec1a4ad85c0649fc6a7087d737)

### Removed

- [`MW-1974`](https://jira.open-xchange.com/browse/MW-1974): Drop Hazelcast Upgrade Packages [`46a7063`](https://gitlab.open-xchange.com/middleware/core/commit/46a7063527acbaed2ad98c4f7a7ac240ae706527)
- [`MW-1774`](https://jira.open-xchange.com/browse/MW-1774): Removed ClusterTimeService [`940239e`](https://gitlab.open-xchange.com/middleware/core/commit/940239e0aa999d1477a01cc02f69a88142f403e9)
- [`MW-1778`](https://jira.open-xchange.com/browse/MW-1778): Disabled/deprecated the 'ramp-up' json action [`e20b7c4`](https://gitlab.open-xchange.com/middleware/core/commit/e20b7c41005dab842d0117cce5eda8c950c55ac9) [`36107b9`](https://gitlab.open-xchange.com/middleware/core/commit/36107b9d90e0090fc0f3ce94483e67a5162e0dd0)
- [`MW-1767`](https://jira.open-xchange.com/browse/MW-1767): Enqueued the drop ldap ids update task [`d839294`](https://gitlab.open-xchange.com/middleware/core/commit/d83929429646338a00e17a79414c9a6cdd698732)

### Fixed
- [`MWB-2054`](https://jira.open-xchange.com/browse/MWB-2054): Auto-delete guests when owner of per-user filestore is
deleted ([`SCR-1193`](https://jira.open-xchange.com/browse/SCR-1193)) [`eaec0e9`](https://gitlab.open-xchange.com/middleware/core/commit/eaec0e9c0c53bc5cce8deacab05827c3a0fe6ec5)
- [`MWB-2048`](https://jira.open-xchange.com/browse/MWB-2048): Limit accepted POP3 server response to reasonable length/size [`478b986`](https://gitlab.open-xchange.com/middleware/core/commit/478b9866c8e07d9f2bb8ff3ae696cf5f4ca7ffd2)
- [`MWB-1877`](https://jira.open-xchange.com/browse/MWB-1877): Avoid DNS rebinding attacks where possible (check against possible block-list on connection establishment) [`2bf40e2`](https://gitlab.open-xchange.com/middleware/core/commit/2bf40e276ca54e814793ed9134923dd6b213eefe)
- [`MWB-2038`](https://jira.open-xchange.com/browse/MWB-2038): Respect possible IPV4-mapped IPv6 addresses when
checking if contained in a block-list [`e4566e4`](https://gitlab.open-xchange.com/middleware/core/commit/e4566e4b799db30e3b0d8adc9018d07291d0939a) [`3a97e40`](https://gitlab.open-xchange.com/middleware/core/commit/3a97e40d6f6af2b2f63d8b5efb8fc34989aae730)
- [`MWB-2047`](https://jira.open-xchange.com/browse/MWB-2047): Limit accepted IMAP server response to reasonable
length/size [`9033774`](https://gitlab.open-xchange.com/middleware/core/commit/90337743c35b0fdfe5e5086a752fa800c3d84e72)
- [`MWB-2037`](https://jira.open-xchange.com/browse/MWB-2037): Drop FOREIGN KEYs from several Groupware tables [`8a5ac87`](https://gitlab.open-xchange.com/middleware/core/commit/8a5ac870928aacd47b9104bf1334eea2540cbf2d)
- [`MWB-2057`](https://jira.open-xchange.com/browse/MWB-2057): Add XCLIENT extension support for sieve [`b5e1320`](https://gitlab.open-xchange.com/middleware/core/commit/b5e1320efad1e86e947f560af0a675ed50a328b7)
- [`MWB-2046`](https://jira.open-xchange.com/browse/MWB-2046): Limit accepted SMTP server response to reasonable
length/size [`1f8c5e2`](https://gitlab.open-xchange.com/middleware/core/commit/1f8c5e2fa5d73da4e4f101b99083855a9eb7eee7)
- [`MWB-1395`](https://jira.open-xchange.com/browse/MWB-1395): Introduced limitation for number of queued image
transformation tasks [`9c17e53`](https://gitlab.open-xchange.com/middleware/core/commit/9c17e531a915877312860590fa2bd5963c94e08b)
- [`MWB-2020`](https://jira.open-xchange.com/browse/MWB-2020): only apply sanitizing to certain fields [`ac8c67c`](https://gitlab.open-xchange.com/middleware/core/commit/ac8c67c376c15cfc73eba3b012781dcfb8104524)
- [`MWB-2019`](https://jira.open-xchange.com/browse/MWB-2019): Sanitize non whitespace control character [`5e1bf5d`](https://gitlab.open-xchange.com/middleware/core/commit/5e1bf5dfbd3137174f4326f2246be32e165888e6)
- [`MWB-2025`](https://jira.open-xchange.com/browse/MWB-2025): Fixed avoidable exception on DEBUG logging [`dd4514a`](https://gitlab.open-xchange.com/middleware/core/commit/dd4514a904431b0cda4c620a5cf11d7c36acc518)
- [`MWB-1967`](https://jira.open-xchange.com/browse/MWB-1967): Don't set i18n name for public IMAP namespace if there are multiple ones configured [`d26a8a5`](https://gitlab.open-xchange.com/middleware/core/commit/d26a8a59708074d0bcd95064eee180425cccdc3c)
- [`MWB-2071`](https://jira.open-xchange.com/browse/MWB-2071): Indicate conflicting calendar object resource in
different collection via CALDAV:unique-scheduling-object-resource
precondition [`3e20448`](https://gitlab.open-xchange.com/middleware/core/commit/3e2044835b8cf48c8e6d80bb2e9131d884ee964f)
- [`MWB-2041`](https://jira.open-xchange.com/browse/MWB-2041): Fixed "file not exists" errors for single shared files [`c95b330`](https://gitlab.open-xchange.com/middleware/core/commit/c95b33022027e19211ad9cecb2132ffa85288e59)
- [`MWB-1790`](https://jira.open-xchange.com/browse/MWB-1790): Orderly complain about missing command-line arguments [`b0a4cf9`](https://gitlab.open-xchange.com/middleware/core/commit/b0a4cf9e99a7d21844a3a4d5c69c8e428d1747bb)
- [`MWB-2068`](https://jira.open-xchange.com/browse/MWB-2068): Orderly accept connect parameters when updating a mail account's attributes [`f78c307`](https://gitlab.open-xchange.com/middleware/core/commit/f78c3074c62a167b39dc2b4023d1c04fd7d59e1f)
- [`MWB-2069`](https://jira.open-xchange.com/browse/MWB-2069): Yield "unsupported" result when analyzing links
pointing to own shares [`1dbc012`](https://gitlab.open-xchange.com/middleware/core/commit/1dbc012ffc8b30631314b9536e8ab48a38161817)
- [`MWB-2030`](https://jira.open-xchange.com/browse/MWB-2030): Orderly set session- and share-cookie when resolving
share link [`212bed8`](https://gitlab.open-xchange.com/middleware/core/commit/212bed8d4719f30bd6994b962f0bb87470485f58)
- [`MWB-2044`](https://jira.open-xchange.com/browse/MWB-2044): Only update folder last-modified if permissions are
sufficient [`f14cf42`](https://gitlab.open-xchange.com/middleware/core/commit/f14cf42f334d9a0fa83996137fcea794e9ff74fe)
- [`MW-1778`](https://jira.open-xchange.com/browse/MW-1778): Added missing annotation [`7b29de7`](https://gitlab.open-xchange.com/middleware/core/commit/7b29de71fcd3af167fc5c568ef4eb1a0095634d9)

## [8.10.0] - 2023-02-08

### Added

- [`MW-1910`](https://jira.open-xchange.com/browse/MW-1910): Extended "needsAction" action to include Delegated
Resources
  * Lookup for events needing action is now also done for attendees the
user has delegated access to (resources and other users)
  * Introduced new parameter "includeDelegates" for
"chronos?action=needsAction" ([`SCR-1162`](https://jira.open-xchange.com/browse/SCR-1162))
  * Adjusted method signature of "getEventsNeedingAction" throughout
chronos stack ([`SCR-1163`](https://jira.open-xchange.com/browse/SCR-1163)) [`546c406`](https://gitlab.open-xchange.com/middleware/core/commit/546c406def1520f8685a515c327f5d1f5ddc6691)
- [`MW-1898`](https://jira.open-xchange.com/browse/MW-1898): On-behalf management for Managed Resources
  * Actions 'updateAttendee' and 'update' in module 'chronos' can now be performed on behalf of a resource attendee
  * This can be indicated by targeting the virtual resource folder id
  * Added 'own_privilege' into 'resource' model to reflect the user's scheduling privilege for a certain resource ([`SCR-1154`](https://jira.open-xchange.com/browse/SCR-1154))
  * Participation status of managed resources will now be 'NEEDS-ACTION' if confirmation is pending
  * Initial hooks for subsequent notification messages are prepared [`ca32f9c`](https://gitlab.open-xchange.com/middleware/core/commit/ca32f9c3b3c004d634fbb03fbc226e2f02622d0a)
- [`MW-1944`](https://jira.open-xchange.com/browse/MW-1944): New Action "getRecurrence" in Module "chronos"
  * Clients can now discover whether a change exception is considered as rescheduled or overridden
  * Introduced new action "getRecurrence" in Module "chronos" ([`SCR-1166`](https://jira.open-xchange.com/browse/SCR-1166))
  * Added corresponding "getRecurrenceInfo" implementation throughout Chronos stack ([`SCR-1167`](https://jira.open-xchange.com/browse/SCR-1167)) [`2ff537d`](https://gitlab.open-xchange.com/middleware/core/commit/2ff537d219164c497eb4f5d32382446246b98229)
- [`MW-1931`](https://jira.open-xchange.com/browse/MW-1931): Extended provisioning for managed resources
  * [`SCR-1161`](https://jira.open-xchange.com/browse/SCR-1161): Extended SOAP provisioning interface for managed resources [`5af1d63`](https://gitlab.open-xchange.com/middleware/core/commit/5af1d63138b4bc1b58faacbd9ec9dbeb123e3a82)
- [`MW-1969`](https://jira.open-xchange.com/browse/MW-1969): Accept "mail" as original to add attachments to a
composition space referring to file attachments of existent mails #2 [`599a83d`](https://gitlab.open-xchange.com/middleware/core/commit/599a83d2f26567c5de2a366190abae17a0c2ba16)
- [`SCR-1181`](https://jira.open-xchange.com/browse/SCR-1181): New Properties to Control 'used-for-sync" Behavior of
Calendar Folders [`821254b`](https://gitlab.open-xchange.com/middleware/core/commit/821254be26bdbb6e6ba1a7d3172853e30dca3d16)
- [`INF-80`](https://jira.open-xchange.com/browse/INF-80): Activate additional languages in default App uite 8 installations [`b186a1d`](https://gitlab.open-xchange.com/middleware/core/commit/b186a1d2d4e5c96ccf4045bce18e2dfbc9fdbbbb)
- [`MW-1969`](https://jira.open-xchange.com/browse/MW-1969): Accept "mail" as original to add attachments to a composition space referring to file attachments of existent mails [`fdbd9d6`](https://gitlab.open-xchange.com/middleware/core/commit/fdbd9d649f4bca5721f4ff5ffa18fab842b6b12d)
- [`MW-1888`](https://jira.open-xchange.com/browse/MW-1888): Upgraded Socket.IO server components to support Engine.IO v4 and Socket.IO v3 [`512d654`](https://gitlab.open-xchange.com/middleware/core/commit/512d65438ce93c9e250d47cedb47f4e0062600bb) (https://gitlab.open-xchange.com/middleware/core/commit/0cb2b2f041236ea8c90b1e5863d8bf922f14a442) [`57f4869`](https://gitlab.open-xchange.com/middleware/core/commit/57f48697a861829d108a74c8950fc446ca46f33f)

### Changed

- [`MWB-2024`](https://jira.open-xchange.com/browse/MWB-2024): Upgraded logback-extension to 2.1.4
- [`MW-1912`](https://jira.open-xchange.com/browse/MW-1912): Allow multiple Password-Change Services [`0ad74d8`](https://gitlab.open-xchange.com/middleware/core/commit/0ad74d8f6746b30bc9a242f6ad69c564a607e4b6)
- Fixed new warning since Eclipse 2022-06 "Project 'PROJECT_NAME' has no explicit encoding set" [`05797c1`](https://gitlab.open-xchange.com/middleware/core/commit/05797c1f32bce473cdc58472070753983f0d90b5)
- [`MW-1957`](https://jira.open-xchange.com/browse/MW-1957): referring to RFC5455-3.8.5.3, shift start/end date of recurrence master to the first occurrence [`1ef8fd9`](https://gitlab.open-xchange.com/middleware/core/commit/1ef8fd9e9573168c9f6e6a98b276793da81688e3)
- Don't build log message if log level does not fit #2 [`35ba26f`](https://gitlab.open-xchange.com/middleware/core/commit/35ba26fbd0f3225362745fbeb4abd7c28ca6bdc2)
- [`MWB-1970`](https://jira.open-xchange.com/browse/MWB-1970): Use active database connection when loading enhanced entity data for events [`5e20d9b`](https://gitlab.open-xchange.com/middleware/core/commit/5e20d9b0886bc7bd97ecf9c0068a5d67c3079dd4)
- [`MWB-1970`](https://jira.open-xchange.com/browse/MWB-1970): Don't advertise 'count' capability for database-backed folders [`cdc6973`](https://gitlab.open-xchange.com/middleware/core/commit/cdc6973049c95b934e1de46acf2ee07715bdca25)
- [`MWB-1970`](https://jira.open-xchange.com/browse/MWB-1970): Maintain cached list of file storage account identifiers per service [`9d8a301`](https://gitlab.open-xchange.com/middleware/core/commit/9d8a301cae43633dbb3d8bf4398e71945d773b9e)
- [`MWB-1970`](https://jira.open-xchange.com/browse/MWB-1970): Use active database connection when loading enhanced entity data for events (2) [`7efa8fc`](https://gitlab.open-xchange.com/middleware/core/commit/7efa8fcbd45210f9ebb3370d524d6c41834ecba3)
- Added special HTTP protocol exception signaling that a certain URI is denied being accessed [`0200041`](https://gitlab.open-xchange.com/middleware/core/commit/0200041fde8759db71e563c6e8fb24f0ac103d11)
- Enrich calendar results with contact details for internal organizers if requested via 'extendedEntities=true' [`e5950b7`](https://gitlab.open-xchange.com/middleware/core/commit/e5950b7fbe403d2cdcce9bad49d1550c21f8260a)
- [`MW-1830`](https://jira.open-xchange.com/browse/MW-1830): Generation of mandatory Secret Values through Helm Chart [`9dbb102`](https://gitlab.open-xchange.com/middleware/core/commit/9dbb102154e25435cd559cca8bc8d39c4945e151)
- Indicate 'optional' participants in notification mails [`e1b31f0`](https://gitlab.open-xchange.com/middleware/core/commit/e1b31f0362abbc1ec446fd692ca45718ae766715)
- Fixed logging & some thread visibility issues [`8fa7246`](https://gitlab.open-xchange.com/middleware/core/commit/8fa72465ce52d77e2c7b9ed975def31cabe8d45d)
- [`MWB-1991`](https://jira.open-xchange.com/browse/MWB-1991): upgraded micrometer from 1.5.1 to 1.10.3 [`63d112c`](https://gitlab.open-xchange.com/middleware/core/commit/63d112cf2592c86bfc325c404f1ae8a2d9eb15f5)
- [`MWB-2001`](https://jira.open-xchange.com/browse/MWB-2001): Added logging for periodic attachment storage
cleaner [`55cc090`](https://gitlab.open-xchange.com/middleware/core/commit/55cc090cf3862b43dcb85b35ad44b37ce874045c)
- Use thread-safe classes [`b606631`](https://gitlab.open-xchange.com/middleware/core/commit/b606631ee4f4cffdf259373df5f39c27d8c15bf6)
- [`MW-1985`](https://jira.open-xchange.com/browse/MW-1985): Improve DB warning/error logs [`9945242`](https://gitlab.open-xchange.com/middleware/core/commit/9945242af13d12a924ed221e5710d58eca1918c3)
- Removed unused Apache POI library from JavaMail bundle [`f42b86d`](https://gitlab.open-xchange.com/middleware/core/commit/f42b86d859c6fd40b90f8dc507ed6b8c58d4e6d2)
- Fixed some issues announced by Eclipse IDE [`e1b054b`](https://gitlab.open-xchange.com/middleware/core/commit/e1b054b92db0e2b083300fed371e8119249e0c9e)
- Improved logged error message [`9417579`](https://gitlab.open-xchange.com/middleware/core/commit/94175796789a097eb274282acf0298421a583b67)
- Removed remnants [`cb9b85d`](https://gitlab.open-xchange.com/middleware/core/commit/cb9b85dffa90ea48116680a02bb1e3edfa1d4b39)
- Resolved warnings [`9778c66`](https://gitlab.open-xchange.com/middleware/core/commit/9778c667fbce2fdd83a305b7e7d978dd20dbd07e) [`ba04ee4`](https://gitlab.open-xchange.com/middleware/core/commit/ba04ee47ad27c11853b1750c5a2b877f1f03fe1c) [`9fea797`](https://gitlab.open-xchange.com/middleware/core/commit/9fea7976d03045de9bcbadde801c700033a0303c) [`5781986`](https://gitlab.open-xchange.com/middleware/core/commit/578198607e8027a9f951ba2471e21e8af8c1ca7c) [`2dbdc9d`](https://gitlab.open-xchange.com/middleware/core/commit/2dbdc9d3cb943b61fc36a2609f94fcaf7dc0ea0e) [`06e0f60`](https://gitlab.open-xchange.com/middleware/core/commit/06e0f60defedc5e3881434c2733472ca98510d89) [`2f2a31f`](https://gitlab.open-xchange.com/middleware/core/commit/2f2a31f31b92e147c9fc7de98860ae6b054d9b86) [`5e6de37`](https://gitlab.open-xchange.com/middleware/core/commit/5e6de3707bf4fede9f025c9e56d9d4899623704f) [`d206ac0`](https://gitlab.open-xchange.com/middleware/core/commit/d206ac099e275a799a406d7bc86c4477f57b3e36) [`cf2ad17`](https://gitlab.open-xchange.com/middleware/core/commit/cf2ad170d66f4d65356208d0e5a8315b89f4832b) [`e48753a`](https://gitlab.open-xchange.com/middleware/core/commit/e48753ac81934ce4071d6bc7c1414f272204b18d)
- Don't build log message if log level does not fit #3 [`b55c826`](https://gitlab.open-xchange.com/middleware/core/commit/b55c826d20437c21acf1ef65d60e43dabb24aee7)

### Removed

- [`MW-1946`](https://jira.open-xchange.com/browse/MW-1946) - removed org.apache.tika (and com.openexchange.textxtraction). The required functionality is now provided through the new bundle com.openexchange.tika.util [`f7076fa`](https://gitlab.open-xchange.com/middleware/core/commit/f7076fa15c23ab62755d24b828abc089547f07bb)
- [`MW-1930`](https://jira.open-xchange.com/browse/MW-1930): Removed direct links from notification mail [`a2e29a9`](https://gitlab.open-xchange.com/middleware/core/commit/a2e29a9bc964c065ac90595f5b0e3c097a2aa445)
- Removed obsolete test [`3733b38`](https://gitlab.open-xchange.com/middleware/core/commit/3733b3830a46d7b989743034ff7ffbaf644fe7f9)

### Fixed

- [`MWB-1983`](https://jira.open-xchange.com/browse/MWB-1983): Limit line length and header count when fetching HTTP headers of an HTTP message + Replaced usage of `java.net.HttpURLConnection` with Apache HttpClient where necessary [`1d12911`](https://gitlab.open-xchange.com/middleware/core/commit/1d129118a4acce147aa72ba75af13cf37bd5a58a)
- [`MWB-2026`](https://jira.open-xchange.com/browse/MWB-2026): Try to handle possible connection loss errors during mail export operation [`6ff82b6`](https://gitlab.open-xchange.com/middleware/core/commit/6ff82b66df59f91664bf9fcec7414778552cd273)
- [`MW-1840`](https://jira.open-xchange.com/browse/MW-1840)-8x-patch: Encrypt with old engine, try decrypt with new if possible [`0f8a3f3`](https://gitlab.open-xchange.com/middleware/core/commit/0f8a3f37175366a025754ccc02982edc7f8d34d2)
- [`MWB-1999`](https://jira.open-xchange.com/browse/MWB-1999): impp type other than work or home is set properly [`e3f0d3c`](https://gitlab.open-xchange.com/middleware/core/commit/e3f0d3cd7fab533b83e31e9a189f137bf9d8d145)
- [`MWB-2023`](https://jira.open-xchange.com/browse/MWB-2023): Fixes to pre-update job for installations with multiple complex roles [`c0bf897`](https://gitlab.open-xchange.com/middleware/core/commit/c0bf897ecca0e234ce7aadeebab2b6c003d30b22)
- [`MWB-2021`](https://jira.open-xchange.com/browse/MWB-2021): Return proper value for "com.openexchange.subscribe.subscriptionFlag" on folder retrieval [`0d186b1`](https://gitlab.open-xchange.com/middleware/core/commit/0d186b15510f28acfccdff5291593ec0a5903c7b)
- [`MWB-2027`](https://jira.open-xchange.com/browse/MWB-2027): Specify missing error message argument on SQL error [`beb2904`](https://gitlab.open-xchange.com/middleware/core/commit/beb29041734407d26ebe55674ddab733e37a40d8)
- [`OXUIB-2162`](https://jira.open-xchange.com/browse/OXUIB-2162): wrong translation for calendar change [`23ff72e`](https://gitlab.open-xchange.com/middleware/core/commit/23ff72e587dc2d4d5bfbfa9e7451e91ac4e1ac4b)
- [`MWB-1997`](https://jira.open-xchange.com/browse/MWB-1997): API access not fully restricted when requiring 2FA [`bd67a4e`](https://gitlab.open-xchange.com/middleware/core/commit/bd67a4e8d03c9507323eb747775fdf27d175deec)
- [`MWB-1983`](https://jira.open-xchange.com/browse/MWB-1983): Limit line length and header count when fetching HTTP headers of an HTTP message + Replaced usage of `java.net.HttpURLConnection` with Apache HttpClient where necessary #2 [`c0e345b`](https://gitlab.open-xchange.com/middleware/core/commit/c0e345b890d2a186318e5e5b37f852ba1f60b552)
- [`MWB-2005`](https://jira.open-xchange.com/browse/MWB-2005): Fixed retrieving RSS feed [`fc07069`](https://gitlab.open-xchange.com/middleware/core/commit/fc070697abe9fad0e31c428d0134d3a7b996b4ef)
- [`MWB-2028`](https://jira.open-xchange.com/browse/MWB-2028): Fixed look-up of attachments in case IMAP message has TNEF content [`5934db4`](https://gitlab.open-xchange.com/middleware/core/commit/5934db4d9a8915c0b889d4cd5d6e18c8de71f9ba)
- [`MWB-2008`](https://jira.open-xchange.com/browse/MWB-2008): Don't allow to access snippets/signatures from other users if not shared [`00957b4`](https://gitlab.open-xchange.com/middleware/core/commit/00957b43ea8a21b5c8242dd1a926da7ff7566947)
- [`MWB-1991`](https://jira.open-xchange.com/browse/MWB-1991): properly remove metrics in case pool is destroyed [`38286d9`](https://gitlab.open-xchange.com/middleware/core/commit/38286d97e08e9f1b45cf0c94dfc5e9bd5dca302e)
- [`MWB-2020`](https://jira.open-xchange.com/browse/MWB-2020): added sanitizing to filter rules + improved the sanitizing regex [`21ca22e`](https://gitlab.open-xchange.com/middleware/core/commit/21ca22e22cfac06f11a51a8f3bfbe4a38506407d)
- [`MWB-1981`](https://jira.open-xchange.com/browse/MWB-1981): properly check returned ical size [`5bea149`](https://gitlab.open-xchange.com/middleware/core/commit/5bea149a40e7cceb620c4903e48f5846315e5b88)
- [`MWB-2025`](https://jira.open-xchange.com/browse/MWB-2025): Fixed avoidable exception on DEBUG logging [`cf950d6`](https://gitlab.open-xchange.com/middleware/core/commit/cf950d6098add3fb4135bafea6fe0072ab2f3957)
- [`MWB-1939`](https://jira.open-xchange.com/browse/MWB-1939): Print exposure time as fraction if possible [`8de8cb3`](https://gitlab.open-xchange.com/middleware/core/commit/8de8cb38c16015b9b790b6a25a4abcf6c188d7d5)
- [`MWB-2006`](https://jira.open-xchange.com/browse/MWB-2006): use owc only on feature branches [`65b1aa9`](https://gitlab.open-xchange.com/middleware/core/commit/65b1aa99719307781fb9a2a2fcd7646b0a769f98)
- [`MWB-2007`](https://jira.open-xchange.com/browse/MWB-2007): Only set "domain" parameter when dropping a cookie if value is considered as valid: Not "localhost". Not an IPv4 identifier. Not an IPv6 identifier [`22f9029`](https://gitlab.open-xchange.com/middleware/core/commit/22f902917a18b095d24efb5eecfa3534cd6f4ee2)
- [`MWB-1928`](https://jira.open-xchange.com/browse/MWB-1928): Only check usage (space capacity) of destination storage when moving from user-associated file storage to context-associated one since no entity assignment takes place #2 [`f76537b`](https://gitlab.open-xchange.com/middleware/core/commit/f76537b904b66e3620697607f27686b51f098d65)
- [`MWB-2036`](https://jira.open-xchange.com/browse/MWB-2036): Do escape column names when building database
statements for context move [`89c9a1f`](https://gitlab.open-xchange.com/middleware/core/commit/89c9a1f16999773519b360d728d2debad1e3d0dd)
- [`MWB-1991`](https://jira.open-xchange.com/browse/MWB-1991): adjusted 3rdPartyLibs.properties [`0fa654a`](https://gitlab.open-xchange.com/middleware/core/commit/0fa654a7d24feabf344f215be06256b148e9ca5b)
- [`MWB-2021`](https://jira.open-xchange.com/browse/MWB-2021): Return proper value for "com.openexchange.subscribe.subscriptionFlag" on folder retrieval (2) [`a1775e7`](https://gitlab.open-xchange.com/middleware/core/commit/a1775e76daca90436c480c7c8f194fc9c5776a56)
- [`MWB-2000`](https://jira.open-xchange.com/browse/MWB-2000): Only query fields necessary to construct contact image URI [`10856cc`](https://gitlab.open-xchange.com/middleware/core/commit/10856cc074065bd6b18de15d42bd1da32aceea78)
- [`MWB-2010`](https://jira.open-xchange.com/browse/MWB-2010): Set correct compression level for data exports [`fb07ee6`](https://gitlab.open-xchange.com/middleware/core/commit/fb07ee6833956966d2ab435ae2a1a08ba84a399c)
- Fixed importing and exporting the same package [`db5cd45`](https://gitlab.open-xchange.com/middleware/core/commit/db5cd45d6e2c2853992e73ba9db83e0b8a47523f)
- [`MWB-2000`](https://jira.open-xchange.com/browse/MWB-2000): Only query fields necessary to construct contact image URI (2) [`96bfe2d`](https://gitlab.open-xchange.com/middleware/core/commit/96bfe2df2540201d234ee608eaf47244026e8d1a)


## [8.9.0] - 2023-01-10

### Added

- [`SCR-1174`](https://jira.open-xchange.com/browse/SCR-1174): New Property
'com.openexchange.resource.simplePermissionMode' [`d48c9fc`](https://gitlab.open-xchange.com/middleware/core/commit/d48c9fccdfd84cde70424003f07cdf3b587e8798)

### Changed

- Refactored context restore for better readability and maintenance [`197a237`](https://gitlab.open-xchange.com/middleware/core/commit/197a237996885c9430ac79d4f011a4afdee8cb60)
- Change for [`MWB-1962`](https://jira.open-xchange.com/browse/MWB-1962): Upgraded Hazelcast from v5.1.2 to v5.2.1 [`bfe140b`](https://gitlab.open-xchange.com/middleware/core/commit/bfe140b4ff243e86c9020f7eff2fbae40d277d8f)
- **IMAP**: Check via ID command if IMAP server appears to be a Dovecot server [`f639fa4`](https://gitlab.open-xchange.com/middleware/core/commit/f639fa410e29a7d6a8a431f48c24146c9438b110)
- Avoid unnecessary creation of byte array when outputting thumbnail content to client [`6777845`](https://gitlab.open-xchange.com/middleware/core/commit/67778458e4db8b8bbf9d1200b3554d7067b76e4f)
- Avoid unnecessary SELECT statement and use "INSERT ... ON DUPLICATE KEY UPDATE" instead [`1b47613`](https://gitlab.open-xchange.com/middleware/core/commit/1b47613e69e72a3293ec0182a53c214a4cd8a63a) [`a4f414d`](https://gitlab.open-xchange.com/middleware/core/commit/a4f414d8677a6f21e2592450a861e40905a4148c)
- Direct initialisation of "AttributeChangers" instances [`6c4bf47`](https://gitlab.open-xchange.com/middleware/core/commit/6c4bf47e6033243f1141bd9d8b154cbda82895cc)
- Use singleton w/ dedicated initialisation/dropping [`48accd9`](https://gitlab.open-xchange.com/middleware/core/commit/48accd96bada48b944492568b1a59fecf26807e6)
- Thread-safe collection [`48d858c`](https://gitlab.open-xchange.com/middleware/core/commit/48d858ceeeec8433aa23b01a976f94b56cb5d974)
- Use proper URL for HttpContext when trying 2nd time [`2984c65`](https://gitlab.open-xchange.com/middleware/core/commit/2984c6562b907bf821be359f43f4807f9fb81861)
- Use singleton w/ dedicated initialisation/dropping #2 [`edeff71`](https://gitlab.open-xchange.com/middleware/core/commit/edeff71e08b5802839218971db89c5b92d7b870e)
- Removed unnecessary variable [`749e77b`](https://gitlab.open-xchange.com/middleware/core/commit/749e77bff65f414c92e904b6dacccb66ec461495)
- bump helm chart version
  * This is for the new configurable helm chart deployment type [`0cf0eb3`](https://gitlab.open-xchange.com/middleware/core/commit/0cf0eb31a52bbbb08f4656e00fb2880fe4a5ea5c)
- Cache as immutable set [`0033fd3`](https://gitlab.open-xchange.com/middleware/core/commit/0033fd3a2ae40f043e28524f95643f16f453b3f8)

### Removed

- removed unnecessary join (to be compatible with guest users) [`d46976c`](https://gitlab.open-xchange.com/middleware/core/commit/d46976c88777626bb7d10764bb8eba3a13a36f75)

### Fixed

- fixed some variables in the translation [`26065e5`](https://gitlab.open-xchange.com/middleware/core/commit/26065e51dda07b2df00c70f802279821be5037fd)
- [`MWB-1947`](https://jira.open-xchange.com/browse/MWB-1947):
  * Introduced map for storing/managing state during authentication flow
  * Added property `com.openexchange.oidc.mail.immediateTokenRefreshOnFailedAuth` to enable/disable immediate refresh of OIDC OAuth tokens on failed authentication against mail/transport service
  * Implemented immediate refresh of OIDC OAuth tokens in case of failed authentication against mail/transport service [`276670e`](https://gitlab.open-xchange.com/middleware/core/commit/276670ea8f46c2ae9e50e0f36259afb9df61c117)
- [`MWB-1966`](https://jira.open-xchange.com/browse/MWB-1966): Use proper error code to advertise resource exceptions
to client [`0e2e389`](https://gitlab.open-xchange.com/middleware/core/commit/0e2e389ff21e8e55bd5ca78f16422f8508f1256e)
- [`MWB-1995`](https://jira.open-xchange.com/browse/MWB-1995): Check if distribution list members are accessible prior
to adding them #2 [`8beba6a`](https://gitlab.open-xchange.com/middleware/core/commit/8beba6a8f7f421db1be2137815027a8fee337133)
- [`MWB-1963`](https://jira.open-xchange.com/browse/MWB-1963): More reasonable default value of 2GB (2147483648 bytes) for `com.openexchange.servlet.maxBodySize` property, which now effectively limits file uploads (no chunked HTTP upload anymore due to omission of Apache Web Server that is replaced by Istio). Moreover, introduced new property "com.openexchange.servlet.maxFormPostSize" with default value of 2MB (2097152 bytes) to have a dedicated property to control max. size for form data sent via POST. [`bd6fe39`](https://gitlab.open-xchange.com/middleware/core/commit/bd6fe39db5eff0029eeb329cf807cbdf2028276f)
- [`MWB-1972`](https://jira.open-xchange.com/browse/MWB-1972): Correctly indicate resource type in principal resources [`1ef0a13`](https://gitlab.open-xchange.com/middleware/core/commit/1ef0a13658c230a820831e045dc768c0b447da28)
- [`MWB-1995`](https://jira.open-xchange.com/browse/MWB-1995): Check if distribution list members are accessible prior
to adding them [`153909b`](https://gitlab.open-xchange.com/middleware/core/commit/153909b2ce9a799a00657c6cb171d884ec94d09a)
- [`MWB-1936`](https://jira.open-xchange.com/browse/MWB-1936): Revisited transport checks [`8542d55`](https://gitlab.open-xchange.com/middleware/core/commit/8542d5529cbe92dda547dfdfa15cfdb035df7fd2)
- [`MW-1989`](https://jira.open-xchange.com/browse/MW-1989): Don't let delete operation fail upon malformed change
exception data while tracking changes [`3d47d7e`](https://gitlab.open-xchange.com/middleware/core/commit/3d47d7e0d36766f8185d4212c45a9823bbf3c2da)
- [`MWB-1985`](https://jira.open-xchange.com/browse/MWB-1985): properly handle public tasks folder in case no-reassign
is set [`036afcc`](https://gitlab.open-xchange.com/middleware/core/commit/036afcc75cd2152917bb0d3e62a4f5aca068e117)
- [`MWB-1984`](https://jira.open-xchange.com/browse/MWB-1984): Prefer address from EMAIL parameter when deciding if
iMIP mails from iCloud are considered as 'known' sender [`543dbcc`](https://gitlab.open-xchange.com/middleware/core/commit/543dbccb07efc206d6883c913385062313c3371b)
- Change for [`DOV-4625`](https://jira.open-xchange.com/browse/DOV-4625): Detect missing space character in case of corrupt NIL value for PREVIEW fetch item; e.g. "PREVIEW NILUID 1" [`d2ca600`](https://gitlab.open-xchange.com/middleware/core/commit/d2ca600f0e35731f2df51a51b478533cb343b436)
- [`MWB-1956`](https://jira.open-xchange.com/browse/MWB-1956): Apple Mail flag taken over even though Open-Xchange color flag has been explicitly set to NONE [`9f18684`](https://gitlab.open-xchange.com/middleware/core/commit/9f186842a0b4cb1f3fb50522f336aa9fdb3029e8)
- [`MWB-1964`](https://jira.open-xchange.com/browse/MWB-1964): Let guest inherit sharing user's filestore if
applicable [`e82657b`](https://gitlab.open-xchange.com/middleware/core/commit/e82657b5b948b31fb6ced7d7a8eb47360065a903)
- [`MWB-1961`](https://jira.open-xchange.com/browse/MWB-1961): throw proper error in case user is missing [`d682bf8`](https://gitlab.open-xchange.com/middleware/core/commit/d682bf86e32fcbc67d7f93e1973ae40cec36a3a3)
- [`MWB-1934`](https://jira.open-xchange.com/browse/MWB-1934): Don't allow empty "From" address on mail transport [`e64de8a`](https://gitlab.open-xchange.com/middleware/core/commit/e64de8afb0dd597bf25a1090bbc57e03659bd2ec)
- [`MWB-1820`](https://jira.open-xchange.com/browse/MWB-1820): only removes guests in case of real failures [`110596f`](https://gitlab.open-xchange.com/middleware/core/commit/110596f9d49a40e1a92025bb4ee4a359fc301bcf)
- [`MWB-1971`](https://jira.open-xchange.com/browse/MWB-1971): improved matching of distribution list members [`1218c53`](https://gitlab.open-xchange.com/middleware/core/commit/1218c53fb0b8d1304ca16be9e0134cf22969ff5b)
- [`MWB-1851`](https://jira.open-xchange.com/browse/MWB-1851): Return proper folder identifier when saving draft to POP3 account [`05e59fc`](https://gitlab.open-xchange.com/middleware/core/commit/05e59fc84b36edff56a7fd12679e63a665b0a6f2)
- [`MWB-1951`](https://jira.open-xchange.com/browse/MWB-1951): Use unicode address to resolve mail recipient [`7fb1c8c`](https://gitlab.open-xchange.com/middleware/core/commit/7fb1c8cc0361a06c14bc53c83883039c860bead8)
- [`MWB-1986`](https://jira.open-xchange.com/browse/MWB-1986): Fixed SQL error in SELECT statement (Mixing of GROUP columns (MIN(),MAX(),COUNT(),...) with no GROUP columns is illegal if there is no GROUP BY clause) [`91105d0`](https://gitlab.open-xchange.com/middleware/core/commit/91105d0d5b66c355d97c0f7c4feab279501ec420)
- [`MWB-1978`](https://jira.open-xchange.com/browse/MWB-1978): Prevent changes of object id when generating delta
event [`7de23e6`](https://gitlab.open-xchange.com/middleware/core/commit/7de23e6ec3b9c8a4a255b6a06d2b02ff836d4bef)

## [8.8.0] - 2022-12-14

### Added

- [`MW-1857`](https://jira.open-xchange.com/browse/MW-1857): Option to disable SMTP for 3rd party Mail Accounts [`a6d5a0b`](https://gitlab.open-xchange.com/middleware/core/commit/a6d5a0b028af89ffe202bebc801376e49fba06dd)
  * Added a new middleware property `com.openexchange.mail.smtp.allowExternal` which defaults to true
  * Utilise that property to filter the transport details in the mail account POJOs
  * Introduced a new read-only JSLob entry under `io.ox/mail//features/allowExternalSMTP` which reflects the middleware's property
  * Forbid sending mail from an external SMTP server as long as the setting is set to false
  * Forbid creating/updating mail accounts with transport information as long as the setting is set to false
  * Added a new warning for preflight/validity checks which reflect this 
- [`MW-1831`](https://jira.open-xchange.com/browse/MW-1831): Push configuration for macOS drive client [`d2a9903`](https://gitlab.open-xchange.com/middleware/core/commit/d2a9903a2e4e327383c767f5d42ac6111091deac)
  * [`SCR-1157`](https://jira.open-xchange.com/browse/SCR-1157): Introduced properties for macOS client push notification configuration
- [`SCR-1165`](https://jira.open-xchange.com/browse/SCR-1165): Added options to specify socket read timeout when applying filter to existent messages [`53f3023`](https://gitlab.open-xchange.com/middleware/core/commit/53f3023894c0e5fe0586cc9f28e7bbdf9220d8d7)
- [`MW-1938`](https://jira.open-xchange.com/browse/MW-1938): New Templates and Examples section for documentation and adapted jenkins workflow to dynamically point to the correct version of the files [`11bbcbc`](https://gitlab.open-xchange.com/middleware/core/commit/11bbcbcde5666689a583729b1437642584e475bd)

### Changed

- MAL: Enhanced [`MSG-1016`](https://jira.open-xchange.com/browse/MSG-1016) error code by actual connect timeout value [`e194eb1`](https://gitlab.open-xchange.com/middleware/core/commit/e194eb18b135072084ffcde5bc2f61008aca5656)
- Mail Auto-Config: Let auto-config attempt fail immediately in case login attempt encounters failed authentication due to wrong credentials/authentication data [`f1fea90`](https://gitlab.open-xchange.com/middleware/core/commit/f1fea90a31b3f5ffb0916812e08937b2ebbdb702) [`45b68d0`](https://gitlab.open-xchange.com/middleware/core/commit/45b68d01df8954efa69a1c3b97a77667b7a3079f)
- [`MWB-1943`](https://jira.open-xchange.com/browse/MWB-1943): Apply consistent configuration to mail auto-config as used when connecting to the account during runtime [`1d682ef`](https://gitlab.open-xchange.com/middleware/core/commit/1d682efe932e8e98efd53dfdadf9a483757d5bf7)
- Don't build log message if log level does not fit [`4b55202`](https://gitlab.open-xchange.com/middleware/core/commit/4b552021198869dd2a0700978e6c3395e938945b)
- [`MW-1941`](https://jira.open-xchange.com/browse/MW-1941): Updated and re-structured documentation [`373dce4`](https://gitlab.open-xchange.com/middleware/core/commit/373dce4b3179fc752c4ebfaeeb788a495cbc9d63)
- [`OXUIB-2066`](https://jira.open-xchange.com/browse/OXUIB-2066): Propagate configured mail fetch limit via JSlob under "io.ox/mail//mailfetchlimit" [`895d606`](https://gitlab.open-xchange.com/middleware/core/commit/895d60671419534a132ffbed75a5c03b3eda8a1b)
- Database: Utility method to re-execute DB operation on transaction roll-back error [`bb47eab`](https://gitlab.open-xchange.com/middleware/core/commit/bb47eab31c667018c4abee35db7f7d3b6da20e5f)
- [`MW-1904`](https://jira.open-xchange.com/browse/MW-1904): Adjust for Reserved Words in MariaDB 10.6 [`d713340`](https://gitlab.open-xchange.com/middleware/core/commit/d713340a42cfa810b7134d4564b69c4d0b5c7c22)
  * Using back-ticks in SQL statements to handle new reserved words in MariaDB 10.6
  * Only the keyword `OFFSET` had to be adjusted in SQL statements
- Don't build log message if log level does not fit #2 [`37dd1ad`](https://gitlab.open-xchange.com/middleware/core/commit/37dd1ad3fc89f950b4fa0b89a81b0f391f051008)
- JavaMail: Optimized creation of FetchResponse instances through remembering if RFC8970 "PREVIEW" capability is advertised by IMAP server [`cb17cd5`](https://gitlab.open-xchange.com/middleware/core/commit/cb17cd55fd7f640a3f70a210a60db8a81dbee452)
- MAL: Enhanced "[`MSG-1016`](https://jira.open-xchange.com/browse/MSG-1016)" error code by actual connect timeout value #2 [`c108082`](https://gitlab.open-xchange.com/middleware/core/commit/c108082c9159996e8429785a64be1b2a10137baf)
- [`MWB-1909`](https://jira.open-xchange.com/browse/MWB-1909): Extended information in case an error occurs [`470911d`](https://gitlab.open-xchange.com/middleware/core/commit/470911d4a4750f8f47234d5831659e796cb85069)

### Fixed

- [`MWB-1902`](https://jira.open-xchange.com/browse/MWB-1902): Use localized display name for groups towards clients [`27f0a50`](https://gitlab.open-xchange.com/middleware/core/commit/27f0a509f008c5b0f06aa892f4b32582f405c413)
- [`MWB-1857`](https://jira.open-xchange.com/browse/MWB-1857): Incomplete response when requesting /infostore?action=list [`0d4ddce`](https://gitlab.open-xchange.com/middleware/core/commit/0d4ddce361658d34c1a9cf79dff7892210241206)
- Change for [`OXUIB-2067`](https://jira.open-xchange.com/browse/OXUIB-2067): Avoid alternative MIME part look-up by Content-Id in case no such part is contained in IMAP message's BODYSTRUCTURE information [`49f3b9e`](https://gitlab.open-xchange.com/middleware/core/commit/49f3b9e0b1adb3b81be7a52032e88cb8242b5942)
- [`MWB-1944`](https://jira.open-xchange.com/browse/MWB-1944): Don't cache user-sensitive non-file-backed properties [`e7d0385`](https://gitlab.open-xchange.com/middleware/core/commit/e7d038583e07e0be6ccbea460b898c1dc3de2864)
- [`MWB-1904`](https://jira.open-xchange.com/browse/MWB-1904): Properly indicate 'DAV:need-privilege' precondition
with HTTP 403 for PUT requests w/o sufficient privileges [`65e64e6`](https://gitlab.open-xchange.com/middleware/core/commit/65e64e6975f18a046ec315e8cd189894e3106e49)
- [`MWB-1940`](https://jira.open-xchange.com/browse/MWB-1940): Only inject a valid image URI into mail body's HTML part if such an inline image seems to exist in parental mail [`d70ce12`](https://gitlab.open-xchange.com/middleware/core/commit/d70ce12d9fb3eb6f11c5fb04a0e76214ba328bdf)
- [`MWB-1887`](https://jira.open-xchange.com/browse/MWB-1887): Delete folders chunk-wise to avoid excessively big database transaction [`244847d`](https://gitlab.open-xchange.com/middleware/core/commit/244847d700a13dc46933a33e65d6db6779b323fd)
- [`MWB-1901`](https://jira.open-xchange.com/browse/MWB-1901): Disable usage of XCLIENT SMTP extension by default [`4452098`](https://gitlab.open-xchange.com/middleware/core/commit/44520987db7a93cf722d63cbd69b4135915e067c)
- [`MWB-1948`](https://jira.open-xchange.com/browse/MWB-1948): Perform alternative SASL long against SMTP server if initial response exceeds max. line length of 998 [`90b9477`](https://gitlab.open-xchange.com/middleware/core/commit/90b9477f379dd1758852505b7c47a0e9d59e4471)
- [`MWB-1899`](https://jira.open-xchange.com/browse/MWB-1899): Accept escaped wild-card characters in search pattern [`141e691`](https://gitlab.open-xchange.com/middleware/core/commit/141e691296de9387b6d33315c8aa22d26b89ff80)
- [`MWB-1912`](https://jira.open-xchange.com/browse/MWB-1912): aligned checks with documentation [`8de34a9`](https://gitlab.open-xchange.com/middleware/core/commit/8de34a9fd72dfbc5855b148076cf48183b373705)
- [`USM-36`](https://jira.open-xchange.com/browse/USM-36): Re-introduce CUD actions [`e83189b`](https://gitlab.open-xchange.com/middleware/core/commit/e83189bd1a33199103ad4c44b0264049389554a8)
- [`MWB-1928`](https://jira.open-xchange.com/browse/MWB-1928): Only check usage (space capacity) of destination storage when moving from user-associated file storage to context-associated one since no entity assignment takes place [`06f177b`](https://gitlab.open-xchange.com/middleware/core/commit/06f177b1701c096a34e555644cba7050df1e5202)
- [`MWB-1909`](https://jira.open-xchange.com/browse/MWB-1909): Handle possible NULL result value when querying counts [`a64eb82`](https://gitlab.open-xchange.com/middleware/core/commit/a64eb82add1b2cc2db107c6ec8ee8a5c040b1e31)
- [`MWB-1950`](https://jira.open-xchange.com/browse/MWB-1950): Do not check the user while resolving mail recipients in recipientOnly modus [`263a2b5`](https://gitlab.open-xchange.com/middleware/core/commit/263a2b561b0a855e53394b1c8a8a210f24be91af)
- [`MWB-1929`](https://jira.open-xchange.com/browse/MWB-1929): Remove sessions from remote nodes during backchannel
logout synchronously [`82d4253`](https://gitlab.open-xchange.com/middleware/core/commit/82d42539b8789d9f93caf0d3d72c5964e98564e6)
- Fix connection leak in test clients [`a415e8e`](https://gitlab.open-xchange.com/middleware/core/commit/a415e8e40d53b2797be28e312c6556b5a9558bf3)
- [`MWB-1931`](https://jira.open-xchange.com/browse/MWB-1931): Don't allow empty passwords [`d506a00`](https://gitlab.open-xchange.com/middleware/core/commit/d506a009efa4d750527c7794b6eb1eb5fc6b2753)
- [`MWB-1944`](https://jira.open-xchange.com/browse/MWB-1944): Don't cache user-sensitive non-file-backed properties [`eb74ebf`](https://gitlab.open-xchange.com/middleware/core/commit/eb74ebfc8abf34aed63b7a2803b00b3bc423c089)
- [`MWB-1887`](https://jira.open-xchange.com/browse/MWB-1887): Don't forget to finish Infostore instance [`f1d4fc4`](https://gitlab.open-xchange.com/middleware/core/commit/f1d4fc47cb0c01feb530f7ec7895c6a093c9e300)
- [`MWB-1923`](https://jira.open-xchange.com/browse/MWB-1923): Avoid premature closing of attachments [`a9a5174`](https://gitlab.open-xchange.com/middleware/core/commit/a9a5174b4d7eccefbc57ac2a2ef7027579dafa68)
- Use proper fall-back for "com.openexchange.imap.folderCacheTimeoutMillis" setting [`87d9b67`](https://gitlab.open-xchange.com/middleware/core/commit/87d9b676fa0bc791d6f081846c962b7589219cc2)
- [`MWB-1941`](https://jira.open-xchange.com/browse/MWB-1941): Deleteuser fails with invalid CU [`035a397`](https://gitlab.open-xchange.com/middleware/core/commit/035a397b98940c0f8ded26612eac68f0963ac5a8)
- [`MWB-1949`](https://jira.open-xchange.com/browse/MWB-1949): fixed wrong option within the documentation of the command line tool [`357d263`](https://gitlab.open-xchange.com/middleware/core/commit/357d26344a5edbeb6bc0e7c99cb5563f4cf17c42)
- [`GUARD-391`](https://jira.open-xchange.com/browse/GUARD-391): Split lines only on newline during normalization [`8873cfd`](https://gitlab.open-xchange.com/middleware/core/commit/8873cfdc890c53755c3a1a291024f76c8d9ba5e5)

### Security

- [`OXUIB-2034`](https://jira.open-xchange.com/browse/OXUIB-2034): Deny setting certain jslob core subtrees [`a603fa8`](https://gitlab.open-xchange.com/middleware/core/commit/a603fa8f9a12e4dbe8d5a85cd2f82ad57430822e) [`929b9a`](https://gitlab.open-xchange.com/middleware/core/commit/929b9a657866a180627b60d0107b764a7234e266)
  * See also [`MWB-1784`](https://jira.open-xchange.com/browse/MWB-1784)

## [8.7.0-8.7.19] - 2022-11-11

### Added

- [`MW-1877`](https://jira.open-xchange.com/browse/MW-1877): Permissions for Resources
  * Introduced resource scheduling privileges 'ask_to_book', 'book_directly' and 'delegate'
  * By default, group 0 has 'book_directly' privileges for each resource("unmanaged mode"), unless defined differently ("managed mode")
  * Extended resource model by a corresponding permissions array, storing privileges per entity
  * HTTP API is adjusted accordingly  ([`SCR-1154`](https://jira.open-xchange.com/browse/SCR-1154))
  * New database table resource_permissions to store resource privileges of users/groups ([`SCR-1153`](https://jira.open-xchange.com/browse/SCR-1153)) [`4de788f`](https://gitlab.open-xchange.com/middleware/core/commit/4de788f37cf9e58343019570abd359efb46050f0)
- [`MWB-1871`](https://jira.open-xchange.com/browse/MWB-1871): added possibility to parse images of nested messages
  * Added new lean property com.openexchange.mail.handler.image.parseNested with defaults to true [`b42dfec`](https://gitlab.open-xchange.com/middleware/core/commit/b42dfecd46380c8a9cf4d8a25443c867918adae3)
- [`MW-1903`](https://jira.open-xchange.com/browse/MW-1903): introduced CORE_TEST param to Jenkinsfile [`6a4a0ba`](https://gitlab.open-xchange.com/middleware/core/commit/6a4a0ba8b72d04adbb1d74488754e04894835af1)
- [`MW-1507`](https://jira.open-xchange.com/browse/MW-1507): Calendars for Resources
  * Introduced virtual folder identifiers for resource calendars ([`SCR-1149`](https://jira.open-xchange.com/browse/SCR-1149))
  * Folder ids can be used in typical "chronos?action=all" requests to get the contained events, actions "advancedSearch", "get" and "list" are supported as well
  * Events returned under the perspective of a virtual resource folder will also have this virtual identifier assigned within the folder field
  * The requesting user will either get all details of an event in a resource folder, or only an anonymized version - depending on whether the event is visible for the user in another folder view or not. [`6fbc61a`](https://gitlab.open-xchange.com/middleware/core/commit/6fbc61a3b999d146bd6ecb75825aee392cfce527)
- [`MW-1792`](https://jira.open-xchange.com/browse/MW-1792): Allow changing of "includeSubfolders" flag through link permission entity [`e326340`](https://gitlab.open-xchange.com/middleware/core/commit/e3263404b8b2659143dfebc546eeb131ee3cbb82)

### Changed

- Minor changes for mail auto-config [`8221066`](https://gitlab.open-xchange.com/middleware/core/commit/822106647db674e48dd6fbc3515448eacadfa235)
- [`MWB-1901`](https://jira.open-xchange.com/browse/MWB-1901): Do not issue XCLIENT command if no XCLIENT parameter is supported [`c915650`](https://gitlab.open-xchange.com/middleware/core/commit/c9156501df1c64e328bd82fc08f4be708857d778)
- [`MWB-666`](https://jira.open-xchange.com/browse/MWB-666): Send "431 - Request Header Fields Too Large" HTTP error response instead of "400 - Bad Request" when HTTP packet header is too large [`a7cc43c`](https://gitlab.open-xchange.com/middleware/core/commit/a7cc43c6a3b3ea9ea8aed22970465c8d70c34ed0)
- JavaMail: Check appropriate capability "SEARCH=X-MIMEPART" prior to performing a file name search [`3cc2ce8`](https://gitlab.open-xchange.com/middleware/core/commit/3cc2ce80a2cccb387d3b9ed65d06d384c62647cf)
- [`OXUIB-2025`](https://jira.open-xchange.com/browse/OXUIB-2025): Added support for TEXT search term to filter messages that contain a specified string in the header or body of the message [`f775905`](https://gitlab.open-xchange.com/middleware/core/commit/f77590578c66e1bb6965d9b207ae8f9a3d600260)
- [`OXUIB-2025`](https://jira.open-xchange.com/browse/OXUIB-2025): Added support for TEXT search term to filter messages that contain a specified string in the header or body of the message #2 [`910eb69`](https://gitlab.open-xchange.com/middleware/core/commit/910eb692d311c2912970642ce73e7f5e1cbadf74)
- [`MW-1915`](https://jira.open-xchange.com/browse/MW-1915): Migrated helm lint/publish and docu build/publish to jenkins [`391bc2b`](https://gitlab.open-xchange.com/middleware/core/commit/391bc2bc2d50a4c127792da2402fb776c68deaf0)
- [`MW-1813`](https://jira.open-xchange.com/browse/MW-1813): New approach for centralized version information [`cf6d801`](https://gitlab.open-xchange.com/middleware/core/commit/cf6d80108b45caad7320190cfb1565d1ee41ee53)
- [`MWB-1826`](https://jira.open-xchange.com/browse/MWB-1826): Added some logging [`49c0b33`](https://gitlab.open-xchange.com/middleware/core/commit/49c0b33375e08c18ef73b7a159dd1b630077bb81)
- [`MWB-1891`](https://jira.open-xchange.com/browse/MWB-1891): Don't validate distribution list member's mail address during user copy [`e3c0f22`](https://gitlab.open-xchange.com/middleware/core/commit/e3c0f2282b16dddebd7c74ecb3dfa6d828f551fe)
- [`MW-1914`](https://jira.open-xchange.com/browse/MW-1914): Extend Webhook integration for Jitsi Conferences
  * Renamed Switchboard Packages and Bundles ([`SCR-1151`](https://jira.open-xchange.com/browse/SCR-1151))
  * Adjusted Switchboard Configuration ([`SCR-1152`](https://jira.open-xchange.com/browse/SCR-1152))
  * Implemented new interceptor for conferences of type "jitsi"
  * Transformed switchboard calendar handler into a handler for a generic webhook target [`0593a47`](https://gitlab.open-xchange.com/middleware/core/commit/0593a47cff6496008ed9d831184622caa4120a78)
- [`INF-30`](https://jira.open-xchange.com/browse/INF-30): Use globally configured appRoot [`16853d6`](https://gitlab.open-xchange.com/middleware/core/commit/16853d600e3a3b3df274b58ea9b6b834b5b186d6)

### Removed

- Removed c.o.dav.push leftovers [`4369c69`](https://gitlab.open-xchange.com/middleware/core/commit/4369c69f5950223b93ba0e341c0afbe9afee3bfa)
- Removed c.o.mail.authenticity leftovers [`c753f59`](https://gitlab.open-xchange.com/middleware/core/commit/c753f597092c49242ca85349e1fbc1e417c95ce5)
- Removed c.o.oauth.linkedin leftovers [`638988b`](https://gitlab.open-xchange.com/middleware/core/commit/638988b333feb74be16ecad11288604ba8326e75)
- Removed c.o.halo.linkedin leftovers [`121f054`](https://gitlab.open-xchange.com/middleware/core/commit/121f054665f2be5590b4c82023c54c9f5c206b4e)
- Removed c.o.subscribe.linkedin leftovers [`01d80d1`](https://gitlab.open-xchange.com/middleware/core/commit/01d80d159cbca0c77a936e965fb092dc7159dda2)
- Removed c.o.mail.authentication leftover [`2a846b0`](https://gitlab.open-xchange.com/middleware/core/commit/2a846b0361deb32f215b850ae3bf8f5cd33c1388)
- Removed no more required folder [`d57ee8c`](https://gitlab.open-xchange.com/middleware/core/commit/d57ee8c5474497e9a18bf9951284d69347699c13)
- Removed no more required folder [`1a482ee`](https://gitlab.open-xchange.com/middleware/core/commit/1a482ee55ba80b13c08d0ff6a5d7a2cfd2d13ee7)
- Removed obsolete o-x-test-bundles [`dd513de`](https://gitlab.open-xchange.com/middleware/core/commit/dd513deff4b9607843bace66b640ffae159c0db3)
- Removed c.o.printing leftovers [`a2f7b3e`](https://gitlab.open-xchange.com/middleware/core/commit/a2f7b3e6ce0131a3930f7be4bcb746d94411c19d)
- Removed no more required folder [`5ee810f`](https://gitlab.open-xchange.com/middleware/core/commit/5ee810f18a1f4ffff77e49630c638b1551ee0b8d)
- Removed redundant/obsolete folder implementations [`102032c`](https://gitlab.open-xchange.com/middleware/core/commit/102032cc6b326494adac2d248af553c98caa5320)

### Fixed

- [`MWB-1907`](https://jira.open-xchange.com/browse/MWB-1907): Restored previous SOAP behaviour by accepting individual parameters instead of a wrapping parameter object [`d1c2de4`](https://gitlab.open-xchange.com/middleware/core/commit/d1c2de445da68893eb916faf00eebe6d2141d77c)
- [`MWB-1876`](https://jira.open-xchange.com/browse/MWB-1876): Check redirect location against blacklisted hosts when creating an iCal subscription. [`e219389`](https://gitlab.open-xchange.com/middleware/core/commit/e219389eda92cff087e1b17e243f8058d0591df1)
- [`MWB-1911`](https://jira.open-xchange.com/browse/MWB-1911): Do not require deputy service in case user replies to a message residing in a shared mail folder [`4377dff`](https://gitlab.open-xchange.com/middleware/core/commit/4377dffe52784e90db43f5e6ca6f573acee3c0e0)
- JavaMail: Add the ability to the API consumers to load the API implementations by using a different protection domain when the API is used with security manager enabled [`12f4647`](https://gitlab.open-xchange.com/middleware/core/commit/12f46472a650ed4797a0b393659c02eba5011cd1)
- JavaMail: Implement equals() and hashcode() on jakarta.mail.Header (#597) [`8294cf2`](https://gitlab.open-xchange.com/middleware/core/commit/8294cf2d307a4551c8f67e18664717d637bc4b4d)
- [`MWB-1908`](https://jira.open-xchange.com/browse/MWB-1908): Keep remembering OIDC -> OX session id mapping in state after auto-login [`c11a94d`](https://gitlab.open-xchange.com/middleware/core/commit/c11a94de3d4cf081d5374679cce52ac11c2837d2)
- JavaMail: j.m.u.FactoryFinder.factoryFromServiceLoader needs PrivilegedAction #621 (#622) [`83d9c14`](https://gitlab.open-xchange.com/middleware/core/commit/83d9c148692ecafe8ca5f4e79768f8eff950d936)
- [`MWB-1909`](https://jira.open-xchange.com/browse/MWB-1909): Adjusted queries issued by datamining tool to obey MySQL's ONLY_FULL_GROUP_BY mode [`a4e293e`](https://gitlab.open-xchange.com/middleware/core/commit/a4e293ec9bac10c536161823bd768b1025957e82)
- JavaMail: Fix630 2 (#633) [`75b7136`](https://gitlab.open-xchange.com/middleware/core/commit/75b71363826a9e0994d254b1c02600c3ceb29939)
- [`MWB-1893`](https://jira.open-xchange.com/browse/MWB-1893): Don't let delete operation fail upon malformed change exception data while tracking changes [`78615b9`](https://gitlab.open-xchange.com/middleware/core/commit/78615b93a48961f692a04bd019e0b1aea72f7371)
- [`MWB-1887`](https://jira.open-xchange.com/browse/MWB-1887): Fire events with a separate thread avoiding unnecessary occupation of deletion-performing main thread [`0cbd10c`](https://gitlab.open-xchange.com/middleware/core/commit/0cbd10c2a993c129f54593c9b8005f8cb5912412)
- [`MWB-1887`](https://jira.open-xchange.com/browse/MWB-1887): Allow /folders?action=clear being performed as enqueuable operation [`cc226a7`](https://gitlab.open-xchange.com/middleware/core/commit/cc226a763d2a7f61fbf961ecb1857ec03f99f6b5)
- [`MWB-1898`](https://jira.open-xchange.com/browse/MWB-1898): Added documentation examples for mapping context-/user-id properties to LDAP attributes properly [`3be7f84`](https://gitlab.open-xchange.com/middleware/core/commit/3be7f84eb890564a882179a9ee66ca196dc955b4)
- [`MW-1813`](https://jira.open-xchange.com/browse/MW-1813): bug fixed by which the version was not resolved correctly [`aa0d040`](https://gitlab.open-xchange.com/middleware/core/commit/aa0d040921afb83f0e156cbecdf1d0284ec4233c)
- [`MWB-1889`](https://jira.open-xchange.com/browse/MWB-1889): Drive mail with expiry date / with password can not be send [`7b462f4`](https://gitlab.open-xchange.com/middleware/core/commit/7b462f4dfa3a0c1390cf0939c5194b0b44e04370)
- [`MWB-1892`](https://jira.open-xchange.com/browse/MWB-1892): Don't filter "com.openexchange.grizzly.serverName" property from log event [`4d342b8`](https://gitlab.open-xchange.com/middleware/core/commit/4d342b833ee278c9ab659cb4bf3c9350c28c3825)
- [`MWB-1878`](https://jira.open-xchange.com/browse/MWB-1878): Handle empty Disposition-Notification-To header on delete [`cf06c47`](https://gitlab.open-xchange.com/middleware/core/commit/cf06c478bdbf5c38081e533ede3f87ae0e8a793f)
- [`MWB-1882`](https://jira.open-xchange.com/browse/MWB-1882): Upgraded Apache Commons Text from v1.9 to v1.10.0 [`7a911be`](https://gitlab.open-xchange.com/middleware/core/commit/7a911be6102d4e3ced03557ec93b8340b1092d4d)
- [`MWB-1890`](https://jira.open-xchange.com/browse/MWB-1890): Do obey folder types restriction when constructing search term for looking up events of user [`87ec00e`](https://gitlab.open-xchange.com/middleware/core/commit/87ec00edd7a2b2a9289edc464155b3d4d7692c7e)
- [`MWB-1874`](https://jira.open-xchange.com/browse/MWB-1874): Remove references to contact in distribution list member when contact's email is cleared [`db7ef9e`](https://gitlab.open-xchange.com/middleware/core/commit/db7ef9e2d6c3f202dc824c55de73db5dd66cb5fc)
- [`MWB-1695`](https://jira.open-xchange.com/browse/MWB-1695): Introduced "requiredCapabilities" for App-specific Password Applications [`SCR-1155`](https://jira.open-xchange.com/browse/SCR-1155) [`ec439e9`](https://gitlab.open-xchange.com/middleware/core/commit/ec439e91b2d5dad46a63f1d375553fa0c0ee357b)
- [`MWB-1865`](https://jira.open-xchange.com/browse/MWB-1865): Use internal resources for image build #2 [`320b808`](https://gitlab.open-xchange.com/middleware/core/commit/320b80873839d63ed75692a936d99378034d1b7d)
- [`MWB-1834`](https://jira.open-xchange.com/browse/MWB-1834): Check command line options before accessing the reseller service [`e94ab2a`](https://gitlab.open-xchange.com/middleware/core/commit/e94ab2afd9130dbfb825ec85fbdca681250f3660)
- [`MWB-1865`](https://jira.open-xchange.com/browse/MWB-1865): Use internal resources for image build [`a48433d`](https://gitlab.open-xchange.com/middleware/core/commit/a48433dbbe1726f65a78de087e7e2c8a49ef89bd)
- use proper fallback property for exclude file pattern [`0eadd7d`](https://gitlab.open-xchange.com/middleware/core/commit/0eadd7dc71e6a6673364f56f416971a54aae19b9)
- [`MWB-1866`](https://jira.open-xchange.com/browse/MWB-1866): Orderly consider public folder mode when userizing event data in result tracker [`15274d9`](https://gitlab.open-xchange.com/middleware/core/commit/15274d9f281cc44f0a29e21e1716bfd35773f35a)
- [`MWB-1719`](https://jira.open-xchange.com/browse/MWB-1719): Don't forget to reassign returned Stream instance when applying filter [`a76a018`](https://gitlab.open-xchange.com/middleware/core/commit/a76a01818d6f80796d217cbe243baf29b132c1e0)
- [`MWB-1870`](https://jira.open-xchange.com/browse/MWB-1870): Multifactor Webauthn provider throws UnsupportedOperationException [`8c8a2b7`](https://gitlab.open-xchange.com/middleware/core/commit/8c8a2b712389c33b023925adba8442a28457a3f4)

## [8.5.0-8.6.3] - 2022-10-05

### Added

- [`MW-1785`](https://jira.open-xchange.com/browse/MW-1785): Introduce pre-upgrade task framework [`6396946`](https://gitlab.open-xchange.com/middleware/core/commit/6396946cdb1062f1db5334409ec914f718ad2627)
- [`MW-1815`](https://jira.open-xchange.com/browse/MW-1815): Attach files from drive to chronos events [`fabeec5`](https://gitlab.open-xchange.com/middleware/core/commit/fabeec51e0182c16bea491833c0c8740187b26b0)
- [`MW-1647`](https://jira.open-xchange.com/browse/MW-1647): Handle linked attachments for appointments [`fc5477c`](https://gitlab.open-xchange.com/middleware/core/commit/fc5477c68bf3d0f1d14a5555f7e1934d094890d0)  
  * Externally hosted attachments can now be stored for appointments, with an URI pointing to the data  
  * Introduced new field `uri` for `AttachmentData` object (HTTP API), with column id `891`  
  * Added new field `uri` for `c.o.groupware.attach.AttachmentMetadata` DTO as well  
  * Adjusted interface `c.o.chronos.storage.AttachmentStorage` and implementation to reference non-managed attachments properly during deletions  
  * *Breaking Change* Update task `com.openexchange.groupware.update.tasks.AttachmentAddUriColumnTask` to add column `uri` in table `prg_attachment`  
- [`MW-1817`](https://jira.open-xchange.com/browse/MW-1817): Integrate upgrade preparation bundle into core-mw helm chart [`997fb26`](https://gitlab.open-xchange.com/middleware/core/commit/997fb26ba685a23216e7f14514a2b0b127625d83)
- [`MW-1607`](https://jira.open-xchange.com/browse/MW-1607): Add domain support for push payload [`e924d1b`](https://gitlab.open-xchange.com/middleware/core/commit/e924d1b6202f66e35a36ca28aeeccfa7cbd9eed4)
  - Drive clients can now subscribe for push notifications using domains 'myFiles', 'sharedFiles' and 'publicFiles'  
  - The domain value gets re-inserted into push payload for transport 'apn2'  
  - Removed configuration property com.openexchange.drive.events.apn2.ios.pushDomain  

### Changed

- [`MWB-1849`](https://jira.open-xchange.com/browse/MWB-1849): Improved parsing of OAuth provider error message [`31933c5`](https://gitlab.open-xchange.com/middleware/core/commit/31933c543f5d3ed245b5d743ab829f466fc22bea)
- [`MWB-1826`](https://jira.open-xchange.com/browse/MWB-1826): Added useful DEBUG log messages when adding an image to a signature [`1f1e8f9`](https://gitlab.open-xchange.com/middleware/core/commit/1f1e8f9e011f7974d24c4711e8b788fb2e4cc454)
- [`MWB-1828`](https://jira.open-xchange.com/browse/MWB-1828): Improved handling of `javax.net.ssl.SSLException` [`5180c7b`](https://gitlab.open-xchange.com/middleware/core/commit/5180c7b35c68543c949e5a3630028b6bf04900ca)
- [`MWB-1849`](https://jira.open-xchange.com/browse/MWB-1849): Improved parsing of OAuth provider error message #2 [`c950617`](https://gitlab.open-xchange.com/middleware/core/commit/c95061775fa6a406ec53859e831c8543effd5c35)
- [`MWB-1830`](https://jira.open-xchange.com/browse/MWB-1830): Improved error message in case of denied request [`e0d3c94`](https://gitlab.open-xchange.com/middleware/core/commit/e0d3c9466a99c0bb60081959f9b010a02b18411d)
- [`MWB-1759`](https://jira.open-xchange.com/browse/MWB-1759): Deny requesting large message chunk in case client queries more than only identifier fields [`8e6ddb4`](https://gitlab.open-xchange.com/middleware/core/commit/8e6ddb4dbf8597b7648f49a0be06339f7db85f6f)
- [`MWB-1800`](https://jira.open-xchange.com/browse/MWB-1800): Introduced configuration option [`4e95327`](https://gitlab.open-xchange.com/middleware/core/commit/4e95327af64167e1cb2a15c69b8259ad3f27c57b)
  - "com.openexchange.calendar.storage.rangeIndexHint" to allow insertion of index hints into typical database queries of the calendar module
- [`MWB-1776`](https://jira.open-xchange.com/browse/MWB-1776): Utility method to clear DNS cache [`b9c7ff3`](https://gitlab.open-xchange.com/middleware/core/commit/b9c7ff3e9085325a57226ea62d2793aeeed4c7a3)
- [`MWB-1759`](https://jira.open-xchange.com/browse/MWB-1759): Don't query flags if not required [`24729be`](https://gitlab.open-xchange.com/middleware/core/commit/24729beb0cf55ae1ec330b98ed9e60eb9811ea48)
- [`MWB-1716`](https://jira.open-xchange.com/browse/MWB-1716): Added some helpful logging about bundle status [`1918165`](https://gitlab.open-xchange.com/middleware/core/commit/19181654c5412109f56726a3d0463d941804b1e7)
- [`MWB-1716`](https://jira.open-xchange.com/browse/MWB-1716): Added some helpful logging about bundle status #2 [`d056354`](https://gitlab.open-xchange.com/middleware/core/commit/d056354459de85b1d1c2b8eb534b91d79dea13be)
- [`MWB-1764`](https://jira.open-xchange.com/browse/MWB-1764): Added DEBUG logging when checking status of a mail account yields an error [`2119413`](https://gitlab.open-xchange.com/middleware/core/commit/2119413790e38ca1f0de1a1ecd7f7ab8c9fc499d)
- [`MWB-1750`](https://jira.open-xchange.com/browse/MWB-1750): Improved handling of possible javax.net.ssl.SSLException "Unsupported or unrecognized SSL message" [`0af276a`](https://gitlab.open-xchange.com/middleware/core/commit/0af276a9850b18c5761fb57234e6e49d8d637312)
- [`MWB-1776`](https://jira.open-xchange.com/browse/MWB-1776): Added logging when DNS cache has been cleared [`fe93ae2`](https://gitlab.open-xchange.com/middleware/core/commit/fe93ae2ab939a1df12331c29fa63ee36874c5438)
- [`MWB-1759`](https://jira.open-xchange.com/browse/MWB-1759): Delay initialization of TLongObjectHashMap [`bbb6a9f`](https://gitlab.open-xchange.com/middleware/core/commit/bbb6a9fefeb1c7b4453f7d90eafb8c99eb419e24)
- [`MWB-1759`](https://jira.open-xchange.com/browse/MWB-1759): Nullify intermediate result [`103f70f`](https://gitlab.open-xchange.com/middleware/core/commit/103f70f964c34ecb34f0c3f33f0f28c0dce9d05f)

### Removed

- [`MW-1866`](https://jira.open-xchange.com/browse/MW-1866): Remove bundle com.openexchange.quartz [`c1975fc`](https://gitlab.open-xchange.com/middleware/core/commit/c1975fc1cf946e14f448e603fedc3ae6340e486d)
- [`MW-1817`](https://jira.open-xchange.com/browse/MW-1817): Remove parallel container execution for update job [`a04115f`](https://gitlab.open-xchange.com/middleware/core/commit/a04115f90064020770f56836939a5b897e27d027)

### Fixed

- [`MWB-1842`](https://jira.open-xchange.com/browse/MWB-1842): Prophylactically decode potentially MIME-encoded strings in property values in iCalendar files from MS Exchange [`24af8ec`](https://gitlab.open-xchange.com/middleware/core/commit/24af8ec1ebe296f428322ecb3cc12ea0f114621e)
- [`MWB-1848`](https://jira.open-xchange.com/browse/MWB-1848): removed fallback value for manifest version field [`8b468a8`](https://gitlab.open-xchange.com/middleware/core/commit/8b468a8ca05b5b7ec4c407b69e9c213080853e55)
- [`MWB-1839`](https://jira.open-xchange.com/browse/MWB-1839): Use dedicated introductions for forwarded meeting requests the user is not invited to [`e03a09e`](https://gitlab.open-xchange.com/middleware/core/commit/e03a09e667e5ccb7e353f29a295e779011af863c)
- [`MWB-1608`](https://jira.open-xchange.com/browse/MWB-1608): Fixed RuntimeExceptions in calendar stack [`bd422ac`](https://gitlab.open-xchange.com/middleware/core/commit/bd422acb277e62e806c2ede7c4fac750fef7de46)
- [`MWB-1808`](https://jira.open-xchange.com/browse/MWB-1808): properly detect reminders with missing permissions [`bb3f1e6`](https://gitlab.open-xchange.com/middleware/core/commit/bb3f1e678164564f9c9d94bff79a74be11cec347)
- [`MWB-1813`](https://jira.open-xchange.com/browse/MWB-1813): Added documentation for mail?action=expunge [`56bff39`](https://gitlab.open-xchange.com/middleware/core/commit/56bff39664dd852d47a6cd6d0ad28e0630fc91a9)
- [`MWB-1811`](https://jira.open-xchange.com/browse/MWB-1811): Ensure internal entity is admin, prevent permission [`57ca47b`](https://gitlab.open-xchange.com/middleware/core/commit/57ca47bbdfc1a08b858fed3225d754b4e50a64a1) [`00b7702`](https://gitlab.open-xchange.com/middleware/core/commit/00b7702ed2b8279ae9fe89871600f9c21f3f230f) [`1358b10`](https://gitlab.open-xchange.com/middleware/core/commit/1358b10b70bbe4216304372f765a0e99337009ae)
- [`MWB-1838`](https://jira.open-xchange.com/browse/MWB-1838): Yield no result when auto-processing REQUEST with party crasher, let client re-apply iTip independently of message status flag [`8ec6208`](https://gitlab.open-xchange.com/middleware/core/commit/8ec62084894d19c38365c7d3973ec1a33b27f5f5)
- [`MWB-1840`](https://jira.open-xchange.com/browse/MWB-1840): Return empty ajax respone if no event was found during resolve action [`81be74c`](https://gitlab.open-xchange.com/middleware/core/commit/81be74c8a5586ff61e2d7b7a64cd40009ef8851e)
- Add missing com.openexchange.gab import in bundle com.openexchange.admin.plugin.hosting [`9749ecb`](https://gitlab.open-xchange.com/middleware/core/commit/9749ecba3407d48934633bdc37167ee579efb26f)
- [`MWB-1805`](https://jira.open-xchange.com/browse/MWB-1805): Use URL-decoded variant of username in Authorization header for macOS Contacts client if applicable [`7d805e8`](https://gitlab.open-xchange.com/middleware/core/commit/7d805e8dd2f7386c4302c9b0301bad4904fc7cc2)
- [`MWB-1735`](https://jira.open-xchange.com/browse/MWB-1735): Fixed links in Command Line Tools articles [`b9ac1ac`](https://gitlab.open-xchange.com/middleware/core/commit/b9ac1ac8737562b8517b62bbce2262dae3018e9a)
- [`MWB-1711`](https://jira.open-xchange.com/browse/MWB-1711): Removed obsolete ContextDbLookupPluginInterface [`d9309b1`](https://gitlab.open-xchange.com/middleware/core/commit/d9309b1f080597ffbf92d4bddcebbdbc620888bc)
- [`MWB-1721`](https://jira.open-xchange.com/browse/MWB-1721): Evaluate 'X-Device-User-Agent' and pretty print common EAS devices in active clients overview [`7b197c1`](https://gitlab.open-xchange.com/middleware/core/commit/7b197c15fa581179e2f9ec65553fdd539f2538e0)
- [`MWB-1702`](https://jira.open-xchange.com/browse/MWB-1702): Skip premature cache invalidations to prevent race conditions upon folder update [`7e643b8`](https://gitlab.open-xchange.com/middleware/core/commit/7e643b89754f304919b86dac92a1420c15add557)
- [`MWB-1787`](https://jira.open-xchange.com/browse/MWB-1787): Prefix download URI with current scheme/host if no absolute URI is configured in manifest [`5424d16`](https://gitlab.open-xchange.com/middleware/core/commit/5424d16ec165a41b50be9dace94238ee6d6a41a5)
- [`MWB-1737`](https://jira.open-xchange.com/browse/MWB-1737): Removed obsolete ETag check after HTTP 409 errors [`eac8317`](https://gitlab.open-xchange.com/middleware/core/commit/eac8317eab2540c690ff7bef23574899fc161a45)
- [`MW-1817`](https://jira.open-xchange.com/browse/MW-1817): Proper yaml in overwrite configmap if no properties are set [`7c4f3c8`](https://gitlab.open-xchange.com/middleware/core/commit/7c4f3c8e88e3f51df4f5cb553c037080a846d72f)
- [`MWB-1760`](https://jira.open-xchange.com/browse/MWB-1760): Properly indicate "share not found" status for invalid targets of anonymous shares [`152f332`](https://gitlab.open-xchange.com/middleware/core/commit/152f332bcbcc34b5270fb95abd713998a77a7440)
- Apply maxHeapSize to init containers [`493c5e4`](https://gitlab.open-xchange.com/middleware/core/commit/493c5e459c2794f6429157fa61a2adb391e73a5d)
- [`MWB-1722`](https://jira.open-xchange.com/browse/MWB-1722): Do not convert aperture value, because we already read the f-number from exif data [`c5d97dc`](https://gitlab.open-xchange.com/middleware/core/commit/c5d97dc137e31621214d8200dbaea26a115dee1d)
- Disable hz update bundle by default [`01c5d7d`](https://gitlab.open-xchange.com/middleware/core/commit/01c5d7d255b56dcf3fd6015a30496524432c6e82)

### Security

- [`MW-1836`](https://jira.open-xchange.com/browse/MW-1836): Updated yq version [`79fca08`](https://gitlab.open-xchange.com/middleware/core/commit/79fca08cfe1b6a18c896894cd4f4a8e8064c5ec1)
- [`MW-1836`](https://jira.open-xchange.com/browse/MW-1836): Replaced default-mysql-client with latest mariadb-client from official repository [`44a5255`](https://gitlab.open-xchange.com/middleware/core/commit/44a52557b1a36ab90a3235683dfe46e81e0724a0)

<!-- References -->
[8.5.0-8.6.3]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.5.0...8.6.3
[8.7.0-8.7.19]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.7.0...8.7.19
[8.8.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.7.19...8.8.0
[8.9.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.8.0...8.9.0
[8.10.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.9.0...8.10.0
[8.11.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.10.0...8.11.0
[8.12.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.11.0...8.12.0
[8.13.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.12.0...8.13.0
[8.14.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.13.0...8.14.0
[8.15.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.14.0...8.15.0
[8.16.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.15.0...8.16.0
[8.17.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.16.0...8.17.0
[8.18.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.17.0...8.18.0
[8.19.0]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.18.0...8.19.0
