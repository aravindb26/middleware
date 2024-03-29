---
title: vCard Mapping
icon: fas fa-exchange-alt
tags: vCard, Contacts, CardDAV
---

This article describes the mapping rules that are used when converting contacts to or from vCards using the Open-Xchange Server.

# Introduction

In former releases of the Open-Xchange Server, when a contact was imported from a vCard, the server only stored vCard parameter values that could be mapped to internal OX properties of a contact, and ignored all further unknown or not mappable properties. Especially during CardDAV synchronization, this often led to partial data loss after the client fetched a previously imported vCard again from the server, for example if the client tried to save a third instant messenger property, a geographical location, or the PGP public key of the contact.

To circumvent these kinds of problems, the vCard import- and export-workflow was redesigned, and now allows to store the original vCard as well besides the mappable contact properties. If enabled, the original data as sent by the client is stored automatically in the context filestore, and is considered again during export in a way where any updates to the OX contact are merged back into the original vCard.

To support the described merging process, the mapping between OX contact attributes and vCard properties has been slightly adjusted to allow a deterministic conversion in both directions. Additionally, all OX contact attributes that are not covered by a well-known vCard property are exported as extended properties, so that all OX properties in an exported vCard are recognized correctly again during import. Further details about the mapping are documented in [Mapping Rules](#mapping-rules) below.

# Configuration

Whether original vCards should be stored or not, as well as their maximum allowed size, can be controlled by some new properties in the configuration file ``contact.properties``:

```properties
 # Specifies whether the original files are persisted during vCard import or 
 # CardDAV synchronization. If enabled, the original vCard files will be stored 
 # in the appropriate filestore and are considered during export again. If 
 # disabled, all not mapped information is discarded and is no longer available 
 # when exporting the vCard again. 
 # Defaults to "true". 
 com.openexchange.contact.storeVCards=true 
 
 # Configures the maximum allowed size of a (single) vCard file in bytes. 
 # vCards larger than the configured maximum size are rejected and not parsed 
 # by the server. A value of "0" or smaller is considered as unlimited. 
 # Defaults to 4194304. 
 com.openexchange.contact.maxVCardSize=4194304 
```

Please note that storing the original vCard requires an attached filestore for the context, so in case there is none (as indicated via ``com.openexchange.capability.filestore=false``), this feature is not available - importing and exporting vCards then works much like before, i.e. no original vCard is saved, and only mappable properties are persisted.

# Workflows

When importing and exporting contacts to or from vCard, there are basically two operation modes: On the one hand, the independent import or export of a contact to or from a vCard, and on the other hand, the process of merging contact updates into an original vCard (or the other way around, merging updated vCards into contacts).

While the first one is mainly used for one-time import-/export-purposes, the latter one is performed during a continuous synchronization of contacts as done via CardDAV, yet also a one-time export will pick up and merge with an existing vCard in case it exists. So, importing and exporting basically involves the following steps:    

* Export a contact with no original vCard:
  * Start with a blank vCard
  * Write all mapped OX properties into the vCard based on the mapping rules below

* Export a contact with an original vCard:
  * Load the stored vCard
  * Merge all mapped OX properties into the vCard based on the mapping rules below

* Import a vCard and create a new contact:
  * Create a new contact
  * Parse all mapped properties based on the mapping rules below
  * Additionally, store the original vCard

* Import a vCard and update an existing contact:
  * Load the stored contact
  * Parse all mapped properties based on the mapping rules below
  * Store or overwrite a previously stored original vCard

# Mapping Rules

Mapping between known OX contact attributes and vCard attributes is based on the following rules, where some of the mappings are simple and direct, while others are bit more complicated due to their different representations in the underlying data models. 

## Simple Mappings

Simple mappings refer to properties where some kind of direct mapping between OX contact attributes and vCard attributes exist. Since many properties in vCards can be defined more than once, the mapping uses the most preferred (based on the ``PREF`` type parameter) or first property in the vCard. The following list gives an overview about the directly mapped, simple properties:

| OX column ID | OX attribute name | vCard property | Remarks |
|:-------------|:------------------|:---------------|:--------|
|          517 |       anniversary |    ANNIVERSARY |         |
|          511 |          birthday |           BDAY |         |
|          100 |        categories |     CATEGORIES |         |
|          101 |      private_flag |          CLASS | Only exported if <tt>true</tt> |
|          500 |      display_name |             FN |         |
|          515 |          nickname |       NICKNAME |         |
|          518 |              note |           NOTE |         |
|          570 |            image1 |          PHOTO |         |
|          515 |          nickname |       NICKNAME |         |
|            5 |     last_modified |            REV | Ignored during import |
|          520 |          position |          TITLE |         |
|          514 |        profession |           ROLE |         |
|          223 |               uid |            UID |         |
|          558 |               url |            URL |         |       

## Extended Mappings

Besides the mappings where a direct property mapping is possible, there are some more mappings from OX contact fields to other well-known vCard properties or custom extended properties:

| OX column ID | OX attribute name | vCard property | Remarks |
|:-------------|:------------------|:---------------|:--------|
|          537 |    assistant_name |             X-ASSISTANT | Additional fallbacks to X-MS-ASSISTANT, X-KADDRESSBOOK-X-AssistantsName, X-EVOLUTION-ASSISTANT |
|          536 |      manager_name |               X-MANAGER | Additional fallbacks to X-MS-MANAGER, X-KADDRESSBOOK-X-ManagersName, X-EVOLUTION-MANAGER |
|          516 |       spouse_name |                X-SPOUSE | Additional fallbacks to X-MS-SPOUSE, X-KADDRESSBOOK-X-SpouseName, X-EVOLUTION-SPOUSE |
|          513 | number_of_children |              X-MS-CHILD |        |
|          616 |   yomi_first_name |   X-PHONETIC-FIRST-NAME |        |
|          617 |    yomi_last_name |    X-PHONETIC-LAST-NAME |        |
|          571 |       userfield01 |       X-OX-USERFIELD-01 |        |
|          572 |       userfield02 |       X-OX-USERFIELD-02 |        |
|          573 |       userfield03 |       X-OX-USERFIELD-03 |        |
|          574 |       userfield04 |       X-OX-USERFIELD-04 |        |
|          575 |       userfield05 |       X-OX-USERFIELD-05 |        |
|          576 |       userfield06 |       X-OX-USERFIELD-06 |        |
|          577 |       userfield07 |       X-OX-USERFIELD-07 |        |
|          578 |       userfield08 |       X-OX-USERFIELD-08 |        |
|          579 |       userfield09 |       X-OX-USERFIELD-09 |        |
|          580 |       userfield10 |       X-OX-USERFIELD-10 |        |
|          581 |       userfield11 |       X-OX-USERFIELD-11 |        |
|          582 |       userfield12 |       X-OX-USERFIELD-12 |        |
|          583 |       userfield13 |       X-OX-USERFIELD-13 |        |
|          584 |       userfield14 |       X-OX-USERFIELD-14 |        |
|          585 |       userfield15 |       X-OX-USERFIELD-15 |        |
|          586 |       userfield16 |       X-OX-USERFIELD-16 |        |
|          587 |       userfield17 |       X-OX-USERFIELD-17 |        |
|          588 |       userfield18 |       X-OX-USERFIELD-18 |        |
|          589 |       userfield19 |       X-OX-USERFIELD-19 |        |
|          590 |       userfield20 |       X-OX-USERFIELD-20 |        |
|          522 |       room_number |        X-OX-ROOM-NUMBER |        |
|          535 |              info |               X-OX-INFO |        |
|          529 | number_of_employee | X-OX-NUMBER-OF-EMPLOYEE |        |
|          534 | business_category |  X-OX-BUSINESS-CATEGORY |        |
|          532 | commercial_register|X-OX-COMMERCIAL-REGISTER |        |
|          531 |            tax_id |             X-OX-TAX-ID |        |
|          530 |      sales_volume |       X-OX-SALES-VOLUME |        |
|          521 |     employee_type |      X-OX-EMPLOYEE-TYPE |        |
|          512 |    marital_status |     X-OX-MARITAL-STATUS |        |
|          618 |      yomi_company |       X-OX-YOMI-COMPANY |        |
|          102 |       color_label |        X-OX-COLOR-LABEL |        |


## Advanced mappings

Apart from the simple mappings above, there are also more complex vCard properties that refer to or are influenced by more than one OX contact property.

### Addresses

* https://tools.ietf.org/html/rfc6350#section-6.3.1 
* OX contacts have three address types: ``business``, ``home`` and and ``other``
* For each, OX stores the properties ``street``, ``city``, ``state``, ``postal code`` and ``country``
* Additionally, the mailing label for snail mail (as set by some Outlook clients) is held in ``addressHome``, ``addressBusiness`` and ``addressOther`` 
* In vCards, ``ADR`` properties have the cardinality ``*``, and may have an assigned ``LABEL`` for the mailing label
* Each one is stored as structured value (``post office box``, ``extended address``, ``street address``, ``locality``, ``region``, ``postal code``, ``country name``)
* The address parts are mapped between OX contacts and vCards as follows:
  * ``street`` <-> ``street address``
  * ``city`` <-> ``locality``
  * ``state`` <-> ``region``
  * ``postal code`` <-> ``postal code``
  * ``country`` <-> ``country name``
  * ``address`` <-> ``LABEL``
* Hence, there are no corresponding OX fields for ``post office box`` and ``extended address``
* During import, the mapping to vCard ``ADR`` properties is based on the ``TYPE`` parameters and follows the following rules:
  * For the ``business`` address, the first ``ADR`` property with type ``work`` and type ``pref`` is parsed <br />if not found, parse the first with type ``work`` <br />if not found, delete the corresponding OX address properties
  * For the ``home`` address, the first ``ADR`` property with type ``home`` and type ``pref`` is parsed <br />if not found, parse the first with type ``home`` <br />if not found, delete the corresponding OX address properties
  * For the ``other`` address, the first ``ADR`` property with type ``x-other`` and type ``pref`` is parsed <br />if not found, parse the first with type ``x-other`` <br />if not found, parse the first without type parameters ``home`` and ``work``  <br />if not found, delete the corresponding OX address properties
* During export, the OX contact addresses are serialized to a previously saved original vCard as follows:
  * Generally, if no single OX property that forms a specific address is set anymore, the corresponding vCard property is deleted during the update
  * For the ``business`` address, the first ``ADR`` property with type ``work`` and type ``pref`` is updated <br />if not found, update the first property with type ``work`` and add the ``pref`` type <br />if not found, add a new property with types ``work`` and ``pref``
  * For the ``home`` address, the first ``ADR`` property with type ``home`` and type ``pref`` is updated <br />if not found, update the first property with type ``home`` <br />if not found, add a new property with type ``home``
  * For the ``other`` address, the first ``ADR`` property with type ``x-other`` and type ``pref`` is updated <br />if not found, update the first property with type ``x-other`` <br />if not found, update the first property without types ``home`` and ``work`` and add the ``x-other`` type <br />if not found, add a new property with type ``x-other``

### E-Mails

* https://tools.ietf.org/html/rfc6350#section-6.4.2
* OX contacts allow storing three different e-mail addresses: ``email1`` (~business), ``email2`` (~home) and ``email3`` (~other)
* A ``telex`` address is stored at ``telephone_telex`` in OX contacts, yet it is expressed as ``EMAIL`` type in vCards    
* In vCards, ``EMAIL`` properties have the cardinality *
* During import, the mapping to vCard ``EMAIL`` properties is based on the ``TYPE`` parameters and follows the following rules:
  * Generally, if no matching vCard ``EMAIL`` property is found, the corresponding OX email property is deleted
  * For the ``business`` mail address, the first ``EMAIL`` property with type ``work`` and type ``pref`` is parsed <br />if not found, parse the first with type ``work`` <br />if not found, and all ``EMAIL`` properties have no distinguishing type (i.e. none is marked ``work``, ``home``, ``x-other`` or ``TLX``), parse the most preferred one
  * For the ``home`` mail address, the first ``EMAIL`` property with type ``home`` and type ``pref`` is parsed <br />if not found, parse the first with type ``home`` <br />if not found, and all ``EMAIL`` properties have no distinguishing type (i.e. none is marked ``work``, ``home``, ``x-other`` or ``TLX``), parse the 2nd most preferred one
  * For the ``other`` mail address, the first ``EMAIL`` property with type ``x-other`` and type ``pref`` is parsed <br />if not found, parse the first with type ``x-other`` <br />if not found, parse the ``EMAIL`` property that is grouped with an ``X-ABLabel`` with value ``_$!<Other>!$_`` <br />if not found, parse the first without type parameter ``work``, ``home``, ``x-other`` or ``TLX`` <br />if not found, and all ``EMAIL`` properties have no distinguishing type (i.e. none is marked ``work``, ``home``, ``x-other`` or ``TLX``), parse the 3rd most preferred one
  * For the ``telex`` address, the first ``EMAIL`` property with type ``TLX`` and type ``pref`` is parsed <br />if not found, parse the first with type ``TLX``
* During export, the OX contact mail addresses are serialized to a previously saved original vCard as follows:
  * Generally, if no single OX property that forms a specific email is set anymore, the corresponding vCard property is deleted during the update
  * For the ``business`` mail address, the first ``EMAIL`` property with type ``work`` and type ``pref`` is updated <br />if not found, update the first with type ``work`` and add the ``pref`` type <br />if not found, and all ``EMAIL`` properties have no distinguishing type (i.e. none is marked ``work``, ``home``, ``x-other`` or ``TLX``), update the most preferred one  and add the ``work`` type <br />if not found, add a new property with types ``work`` and ``pref``
  * For the ``home`` mail address, the first ``EMAIL`` property with type ``home`` and type ``pref`` is updated <br />if not found, update the first with type ``home`` <br />if not found, and all ``EMAIL`` properties have no distinguishing type (i.e. none is marked ``work``, ``home``, ``x-other`` or ``TLX``), update the 2nd most preferred one and add the ``home`` type <br />if not found, add a new property with type ``home``
  * For the ``other`` mail address, the first ``EMAIL`` property with type ``x-other`` and type ``pref`` is updated <br />if not found, update the first with type ``x-other`` <br />if not found, update the ``EMAIL`` property that is grouped with an ``X-ABLabel`` with value ``_$!<Other>!$_`` and add the ``x-other`` type <br />if not found, and all ``EMAIL`` properties have no distinguishing type (i.e. none is marked ``work``, ``home``, ``x-other`` or ``TLX``), update the 3rd most preferred one and add the ``x-other`` type <br />if not found, add a new property with types ``x-other``
  * For the ``telex`` address, the first ``EMAIL`` property with type ``tlx`` and type ``pref`` is updated <br />if not found, update the first with type ``tlx`` <br />if not found, add a new property with type ``tlx``

### Instant Messenger

* https://tools.ietf.org/html/rfc6350#section-6.4.3
* OX contacts allow to store two IM addresses: ``instant_messenger1`` (~business), ``instant_messenger2`` (~home)
* OX contacts store IM addresses as arbitrary strings, there are no special protocol semantics applied
* In vCards, IMPP properties have the cardinality *
* In vCards, an instant messenger addresses are formed as URI, where the protocol part indicate the messenger type (such as 'aim' or 'irc') 
* During import, the mapping to vCard ``IMPP`` properties is based on the ``TYPE`` parameters and follows the following rules:
  * The whole URI string is stored during import  
  * Generally, if no matching vCard ``IMPP`` property is found, the corresponding OX instant messenger properties are deleted
  * For the ``business`` IM address, the first ``IMPP`` property with type ``work`` and type ``pref`` is parsed <br />if not found, parse the first with type ``work`` <br /> if not found, and all IMPP properties have no distinguishing type (i.e. none is marked work or home), parse the most preferred one
  * For the ``home`` IM address, the first ``IMPP`` property with type ``home`` and type ``pref`` is parsed <br />if not found, parse the first with type ``home`` <br /> if not found, and all IMPP properties have no distinguishing type (i.e. none is marked work or home), parse the 2nd most preferred one
* During export, the OX contact IM addresses are serialized to a previously saved original vCard as follows:
  * If the stored value represents a URI, this URI is used directly, otherwise, a synthetic URI is constructed    
  * Generally, if no single OX property that forms a specific IM address is set anymore, the corresponding vCard property is deleted during the update
  * For the ``business`` IM address, the first ``IMPP`` property with type ``work`` and type ``pref`` is updated <br />if not found, update the first with type ``work`` and add the ``pref`` type <br />if not found, add a new property with types ``work`` and ``pref``
  * For the ``home`` IM address, the first ``IMPP`` property with type ``home`` and type ``pref`` is updated <br />if not found, update the first with type ``home`` <br />if not found, add a new property with types ``home``

### Organization

* https://tools.ietf.org/html/rfc6350#section-6.6.4
* OX contacts store organizational attributes at ``company``, ``department`` and ``branches``
* In vCards, the structured ``ORG`` property holds the ``X.520 Organization Name and Organization Unit attributes [CCITT.X520.1988]``
* During import, the mapping to the vCard ``ORG`` property is based on the following rules:
  * The first value from the structured ``ORG`` property is used as ``company``
  * The second value from the structured ``ORG`` property is used as ``department``
  * All further values from the structured ``ORG`` property are used as ``branches``
  * If no ``ORG`` property is found, all corresponding contact attributes are deleted, too
* During export, the OX contact's organizational attributes are serialized to a previously saved original vCard as follows:
  * If no single OX property that forms the ``ORG`` property is set anymore, the corresponding vCard property is deleted during the update
  * The structured value is always rewritten
  * The ``company`` is used as first value of the structured value
  * The ``department`` is used as second value of the structured value
  * The ``branches`` are used as further values of the structured value

### Telephone

* https://tools.ietf.org/html/rfc6350#section-6.4.1
* OX contacts store telephone and fax numbers at the fields ``telephone_business1``, ``telephone_business2``, ``fax_business``, ``telephone_callback``, ``telephone_car``, ``telephone_company``, ``telephone_home1``, ``telephone_home2``, ``fax_home``, ``cellular_telephone1``, ``cellular_telephone2``, ``telephone_other``, ``fax_other``, ``telephone_isdn``, ``telephone_pager``, ``telephone_primary``, ``telephone_radio``, ``telephone_ttytdd``, ``telephone_ip``, ``telephone_assistant`` 
* In vCards, TEL properties have the cardinality *
* During import, the mapping from vCard ``TEL`` properties is based on the ``TYPE`` parameters and follows the following rules:
  * Generally, if no matching vCard ``TEL`` property is found, the corresponding OX telephone properties are deleted
  * For the ``telephone_pager`` attribute, the most preferred ``TEL`` property with type ``pager`` is parsed
  * For the ``telephone_ttytdd`` attribute, the most preferred ``TEL`` property with type ``textphone`` is parsed
  * For the ``telephone_isdn`` attribute, the most preferred ``TEL`` property with type ``isdn`` is parsed
  * For the ``telephone_car`` attribute, the most preferred ``TEL`` property with type ``car`` is parsed
  * For the ``cellular_telephone1`` attribute, the most preferred ``TEL`` property with type ``cell`` is parsed
  * For the ``cellular_telephone2`` attribute, the most preferred ``TEL`` property with types ``cell`` and ``x-2nd`` is parsed <br />if not found, parse the second most preferred ``TEL`` property with type ``cell``
  * For the ``telephone_callback`` attribute, the most preferred ``TEL`` property with type ``x-callback`` is parsed
  * For the ``telephone_company`` attribute, the most preferred ``TEL`` property with type ``x-company`` is parsed
  * For the ``telephone_assistant`` attribute, the most preferred ``TEL`` property with type ``x-assistant`` is parsed
  * For the ``telephone_ip`` attribute, the most preferred ``TEL`` property with type ``x-ip`` is parsed
  * For the ``telephone_radio`` attribute, the most preferred ``TEL`` property with type ``x-radio`` is parsed
  * For the ``telephone_primary`` attribute, the most preferred ``TEL`` property with type ``x-primary`` is parsed
  * For the ``fax_business`` attribute, the most preferred ``TEL`` property with types ``fax`` and type ``work`` is parsed
  * For the ``fax_home`` attribute, the most preferred ``TEL`` property with types ``fax`` and type ``home`` is parsed
  * For the ``fax_other`` attribute, the most preferred ``TEL`` property with types ``fax`` and type ``x-other`` is parsed
  * For the ``telephone_business1`` attribute, the most preferred ``TEL`` property with types ``voice`` and type ``work`` is parsed <br />if not found, parse the most preferred ``TEL`` property with type ``work`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone``
  * For the ``telephone_business2`` attribute, the most preferred ``TEL`` property with types ``voice``, ``work`` and ``x-2nd`` is parsed <br />if not found, parse the most preferred ``TEL`` property with types ``work`` and ``x-2nd`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` <br />if not found, parse the second most preferred ``TEL`` property with types ``voice`` and ``work`` <br />if not found, parse the second most preferred ``TEL`` property with types ``work`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone``
  * For the ``telephone_home1`` attribute, the most preferred ``TEL`` property with types ``voice`` and type ``home`` is parsed <br />if not found, parse the most preferred ``TEL`` property with type ``home`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone``
  * For the ``telephone_home2`` attribute, the most preferred ``TEL`` property with types ``voice``, ``home`` and ``x-2nd`` is parsed <br />if not found, parse the most preferred ``TEL`` property with types ``home`` and ``x-2nd`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` <br />if not found, parse the second most preferred ``TEL`` property with types ``voice`` and ``home`` <br />if not found, parse the second most preferred ``TEL`` property with types ``home`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone``
  * For the ``telephone_other`` attribute, the most preferred ``TEL`` property with type ``x-other`` is parsed
* During export, the OX contact telephone numbers are serialized to a previously saved original vCard as follows:
  * Generally, if the OX telephone property is not set anymore, the corresponding vCard property is deleted during the update
  * For the ``telephone_pager`` attribute, the most preferred ``TEL`` property with type ``pager`` is updated <br />if not found, add a new ``TEL`` property with type ``pager``  
  * For the ``telephone_ttytdd`` attribute, the most preferred ``TEL`` property with type ``textphone`` is updated <br />if not found, add a new ``TEL`` property with type ``textphone``
  * For the ``telephone_isdn`` attribute, the most preferred ``TEL`` property with type ``isdn`` is updated <br />if not found, add a new ``TEL`` property with type ``isdn``  
  * For the ``telephone_car`` attribute, the most preferred ``TEL`` property with type ``car`` is updated <br />if not found, add a new ``TEL`` property with type ``car``  
  * For the ``cellular_telephone1`` attribute, the most preferred ``TEL`` property with type ``cell`` is updated <br />if not found, add a new ``TEL`` property with type ``cell``
  * For the ``cellular_telephone2`` attribute, the most preferred ``TEL`` property with types ``cell`` and ``x-2nd`` is updated <br />if not found, parse the update the most preferred ``TEL`` property with type ``cell`` and add the type ``x-2nd``  <br />if not found, add a new ``TEL`` property with types ``cell`` and ``x-2nd``
  * For the ``telephone_callback`` attribute, the most preferred ``TEL`` property with type ``x-callback`` is updated <br />if not found, add a new ``TEL`` property with type ``x-callback``  
  * For the ``telephone_company`` attribute, the most preferred ``TEL`` property with type ``x-company`` is updated <br />if not found, add a new ``TEL`` property with type ``x-company``  
  * For the ``telephone_assistant`` attribute, the most preferred ``TEL`` property with type ``x-assistant`` is updated <br />if not found, add a new ``TEL`` property with type ``x-assistant``  
  * For the ``telephone_ip`` attribute, the most preferred ``TEL`` property with type ``x-ip`` is updated <br />if not found, add a new ``TEL`` property with type ``x-ip``  
  * For the ``telephone_radio`` attribute, the most preferred ``TEL`` property with type ``x-radio`` is updated <br />if not found, add a new ``TEL`` property with type ``x-radio``  
  * For the ``telephone_primary`` attribute, the most preferred ``TEL`` property with type ``x-primary`` is updated <br />if not found, add a new ``TEL`` property with type ``x-primary``  
  * For the ``fax_business`` attribute, the most preferred ``TEL`` property with types ``fax`` and type ``work`` is updated <br />if not found, add a new ``TEL`` property with types ``fax`` and `work
  * For the ``fax_home`` attribute, the most preferred ``TEL`` property with types ``fax`` and type ``home`` is updated <br />if not found, add a new ``TEL`` property with types ``fax`` and ``home``  
  * For the ``fax_other`` attribute, the most preferred ``TEL`` property with types ``fax`` and type ``x-other`` is updated <br />if not found, add a new ``TEL`` property with types ``fax`` and ``x-other``
  * For the ``telephone_business1`` attribute, the most preferred ``TEL`` property with types ``voice`` and type ``work`` is updated <br />if not found, update the most preferred ``TEL`` property with type ``work`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` <br />if not found, add a new ``TEL`` property with types ``voice``, ``work`` and ``pref``
  * For the ``telephone_business2`` attribute, the most preferred ``TEL`` property with types ``voice``, ``work`` and ``x-2nd`` is updated <br />if not found, update the most preferred ``TEL`` property with types ``work`` and ``x-2nd`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` <br />if not found, update the second most preferred ``TEL`` property with types ``voice`` and ``work`` and add the type ``x-2nd`` <br />if not found, update the second most preferred ``TEL`` property with types ``work`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` and add the type ``x-2nd`` <br />if not found, add a new ``TEL`` property with types ``voice``, ``work`` and ``x-2nd``
  * For the ``telephone_home1`` attribute, the most preferred ``TEL`` property with types ``voice`` and type ``home`` is updated <br />if not found, update the most preferred ``TEL`` property with type ``home`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` <br />if not found, add a new ``TEL`` property with types ``voice``, ``home`` and ``pref``
  * For the ``telephone_home2`` attribute, the most preferred ``TEL`` property with types ``voice``, ``home`` and ``x-2nd`` is updated <br />if not found, update the most preferred ``TEL`` property with types ``home`` and ``x-2nd`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` <br />if not found, update the second most preferred ``TEL`` property with types ``voice`` and ``home`` and add the type ``x-2nd`` <br />if not found, update the second most preferred ``TEL`` property with types ``home`` and without any of the types ``text``, ``fax``, ``cell``, ``video``, ``pager``, ``textphone`` and add the type ``x-2nd`` <br />if not found, add a new ``TEL`` property with types ``voice``, ``home`` and ``x-2nd``
  * For the ``telephone_other`` attribute, the most preferred ``TEL`` property with type ``x-other`` is updated <br />if not found, add a new ``TEL`` property with types ``voice`` and ``x-other``

### (Structured) name

* https://tools.ietf.org/html/rfc6350#section-6.2.2 
* OX contacts stores name-related properties at the fields: ``first_name``, ``last_name``, ``second_name``, ``suffix`` and ``title`` 
* In vCards, the structured property ``N`` stores the different parts of a name, which are ``family``, ``given``, ``additional``, ``prefixes`` and ``suffixes``
* The parts are mapped between OX contacts and vCards as follows:
  * ``last_name`` <-> ``family``
  * ``first_name`` <-> ``given``
  * ``second_name`` <-> ``additional``
  * ``title`` <-> ``prefixes``
  * ``suffix`` <-> ``suffixes``
* During import, the mapping to the vCard ``N`` property is based on the following rules:
  * If there's no ``N`` property in the vCard, all name-related properties of the contact are deleted
  * Otherwise, the properties are imported based on the above mapping, with the multi-values in ``prefixes`` and ``suffixes`` being concatenated into the corresponding OX contact properties ``title`` and ``suffix`` 
* During export, the OX contact name properties are serialized to a previously saved original vCard as follows:
  * If no single OX property that forms the structured name is set anymore, the corresponding vCard property is deleted during the update
  * Otherwise, the names serialized into a new or already existing ``N`` property following the above mapping rules, with the multi-value properties ``prefixes`` and ``suffixes`` being filled with the ``title`` and ``suffix`` values, split by whitespace

