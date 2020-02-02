# Cross Language Validation Schema - V0.1
JSON schema that specifies validation rules in a language independant manner to enable cross language validation.

An online, interactive JSON Schema validator can be found here: https://www.jsonschemavalidator.net/

## Table of Contents
* [TL;DR](#tl;dr)
* [Motivation](#motivation)
* [Documentation](#documentation)
* [Implementations](#implementations)
* [TODOs](#todos)

# TL;DR
The purbose of this _JSON Schema_ is to describe the structure of _complex validation rules_, independent of a specific programming language. The resulting JSON documents are intended to be used in applications that involve multiple components written in different programmming languages where the rules have to be validated in several components, e.g. in a frontend written in ES6 and a backend written in Java. 

The **main objective** is to apply the [DRY principle](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) even for validation rules, according to the motto "define once, validate everywhere" (DOVE :)

Of cause this requires the implementation of generic validators as well as the implementation of JSON producers resp. JSON consumers for valid JSON documents in the programming languages that are involved.

# Motivation
Validation of input data plays a crucial role in almost any web app. Whereby the server-side validation is a must, because the server should never trust the client-side.
But client side validation is also important for a good user experience.

The idea for this JSON Schema was inspired by a web application that had _many_ non trivial validation rules. The front-end of this application was written in Angular, while the back-end was written in Java. Most of these validation rules should be utilized also on the client-side. On the one hand to provide good user guidance, on the other hand to avoid numerious client-server round trips:
- For a _mandatory_ object property the corresponding form input field should be decorated with a visual indicator and the submit button should be disabled as long as the user didn't enter a value for this property.
- For an _immutable_ (a.k.a. _read-only_) object property the corresponding form input field should be set to disabled state.
- For _content related validation rules_ the client should give immediate feedback to the user if the content of the input field doesn't pass the validation check.

## Drawbacks of existing validation frameworks
Although the Java Bean Validation framework has become the de-facto standard for Java based apps, it has several drawbacks and limitations:
- Most importantly: the validation rules can't be _reused_ easily in non-java components, and have to be re-implemented.
- Because of the annotation approach, real POJOs can not be validated.
- The functionality of some standard annotations are quite limited. E.g. with @Future it is not possible to validate that a date is 2 days in the future.
- Cross property validations requires lot of code ...
- Independent of any user permissions???
- ...

...

## Required features ...
It must be possible to define validation rules 
- for hierarchical/nested properties
- that involve 2 or more properties (multiple properties validation)
- for arrays/ lists ...
- that depends on user permissions

# Documentation

## Close-to-life example
Lets say we work for a company that rents medical equipment. Each medical _article_ may contain several _accessories_. Articles are grouped in _medical sets_. The equipment is store in warehouses and deliverd to customers locations. Staff members schedule _reservations_ on behalf of their customers.

### Example objects
This are example objects for the aforementioned entity types with some of their properties. The more technical properties like _id_, _createdBy_ etc. are omitted:

- Accessory
```json
  {
    "name": "Biopsy Forcep",
    "number": "BF-123"
  }
```
- Article
```json
  {
    "name": "Diagnostic Video Colonoscope",
    "number": "DVC-H123T/Z",
    "status": "ACTIVE",
    "animalUse": "true",
    "usedOnce": "false",
    "medicalSetId": null,
    "responsibleUser": null,
    "accessories": [
    ]
  }
```
- MedicalSet
```json
  {
    "name": "Endoscope set advanced",
    "number": "ESA-1",
    "status": "COMMISSIONING",
    "animalUse": "true",
    "articles": [
    ]
  }
```
- Reservation with customer
```json
  {
    "status": "PREPARATION",
    "startDate": "2019-12-01",
    "endDate": "2019-12-31",
    "customer": {
      "name": "Estetical Pet Clinic",
      "status": "GOLD",
      "address": {
        "city": "New York City",
        "zipCode": "10001"
      }
    },
    "medicalSets": [
    ]
  }
```

### Example validation rules
A fictitious requirements document could mention validation rules like this:
1. Only mangers are allowed to set customer level to PLATINUM.
1. Allowed article status transitions are: NEW -> ACTIVE, NEW -> INACTIVE, ACTIVE -> INACTIVE/DECOMMISSIONED, INACTIVE -> ACTIVE/DECOMMISSIONED".
1. An article must have a resposible user if the article status is not NEW and the article is not assigned to a MedicalSet.
1. If an article is used for the first time, it has to be flagged as such. This flag must never be reset.
1. The animalUse property of an article must not be changed if a) it is assigned to a medical set, or b) it has been used once for animals.
1. The reservation start date must be 3 days in the future.
1. The reservation end date must be after the start date. (?)
1. A reservation not in status PREPARATION must contain 1 to 3 medical sets. If the customer has status PLATINUM, it may contain up to 5 medical sets. (?)

## JSON structure
### Top-level content
A valid JSON contains 4 key-value pairs:
- _schema-version_ specifies the version of the JSON schema in use.
- _mandatoryRules_ is an object that may contain a key-value pair for each type that has validation rules regarding mandatory properties.
- _immutableRules_ is an object that may contain a key-value pair for each type that has validation rules regarding immutable properties.
- _contentRules_ is an object that may contain a key-value pair for each type that has validation rules regarding the content of properties.

```json
{
  "schema-version": "0.1",
  "mandatoryRules": {},
  "immutableRules": {},
  "contentRules": {}
}
```

### Type related validation rules
Validation rules regarding the properties of an entity type are defined by a key-value pair where the key is the name of the entity type. The value is an object that may contain a key-value pair for each property of that type.
```json
  {
    "article": {},
    "customer": {},
    "reservation": {}
  }
```

### Property related validation rules
Validation rules for a single property of an entity type are defined by a key-value pair.

The name of the key is determined by the type of the property
- For a _simple property_ it is its name, e.g.
  - "responsibleUser"
-  For a _nested property_ the name is build by concatenating the property names of the access path by using "." separators, e.g.
  - "customer.address.city"
- For _properties of objects that are part of arrays_, the property names can be appended by an _array index_ definition _[x]_, where _x_ is the index of the expected object, e.g.
  - "medicalSets[0].articles[0].animalUse"

The value is an **array** that may contain _contraint objects_.
- For validation rules regarding **mandatory** and **immutable** properties the array **may be empty**. That means that this property is mandatory resp. immutable, period. Technically speaking the validation rule always evaluates to _true_.
- For **content** related validation rules the array **must not be empty**, because there must be at least one statement about the _expected content_ of the property the rule is defined for.

```json
    {
      "status": [],
      "customer.address.city": [],
      "medicalSets[0].articles[0].animalUse": []
    }
```

### Contraint types and objects
Constraint objects specify _further conditions_ under which the validation rules will validate to true. There are _3 types_ of these conditions represented by three key-values pairs:
1. The key _content_ is used for conditions that relate to the content of the property the validation rule is defined for. The value is a simple constraint object that defines the allowed content of this property. 
    > JSON for example validation rule: "The article name length must be between 5 and 100 characters":
    ```json
    { 
      "contentRules": {
        "article": {
          "name": [
            {
              "content": {
                "type": "SIZE",
                "min": 5,
                "max": 100
              }
            }
          ]
        }
      }
    }
    ```
1. The key _permissions_ is used to restrict the validity of the validation rule to certain user permissions.
    > JSON for example validation rule: "The article name must not be modified if the user (who wants to insert resp. update the object) owns any of the permissons resp. roles APPRENTICE or REVIEWER":
    ```json
    { 
      "immutableRules": {
        "article": {
          "name": [
            {
              "permissions": {
                "type": "ANY",
                "values": [
                  "APPRENTICE",
                  "REVIEWER"
                ]
              }
            }
          ]
        }
      }
    }
    ```

1. Often, deciding whether a validation rule is valid depends on constraints on other _related properties_. These constraints are themself connected via logical _AND/OR operations_.
    
    Let _a, b, c_ and _d_ be 4 of these constraints. Then it should be possible to define logical expressions like _"a AND b OR c AND d"_, _"(a OR b) AND (c OR d)", and similar variants.

    These kinds of validation rules can be expressed by a JSON structure where the _related property constrains_ are grouped by using the keys _constraintsTopGroup_ and _constraintsSubGroups_ like this:
    ```json
        {
          "constraintsTopGroup": {
            "operator": "AND",
            "constraintsSubGroups": [
              {
                "operator": "OR",
                "constraints": [
                ]
              }
            ]
          }
        }
    ```
    > JSON for example validation rule: "The animalUse property of an article must not be changed if a) it is assigned to a medical set, or b) it has been used once for animals":
    ```json
    { 
      "immutableRules": {
        "article": {
          "animalUse": [
            {
              "constraintsTopGroup": {
                "operator": "OR",
                "constraintsSubGroups": [
                  {
                    "operator": "AND",
                    "constraints": [
                      {
                        "property": "medicalSetId",
                        "type": "EQUALS_NOT_NULL"
                      }
                    ]
                  },
                  {
                    "operator": "AND",
                    "constraints": [
                      {
                        "property": "usedOnce",
                        "type": "EQUALS_ANY",
                        "values": [
                          true
                        ]
                      },
                      {
                        "property": "animalUse",
                        "type": "EQUALS_ANY",
                        "values": [
                          true
                        ]
                      }
                    ]
                  }
                ]
              }
            }
          ]
        }
      }
    }
    ```

For content related validation rules the _content_ key is mandatory.
For validation rules regarding mandatory and immutable properties at least one of the _permissions_ and _dependencies_ keys must be present. The _content_ key is not allowed (resp. necessary).

### Combining contraint types and objects
TODO


## Elementary constraints
An elementary contraint can be used as a _content constraint_ or a _related property constraint_.

The value of the key _type_ is a string stating the type of the constraint. 
Each type may have type further specific key-values pairs.

If the constraint is used as a _related property constraint_ an additional key-value pair is required, where the key is _property_ and the value is the name of the property to which the constraint should be applied.

### EQUALS
#### EQUALS_ANY
The EQUALS_ANY constraint validates that the mentioned property value equals one of the (static) values listed in the array named _values_. It can be applied to string, number, and boolean. If the string complies to the data-time format it should be interpreted as such.
```json
    {
      "property": "status",
      "type": "EQUALS_ANY",
      "values": [
        "ACTIVE",
        "INACTIVE"
      ]
    }
```
#### EQUALS_ANY_REF
With the EQUALS_ANY_REF constraint it is possible to compair the values of different properties. It validates that the mentioned property value equals one of the property values referenced by the property names listed in the array named _values_.
```json
    {
      "property": "status",
      "type": "EQUALS_ANY_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ]
    }
```
#### EQUALS_NONE
TODO
```json
    {
      "property": "status",
      "type": "EQUALS_NONE",
      "values": [
        "NEW",
        "DECOMMISSIONED"
      ]
    }
```
#### EQUALS_NONE_REF
TODO
```json
    {
      "property": "status",
      "type": "EQUALS_NONE_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ]
    }
```
#### EQUALS_NULL
TODO
```json
    {
      "property": "status",
      "type": "EQUALS_NULL"
    }
```
#### EQUALS_NOT_NULL
TODO
```json
    {
      "property": "status",
      "type": "EQUALS_NOT_NULL"
    }
```
#### REGEX_ANY
TODO
```json
    {
      "property": "zipCode",
      "type": "REGEX_ANY",
      "values": [
        "^[0-9]{5}$"
      ]
    }
```
#### SIZE 
SIZE validates that the mentioned property value has a size between the attributes min and max. 

It can be applied to
- _string_
- _array_
- _object_
```json
    {
      "property": "arrayProp",
      "type": "SIZE",
      "min": 1,
      "max": 10
    }
```

# Implementations
TODO
- see Cross Language Validation Java (Producer and Validator)
- see Cross Language Validation ES6 (Consumer and Validator)

#### TODOs
Handle TODOs ...
Think about possible extensions, e.g.
-  Allow array index definitions like [1,3,5], [0/3], [1-9] or [*]








